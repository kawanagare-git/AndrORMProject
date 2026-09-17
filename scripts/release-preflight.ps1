[CmdletBinding()]
param(
    [string]$ProjectRoot,
    [string]$ManifestPath,
    [string]$ExternalProjectRoot = "E:\Projects\AndrOrmExternalTest",
    [string]$SigningFingerprint = "7975CD2B10B9189CB486697C305208AF7E088CBC",
    [switch]$ValidateOnly
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

function Write-Section {
    param([Parameter(Mandatory = $true)][string]$Message)
    Write-Host ""
    Write-Host "=== $Message ===" -ForegroundColor Cyan
}

function Write-Pass {
    param([Parameter(Mandatory = $true)][string]$Message)
    Write-Host "[PASS] $Message" -ForegroundColor Green
}

function Write-Warn {
    param([Parameter(Mandatory = $true)][string]$Message)
    Write-Host "[WARN] $Message" -ForegroundColor Yellow
}

function Write-Fail {
    param([Parameter(Mandatory = $true)][string]$Message)
    Write-Host "[ERROR] $Message" -ForegroundColor Red
}

function Read-YesNo {
    param(
        [Parameter(Mandatory = $true)][string]$Prompt,
        [bool]$DefaultYes = $false
    )

    $suffix = if ($DefaultYes) { "[Y/n]" } else { "[y/N]" }
    while ($true) {
        $answer = Read-Host "$Prompt $suffix"
        if ([string]::IsNullOrWhiteSpace($answer)) {
            return $DefaultYes
        }
        switch ($answer.Trim().ToLowerInvariant()) {
            "y" { return $true }
            "yes" { return $true }
            "n" { return $false }
            "no" { return $false }
            default { Write-Warn "y または n を入力してください。" }
        }
    }
}

function Invoke-CommandChecked {
    param(
        [Parameter(Mandatory = $true)][string]$FilePath,
        [Parameter(Mandatory = $true)][string[]]$Arguments,
        [Parameter(Mandatory = $true)][string]$Description,
        [string]$WorkingDirectory
    )

    Write-Host "[RUN ] $Description"
    $previousLocation = Get-Location
    try {
        if ([string]::IsNullOrWhiteSpace($WorkingDirectory) -eq $false) {
            Set-Location $WorkingDirectory
        }
        & $FilePath @Arguments
        if ($LASTEXITCODE -ne 0) {
            throw "$Description が終了コード $LASTEXITCODE で失敗しました。"
        }
    }
    finally {
        Set-Location $previousLocation
    }
    Write-Pass $Description
}

function Get-GradleProperty {
    param(
        [Parameter(Mandatory = $true)][string]$PropertiesFile,
        [Parameter(Mandatory = $true)][string]$PropertyName
    )

    foreach ($line in Get-Content -LiteralPath $PropertiesFile) {
        if ($line -match "^\s*${PropertyName}\s*=\s*(?<value>.+?)\s*$") {
            return $Matches["value"]
        }
    }
    return $null
}

function Get-ModuleDirectory {
    param(
        [Parameter(Mandatory = $true)][string]$Root,
        [Parameter(Mandatory = $true)][string]$ProjectPath
    )

    $relativePath = $ProjectPath.TrimStart(":").Replace(":", [System.IO.Path]::DirectorySeparatorChar)
    return Join-Path $Root $relativePath
}

function Get-BuildFile {
    param([Parameter(Mandatory = $true)][string]$ModuleDirectory)

    $kotlinFile = Join-Path $ModuleDirectory "build.gradle.kts"
    if (Test-Path -LiteralPath $kotlinFile -PathType Leaf) {
        return $kotlinFile
    }
    $groovyFile = Join-Path $ModuleDirectory "build.gradle"
    if (Test-Path -LiteralPath $groovyFile -PathType Leaf) {
        return $groovyFile
    }
    return $null
}

function Test-PublishingConfiguration {
    param([Parameter(Mandatory = $true)][string]$BuildFile)

    $content = Get-Content -LiteralPath $BuildFile -Raw
    return (
        $content -match "com\.vanniktech\.maven\.publish" -or
        $content -match '["'']maven-publish["'']' -or
        $content -match "\bmavenPublishing\s*\{" -or
        $content -match "\bpublishing\s*\{"
    )
}

function Get-GradleWrapper {
    param([Parameter(Mandatory = $true)][string]$Root)

    $wrapper = Join-Path $Root "gradlew.bat"
    if (Test-Path -LiteralPath $wrapper -PathType Leaf) {
        return $wrapper
    }
    $wrapper = Join-Path $Root "gradlew"
    if (Test-Path -LiteralPath $wrapper -PathType Leaf) {
        return $wrapper
    }
    throw "Gradle Wrapper が見つかりません: $Root"
}

function Get-ExpectedTestTask {
    param(
        [Parameter(Mandatory = $true)][string]$GradleWrapper,
        [Parameter(Mandatory = $true)][string]$ProjectRoot,
        [Parameter(Mandatory = $true)][string]$ProjectPath
    )

    $output = & $GradleWrapper "$ProjectPath`:tasks" "--all" "--console=plain" 2>&1
    if ($LASTEXITCODE -ne 0) {
        throw "$ProjectPath の Gradle タスク一覧を取得できません。"
    }

    $text = ($output | Out-String)
    if ($text -match "(?m)^testDebugUnitTest\s+-") {
        return "$ProjectPath`:testDebugUnitTest"
    }
    if ($text -match "(?m)^test\s+-") {
        return "$ProjectPath`:test"
    }
    throw "$ProjectPath で test または testDebugUnitTest を検出できません。"
}

function Get-PlainText {
    param([Parameter(Mandatory = $true)][System.Security.SecureString]$SecureString)

    $pointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($SecureString)
    try {
        return [Runtime.InteropServices.Marshal]::PtrToStringBSTR($pointer)
    }
    finally {
        [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($pointer)
    }
}

function Get-AdbPath {
    $command = Get-Command adb -ErrorAction SilentlyContinue
    if ($null -ne $command) {
        return $command.Source
    }

    foreach ($root in @($env:ANDROID_SDK_ROOT, $env:ANDROID_HOME)) {
        if ([string]::IsNullOrWhiteSpace($root) -eq $false) {
            $candidate = Join-Path $root "platform-tools\adb.exe"
            if (Test-Path -LiteralPath $candidate -PathType Leaf) {
                return $candidate
            }
        }
    }
    return $null
}

function Test-AndroidDeviceConnected {
    param([AllowNull()][string]$AdbPath)

    if ([string]::IsNullOrWhiteSpace($AdbPath)) {
        return $false
    }
    $lines = & $AdbPath devices 2>$null
    if ($LASTEXITCODE -ne 0) {
        return $false
    }
    foreach ($line in $lines) {
        if ($line -match "\tdevice$") {
            return $true
        }
    }
    return $false
}

function Test-ZipEntryExists {
    param(
        [Parameter(Mandatory = $true)][string]$ArchivePath,
        [Parameter(Mandatory = $true)][string]$EntryPath
    )

    Add-Type -AssemblyName System.IO.Compression.FileSystem
    $archive = [System.IO.Compression.ZipFile]::OpenRead($ArchivePath)
    try {
        foreach ($entry in $archive.Entries) {
            if ($entry.FullName -eq $EntryPath) {
                return $true
            }
        }
        return $false
    }
    finally {
        $archive.Dispose()
    }
}

if ([string]::IsNullOrWhiteSpace($ProjectRoot)) {
    $ProjectRoot = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot ".."))
}
else {
    $ProjectRoot = [System.IO.Path]::GetFullPath($ProjectRoot)
}
if ([string]::IsNullOrWhiteSpace($ManifestPath)) {
    $ManifestPath = Join-Path $ProjectRoot "release\androrm-release-manifest.json"
}
else {
    $ManifestPath = [System.IO.Path]::GetFullPath($ManifestPath)
}

$updateScript = Join-Path $PSScriptRoot "update-release-manifest.ps1"
$workflowPath = Join-Path $ProjectRoot ".github\workflows\publish-maven-central.yml"
$propertiesPath = Join-Path $ProjectRoot "gradle.properties"
$tempDirectory = $null
$plainPassword = $null
$initialLocation = Get-Location
$scriptExitCode = 0

try {
    Set-Location $ProjectRoot
    Write-Section "AndrORM リリース前検査"
    Write-Host "AndrORMProject      : $ProjectRoot"
    Write-Host "外部検証プロジェクト: $ExternalProjectRoot"
    Write-Host "マニフェスト        : $ManifestPath"

    if ((Test-Path -LiteralPath $updateScript -PathType Leaf) -eq $false) {
        throw "マニフェスト更新バッチがありません: $updateScript"
    }
    & $updateScript -ProjectRoot $ProjectRoot -ManifestPath $ManifestPath -NonInteractive:$ValidateOnly
    if ($? -eq $false) {
        throw "リリースマニフェストの更新または検証に失敗しました。"
    }

    $manifest = Get-Content -LiteralPath $ManifestPath -Raw | ConvertFrom-Json
    $publishedModules = @($manifest.modules | Where-Object { [bool]$_.publishRequired })
    $directModules = @($manifest.modules | Where-Object { [bool]$_.directConsumerDependency })
    if ($publishedModules.Count -eq 0) {
        throw "公開対象モジュールが0件です。"
    }
    Write-Pass "公開対象モジュール $($publishedModules.Count) 件を確認しました。"

    if ((Test-Path -LiteralPath $propertiesPath -PathType Leaf) -eq $false) {
        throw "gradle.properties がありません: $propertiesPath"
    }
    $versionProperty = [string]$manifest.publication.versionProperty
    $version = Get-GradleProperty -PropertiesFile $propertiesPath -PropertyName $versionProperty
    if ([string]::IsNullOrWhiteSpace($version)) {
        throw "gradle.properties から $versionProperty を取得できません。"
    }
    Write-Pass "リリースバージョン: $version"

    $gradleWrapper = Get-GradleWrapper -Root $ProjectRoot

    Write-Section "Git と公開元ソース"
    if ($ValidateOnly -eq $false) {
        $currentBranch = (& git -C $ProjectRoot branch --show-current).Trim()
        if ($LASTEXITCODE -ne 0) {
            throw "現在の Git ブランチを取得できません。"
        }
        $expectedBranch = [string]$manifest.repository.developmentBranch
        if ($currentBranch -ne $expectedBranch) {
            throw "作業ブランチが異なります。現在: $currentBranch / 必須: $expectedBranch"
        }
        Write-Pass "作業ブランチ: $currentBranch"

        $gitStatus = & git -C $ProjectRoot status --porcelain
        if ($LASTEXITCODE -ne 0) {
            throw "Git の状態を取得できません。"
        }
        if (@($gitStatus).Count -gt 0) {
            throw "未コミットの変更があります。リリース前検査はコミット後に実行してください。"
        }
        Write-Pass "未コミット変更なし"
    }
    else {
        Write-Pass "ValidateOnly のためブランチと未コミット状態の判定を省略しました。"
    }

    Write-Section "モジュールと Maven 公開設定"
    [string[]]$testTasks = @()
    foreach ($module in $publishedModules) {
        $projectPath = [string]$module.projectPath
        $moduleDirectory = Get-ModuleDirectory -Root $ProjectRoot -ProjectPath $projectPath
        if ((Test-Path -LiteralPath $moduleDirectory -PathType Container) -eq $false) {
            throw "$projectPath のモジュールディレクトリがありません: $moduleDirectory"
        }
        $buildFile = Get-BuildFile -ModuleDirectory $moduleDirectory
        if ([string]::IsNullOrWhiteSpace($buildFile)) {
            throw "$projectPath の build.gradle(.kts) がありません。"
        }
        if ((Test-PublishingConfiguration -BuildFile $buildFile) -eq $false) {
            throw "$projectPath に Maven 公開設定がありません。"
        }

        $propertiesOutput = & $gradleWrapper "$projectPath`:properties" "--console=plain" 2>&1
        if ($LASTEXITCODE -ne 0) {
            throw "$projectPath の Gradle properties 取得に失敗しました。"
        }
        $propertiesText = $propertiesOutput | Out-String
        $versionMatch = [regex]::Match($propertiesText, "(?m)^version:\s*(?<value>.+?)\s*$")
        if ($versionMatch.Success -eq $false) {
            throw "$projectPath の version を確認できません。"
        }
        if ($versionMatch.Groups["value"].Value -ne $version) {
            throw "$projectPath の version が一致しません。期待: $version / 実際: $($versionMatch.Groups['value'].Value)"
        }

        $testTask = Get-ExpectedTestTask -GradleWrapper $gradleWrapper -ProjectRoot $ProjectRoot -ProjectPath $projectPath
        $testTasks += $testTask
        Write-Pass "$projectPath / artifactId=$($module.artifactId) / test=$testTask"
    }

    Write-Section "GitHub Actions"
    if ((Test-Path -LiteralPath $workflowPath -PathType Leaf) -eq $false) {
        throw "公開ワークフローがありません: $workflowPath"
    }
    $workflowText = Get-Content -LiteralPath $workflowPath -Raw
    if ($workflowText -notmatch "publishToMavenCentral") {
        throw "公開ワークフローに publishToMavenCentral がありません。"
    }
    foreach ($testTask in $testTasks) {
        if ($workflowText.Contains($testTask) -eq $false) {
            throw "公開ワークフローにテストタスクがありません: $testTask"
        }
    }
    Write-Pass "公開ワークフローに公開処理と全公開モジュールのテストがあります。"

    Write-Section "README の利用者向け必須依存"
    $readmeFiles = @(Get-ChildItem -LiteralPath $ProjectRoot -File | Where-Object { $_.Name -match "(?i)^readme.*\.md$" })
    if ($readmeFiles.Count -eq 0) {
        throw "README ファイルを検出できません。"
    }
    foreach ($readmeFile in $readmeFiles) {
        $readmeText = Get-Content -LiteralPath $readmeFile.FullName -Raw
        foreach ($module in $directModules) {
            $coordinate = "$($manifest.publication.groupId):$($module.artifactId):$version"
            if ($readmeText.Contains($coordinate) -eq $false) {
                throw "$($readmeFile.Name) に必須依存がありません: $coordinate"
            }
        }
        Write-Pass "$($readmeFile.Name) の必須3依存"
    }

    if ($ValidateOnly) {
        Write-Section "ValidateOnly 結果"
        Write-Pass "ソース、マニフェスト、公開設定、ワークフロー、README の静的検査に成功しました。"
        return
    }

    Write-Section "PGP 秘密鍵"
    $gpgCommand = Get-Command gpg -ErrorAction SilentlyContinue
    if ($null -eq $gpgCommand) {
        throw "gpg コマンドが見つかりません。"
    }
    $secretKeyOutput = & $gpgCommand.Source --batch --with-colons --list-secret-keys $SigningFingerprint 2>&1
    if ($LASTEXITCODE -ne 0) {
        throw "指定した PGP 秘密鍵を GPG 鍵リングから取得できません: $SigningFingerprint"
    }
    if (($secretKeyOutput | Out-String) -notmatch [regex]::Escape($SigningFingerprint)) {
        throw "指定した PGP 秘密鍵が GPG 鍵リングにありません: $SigningFingerprint"
    }
    Write-Pass "GPG 鍵リングに秘密鍵があります。"

    $securePassword = Read-Host "PGP のパスフレーズを入力してください" -AsSecureString
    $plainPassword = Get-PlainText -SecureString $securePassword
    if ([string]::IsNullOrWhiteSpace($plainPassword)) {
        throw "PGP のパスフレーズが空です。"
    }

    $tempDirectory = Join-Path ([System.IO.Path]::GetTempPath()) ("androrm-release-" + [Guid]::NewGuid().ToString("N"))
    New-Item -Path $tempDirectory -ItemType Directory -Force | Out-Null
    $tempKeyPath = Join-Path $tempDirectory "androrm-signing-key.asc"

    $exportedKey = $plainPassword | & $gpgCommand.Source --batch --yes --pinentry-mode loopback --passphrase-fd 0 --armor --export-secret-keys $SigningFingerprint
    if ($LASTEXITCODE -ne 0) {
        throw "PGP 秘密鍵の一時エクスポートに失敗しました。"
    }
    $keyText = ($exportedKey | Out-String).Trim()
    if ($keyText -notmatch "-----BEGIN PGP PRIVATE KEY BLOCK-----") {
        throw "エクスポート結果が PGP 秘密鍵ではありません。"
    }
    [System.IO.File]::WriteAllText($tempKeyPath, $keyText + [Environment]::NewLine, [System.Text.Encoding]::ASCII)
    Write-Pass "秘密鍵を一時ファイルへエクスポートしました。"

    $env:ORG_GRADLE_PROJECT_signingInMemoryKey = Get-Content -LiteralPath $tempKeyPath -Raw
    $env:ORG_GRADLE_PROJECT_signingInMemoryKeyPassword = $plainPassword

    Write-Section "公開対象モジュールのテスト"
    Invoke-CommandChecked -FilePath $gradleWrapper -Arguments $testTasks -Description "全公開モジュールの Unit Test" -WorkingDirectory $ProjectRoot

    Write-Section "Maven Local 公開"
    Invoke-CommandChecked -FilePath $gradleWrapper -Arguments @("clean", "publishToMavenLocal") -Description "Maven Local への署名付き公開" -WorkingDirectory $ProjectRoot

    Write-Section "Maven Local 成果物と署名"
    $groupPath = ([string]$manifest.publication.groupId).Replace(".", [System.IO.Path]::DirectorySeparatorChar)
    $mavenLocalRoot = Join-Path (Join-Path $HOME ".m2\repository") $groupPath
    foreach ($module in $publishedModules) {
        $artifactId = [string]$module.artifactId
        $artifactDirectory = Join-Path (Join-Path $mavenLocalRoot $artifactId) $version
        if ((Test-Path -LiteralPath $artifactDirectory -PathType Container) -eq $false) {
            throw "Maven Local に成果物ディレクトリがありません: $artifactDirectory"
        }

        $requiredPatterns = @(
            "$artifactId-$version.pom",
            "$artifactId-$version.module",
            "$artifactId-$version-sources.jar",
            "$artifactId-$version-javadoc.jar"
        )
        foreach ($requiredName in $requiredPatterns) {
            $requiredPath = Join-Path $artifactDirectory $requiredName
            if ((Test-Path -LiteralPath $requiredPath -PathType Leaf) -eq $false) {
                throw "必須成果物がありません: $requiredPath"
            }
        }

        $mainCandidates = @(
            (Join-Path $artifactDirectory "$artifactId-$version.jar"),
            (Join-Path $artifactDirectory "$artifactId-$version.aar")
        )
        $mainArtifact = $mainCandidates | Where-Object { Test-Path -LiteralPath $_ -PathType Leaf } | Select-Object -First 1
        if ([string]::IsNullOrWhiteSpace($mainArtifact)) {
            throw "メイン成果物の JAR または AAR がありません: $artifactDirectory"
        }

        $filesToVerify = @(
            $mainArtifact,
            (Join-Path $artifactDirectory "$artifactId-$version.pom"),
            (Join-Path $artifactDirectory "$artifactId-$version.module"),
            (Join-Path $artifactDirectory "$artifactId-$version-sources.jar"),
            (Join-Path $artifactDirectory "$artifactId-$version-javadoc.jar")
        )
        foreach ($filePath in $filesToVerify) {
            $signaturePath = "$filePath.asc"
            if ((Test-Path -LiteralPath $signaturePath -PathType Leaf) -eq $false) {
                throw "署名ファイルがありません: $signaturePath"
            }
            & $gpgCommand.Source --verify $signaturePath $filePath 2>&1 | Out-Host
            if ($LASTEXITCODE -ne 0) {
                throw "PGP 署名検証に失敗しました: $filePath"
            }
        }
        Write-Pass "$artifactId / main・POM・module・sources・javadoc・署名"

        if ($artifactId -eq "androrm-detekt-rules") {
            if ((Test-ZipEntryExists -ArchivePath $mainArtifact -EntryPath "META-INF/services/io.gitlab.arturbosch.detekt.api.RuleSetProvider") -eq $false) {
                throw "androrm-detekt-rules の RuleSetProvider サービス定義がありません。"
            }
            Write-Pass "androrm-detekt-rules の RuleSetProvider サービス定義"
        }
    }

    Write-Section "外部プロジェクトによる Maven Local 検証"
    if ((Test-Path -LiteralPath $ExternalProjectRoot -PathType Container) -eq $false) {
        throw "外部検証プロジェクトがありません: $ExternalProjectRoot"
    }
    $externalSettingsCandidates = @(
        (Join-Path $ExternalProjectRoot "settings.gradle.kts"),
        (Join-Path $ExternalProjectRoot "settings.gradle")
    )
    $externalSettings = $externalSettingsCandidates | Where-Object { Test-Path -LiteralPath $_ -PathType Leaf } | Select-Object -First 1
    if ([string]::IsNullOrWhiteSpace($externalSettings)) {
        throw "外部検証プロジェクトの settings.gradle(.kts) がありません。"
    }
    $externalSettingsText = Get-Content -LiteralPath $externalSettings -Raw
    if ($externalSettingsText -notmatch "\bmavenLocal\s*\(") {
        throw "Maven Local 検証前の外部プロジェクトに mavenLocal() がありません: $externalSettings"
    }
    $mavenLocalIndex = $externalSettingsText.IndexOf("mavenLocal", [System.StringComparison]::Ordinal)
    $mavenCentralAfterLocal = $externalSettingsText.IndexOf(
        "mavenCentral",
        $mavenLocalIndex,
        [System.StringComparison]::Ordinal
    )
    if ($externalSettingsText -match "\bmavenCentral\s*\(" -and $mavenCentralAfterLocal -lt 0) {
        throw "依存関係用リポジトリでは mavenLocal() を mavenCentral() より前に置いてください: $externalSettings"
    }
    Write-Pass "Maven Local 検証用のリポジトリ設定を確認しました。"

    $externalBuildFiles = @(Get-ChildItem -LiteralPath $ExternalProjectRoot -Recurse -File | Where-Object { $_.Name -eq "build.gradle.kts" -or $_.Name -eq "build.gradle" })
    $externalBuildText = ($externalBuildFiles | ForEach-Object { Get-Content -LiteralPath $_.FullName -Raw }) -join [Environment]::NewLine
    foreach ($module in $directModules) {
        $coordinate = "$($manifest.publication.groupId):$($module.artifactId):$version"
        if ($externalBuildText.Contains($coordinate) -eq $false) {
            throw "外部検証プロジェクトに必須依存がありません: $coordinate"
        }
        Write-Pass $coordinate
    }

    $externalGradle = Get-GradleWrapper -Root $ExternalProjectRoot
    Invoke-CommandChecked -FilePath $externalGradle -Arguments @("--stop") -Description "外部プロジェクトの Gradle Daemon 停止" -WorkingDirectory $ExternalProjectRoot
    Invoke-CommandChecked -FilePath $externalGradle -Arguments @("`:app`:clean", "`:app`:assembleDebug", "`:app`:detekt", "--refresh-dependencies") -Description "Maven Local 版の assembleDebug と Detekt" -WorkingDirectory $ExternalProjectRoot

    $adbPath = Get-AdbPath
    if (Test-AndroidDeviceConnected -AdbPath $adbPath) {
        $testClass = "jp.pgw.lab78.andrormexternaltest.AndrOrmExternalAndroidTest"
        Invoke-CommandChecked -FilePath $externalGradle -Arguments @("`:app`:connectedDebugAndroidTest", "-Pandroid.testInstrumentationRunnerArguments.class=$testClass") -Description "Maven Local 版の Android Test" -WorkingDirectory $ExternalProjectRoot
    }
    else {
        Write-Warn "接続済み Android 端末または AVD を検出できません。"
        if ((Read-YesNo -Prompt "同じバージョンで Android Test を別途実行し、成功を確認済みですか？" -DefaultYes $false) -eq $false) {
            throw "Android Test の成功確認がありません。"
        }
    }

    Write-Section "リリース前検査結果"
    Write-Pass "公開前の自動検査にすべて成功しました。"
    Write-Host "次の工程は、mk-develop から master への Pull Request です。"
}
catch {
    Write-Host ""
    Write-Fail $_.Exception.Message
    $scriptExitCode = 1
}
finally {
    Remove-Item Env:ORG_GRADLE_PROJECT_signingInMemoryKey -ErrorAction SilentlyContinue
    Remove-Item Env:ORG_GRADLE_PROJECT_signingInMemoryKeyPassword -ErrorAction SilentlyContinue
    $plainPassword = $null
    if ([string]::IsNullOrWhiteSpace($tempDirectory) -eq $false) {
        if (Test-Path -LiteralPath $tempDirectory -PathType Container) {
            Remove-Item -LiteralPath $tempDirectory -Recurse -Force
            Write-Pass "一時エクスポートした PGP 秘密鍵を削除しました。"
        }
    }
    Set-Location $initialLocation
}

if ($scriptExitCode -ne 0) {
    exit $scriptExitCode
}
