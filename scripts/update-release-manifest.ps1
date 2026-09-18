[CmdletBinding()]
param(
    [string]$ProjectRoot,
    [string]$ManifestPath,
    [switch]$NonInteractive
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

    if ($NonInteractive) {
        throw "非対話モードでは回答が必要な質問を処理できません: $Prompt"
    }

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

function Read-ConsumerConfiguration {
    while ($true) {
        Write-Host "利用者側の Gradle Configuration を選択してください。"
        Write-Host "  1. implementation"
        Write-Host "  2. ksp"
        Write-Host "  3. detektPlugins"
        Write-Host "  4. その他"
        $selection = Read-Host "選択"

        switch ($selection.Trim()) {
            "1" { return "implementation" }
            "2" { return "ksp" }
            "3" { return "detektPlugins" }
            "4" {
                $customValue = Read-Host "Configuration 名を入力してください"
                if ([string]::IsNullOrWhiteSpace($customValue) -eq $false) {
                    return $customValue.Trim()
                }
                Write-Warn "Configuration 名は空にできません。"
            }
            default { Write-Warn "1 ～ 4 のいずれかを入力してください。" }
        }
    }
}

function Remove-GradleComments {
    param([Parameter(Mandatory = $true)][string]$Text)

    $result = [regex]::Replace(
        $Text,
        "/\*.*?\*/",
        "",
        [System.Text.RegularExpressions.RegexOptions]::Singleline
    )
    return [regex]::Replace($result, "(?m)//.*$", "")
}

function Get-GradleProjectPaths {
    param([Parameter(Mandatory = $true)][string]$SettingsFile)

    $content = Remove-GradleComments -Text (Get-Content -LiteralPath $SettingsFile -Raw)
    [string[]]$result = @()

    $callMatches = [regex]::Matches($content, "(?ms)\binclude\s*\((?<body>.*?)\)")
    foreach ($callMatch in $callMatches) {
        $pathMatches = [regex]::Matches($callMatch.Groups["body"].Value, '["''](?<path>:[^"'']+)["'']')
        foreach ($pathMatch in $pathMatches) {
            $result += $pathMatch.Groups["path"].Value
        }
    }

    $lineMatches = [regex]::Matches($content, "(?m)^\s*include\s+(?<body>[^\r\n]+)$")
    foreach ($lineMatch in $lineMatches) {
        $pathMatches = [regex]::Matches($lineMatch.Groups["body"].Value, '["''](?<path>:[^"'']+)["'']')
        foreach ($pathMatch in $pathMatches) {
            $result += $pathMatch.Groups["path"].Value
        }
    }

    return @($result | Where-Object { [string]::IsNullOrWhiteSpace($_) -eq $false } | Select-Object -Unique)
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

function Get-PublicationInformation {
    param(
        [AllowNull()][string]$BuildFile,
        [Parameter(Mandatory = $true)][string]$DefaultArtifactId
    )

    if ([string]::IsNullOrWhiteSpace($BuildFile)) {
        return [PSCustomObject]@{
            HasPublishingConfiguration = $false
            ArtifactId = $DefaultArtifactId
        }
    }

    $content = Get-Content -LiteralPath $BuildFile -Raw
    $hasPublishingConfiguration =
        $content -match "com\.vanniktech\.maven\.publish" -or
        $content -match '["'']maven-publish["'']' -or
        $content -match "\bmavenPublishing\s*\{" -or
        $content -match "\bpublishing\s*\{"

    $artifactId = $DefaultArtifactId
    $artifactAssignment = [regex]::Match($content, '\bartifactId\s*=\s*["''](?<value>[^"'']+)["'']')
    if ($artifactAssignment.Success) {
        $artifactId = $artifactAssignment.Groups["value"].Value
    }
    else {
        $coordinatesCall = [regex]::Match($content, '\bcoordinates\s*\(\s*[^,]+,\s*["''](?<value>[^"'']+)["'']')
        if ($coordinatesCall.Success) {
            $artifactId = $coordinatesCall.Groups["value"].Value
        }
    }

    return [PSCustomObject]@{
        HasPublishingConfiguration = $hasPublishingConfiguration
        ArtifactId = $artifactId
    }
}

function Get-KnownModuleSetting {
    param([Parameter(Mandatory = $true)][string]$ProjectPath)

    switch ($ProjectPath) {
        ":app" { return [PSCustomObject]@{ IsKnown = $true; PublishRequired = $false; Direct = $false; Configuration = $null } }
        ":shared-library" { return [PSCustomObject]@{ IsKnown = $true; PublishRequired = $true; Direct = $false; Configuration = $null } }
        ":androrm-common" { return [PSCustomObject]@{ IsKnown = $true; PublishRequired = $true; Direct = $false; Configuration = $null } }
        ":androrm-generator-ksp" { return [PSCustomObject]@{ IsKnown = $true; PublishRequired = $true; Direct = $true; Configuration = "ksp" } }
        ":androrm-runtime" { return [PSCustomObject]@{ IsKnown = $true; PublishRequired = $true; Direct = $true; Configuration = "implementation" } }
        ":androrm-detekt-rules" { return [PSCustomObject]@{ IsKnown = $true; PublishRequired = $true; Direct = $true; Configuration = "detektPlugins" } }
        default { return [PSCustomObject]@{ IsKnown = $false; PublishRequired = $false; Direct = $false; Configuration = $null } }
    }
}

function Get-GradleProperty {
    param(
        [Parameter(Mandatory = $true)][string]$PropertiesFile,
        [Parameter(Mandatory = $true)][string]$PropertyName
    )

    if (Test-Path -LiteralPath $PropertiesFile -PathType Leaf) {
        foreach ($line in Get-Content -LiteralPath $PropertiesFile) {
            if ($line -match "^\s*${PropertyName}\s*=\s*(?<value>.+?)\s*$") {
                return $Matches["value"]
            }
        }
    }
    return $null
}

function New-ManifestEntry {
    param(
        [Parameter(Mandatory = $true)][string]$ProjectPath,
        [Parameter(Mandatory = $true)][string]$ArtifactId,
        [Parameter(Mandatory = $true)][bool]$PublishRequired,
        [Parameter(Mandatory = $true)][bool]$DirectConsumerDependency,
        [AllowNull()][string]$ConsumerConfiguration
    )

    return [PSCustomObject][ordered]@{
        projectPath = $ProjectPath
        artifactId = $ArtifactId
        publishRequired = $PublishRequired
        directConsumerDependency = $DirectConsumerDependency
        consumerConfiguration = $ConsumerConfiguration
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

Write-Section "AndrORM リリースマニフェスト更新"
Write-Host "プロジェクトルート : $ProjectRoot"
Write-Host "マニフェスト       : $ManifestPath"

$settingsCandidates = @(
    (Join-Path $ProjectRoot "settings.gradle.kts"),
    (Join-Path $ProjectRoot "settings.gradle")
)
$settingsFile = $settingsCandidates | Where-Object { Test-Path -LiteralPath $_ -PathType Leaf } | Select-Object -First 1
if ([string]::IsNullOrWhiteSpace($settingsFile)) {
    Write-Fail "settings.gradle.kts または settings.gradle が見つかりません。"
    exit 1
}

$currentProjectPaths = @(Get-GradleProjectPaths -SettingsFile $settingsFile)
if ($currentProjectPaths.Count -eq 0) {
    Write-Fail "Gradle モジュールを検出できませんでした。"
    exit 1
}
Write-Pass "$($currentProjectPaths.Count) 件の Gradle モジュールを検出しました。"

$existingManifest = $null
$existingByPath = @{}
if (Test-Path -LiteralPath $ManifestPath -PathType Leaf) {
    try {
        $existingManifest = Get-Content -LiteralPath $ManifestPath -Raw | ConvertFrom-Json
        foreach ($entry in @($existingManifest.modules)) {
            $existingByPath[[string]$entry.projectPath] = $entry
        }
        Write-Pass "既存マニフェストを読み込みました。"
    }
    catch {
        Write-Fail "既存マニフェストを JSON として読み込めません。"
        Write-Host $_.Exception.Message
        exit 1
    }
}
else {
    Write-Warn "マニフェストが存在しないため、新規作成します。"
}

[object[]]$updatedEntries = @()
[string[]]$validationErrors = @()

foreach ($projectPath in $currentProjectPaths) {
    $moduleName = $projectPath.Split(":")[-1]
    $moduleDirectory = Get-ModuleDirectory -Root $ProjectRoot -ProjectPath $projectPath
    $buildFile = Get-BuildFile -ModuleDirectory $moduleDirectory
    $publication = Get-PublicationInformation -BuildFile $buildFile -DefaultArtifactId $moduleName

    if ($existingByPath.ContainsKey($projectPath)) {
        $entry = $existingByPath[$projectPath]
        $artifactId = [string]$entry.artifactId
        if ([string]::IsNullOrWhiteSpace($artifactId)) {
            $artifactId = $publication.ArtifactId
        }

        $publishRequired = [bool]$entry.publishRequired
        $direct = [bool]$entry.directConsumerDependency
        $configuration = if ($null -eq $entry.consumerConfiguration) { $null } else { [string]$entry.consumerConfiguration }

        if ($publishRequired -eq $false -and $publication.HasPublishingConfiguration) {
            if ($NonInteractive) {
                $validationErrors += "$projectPath はマニフェストで非公開ですが、Maven 公開設定が追加されています。対話モードで公開条件を確認してください。"
            }
            else {
                Write-Warn "$projectPath はマニフェストで非公開ですが、Maven 公開設定があります。"
                if (Read-YesNo -Prompt "公開対象へ変更しますか？" -DefaultYes $false) {
                    $publishRequired = $true
                    $direct = Read-YesNo -Prompt "利用者が build.gradle(.kts) へ直接指定する依存関係ですか？" -DefaultYes $false
                    if ($direct) {
                        $configuration = Read-ConsumerConfiguration
                    }
                }
            }
        }

        if ($publishRequired -and $publication.HasPublishingConfiguration -eq $false) {
            $validationErrors += "$projectPath は公開対象ですが、Maven 公開設定を確認できません。"
        }
        if ($publishRequired -eq $false -and $direct) {
            $validationErrors += "$projectPath は非公開ですが、directConsumerDependency=true です。"
        }
        if ($direct -and [string]::IsNullOrWhiteSpace($configuration)) {
            $validationErrors += "$projectPath は利用者の直接依存ですが、consumerConfiguration がありません。"
        }
        if ($direct -eq $false) {
            $configuration = $null
        }

        if ($publishRequired -and $artifactId -ne $publication.ArtifactId) {
            Write-Warn "$projectPath の artifactId を build.gradle(.kts) 側の値へ更新します: $artifactId -> $($publication.ArtifactId)"
            $artifactId = $publication.ArtifactId
        }

        $updatedEntries += New-ManifestEntry -ProjectPath $projectPath -ArtifactId $artifactId -PublishRequired $publishRequired -DirectConsumerDependency $direct -ConsumerConfiguration $configuration
        continue
    }

    Write-Section "新しいモジュールを検出"
    Write-Host "Project path : $projectPath"
    Write-Host "Directory    : $moduleDirectory"
    Write-Host "Build file   : $(if ($null -eq $buildFile) { '(なし)' } else { $buildFile })"
    Write-Host "artifactId   : $($publication.ArtifactId)"
    Write-Host "公開設定     : $(if ($publication.HasPublishingConfiguration) { '検出' } else { '未検出' })"

    $known = Get-KnownModuleSetting -ProjectPath $projectPath
    if ($known.IsKnown) {
        $publishRequired = [bool]$known.PublishRequired
        $direct = [bool]$known.Direct
        $configuration = $known.Configuration
        Write-Pass "既知の AndrORM モジュール設定を適用しました。"
    }
    else {
        if ($NonInteractive) {
            $validationErrors += "$projectPath はマニフェスト未登録の新規モジュールです。対話モードで公開条件を登録してください。"
            continue
        }

        $publishRequired = Read-YesNo -Prompt "Maven Central への公開対象ですか？" -DefaultYes $publication.HasPublishingConfiguration
        $direct = $false
        $configuration = $null
        if ($publishRequired) {
            $direct = Read-YesNo -Prompt "利用者が build.gradle(.kts) へ直接指定する依存関係ですか？" -DefaultYes $false
            if ($direct) {
                $configuration = Read-ConsumerConfiguration
            }
        }
    }

    if ($publishRequired -and $publication.HasPublishingConfiguration -eq $false) {
        $validationErrors += "$projectPath を公開対象にしましたが、Maven 公開設定を確認できません。"
    }

    $updatedEntries += New-ManifestEntry -ProjectPath $projectPath -ArtifactId $publication.ArtifactId -PublishRequired $publishRequired -DirectConsumerDependency $direct -ConsumerConfiguration $configuration
}

$removedPaths = @($existingByPath.Keys | Where-Object { $currentProjectPaths -notcontains $_ } | Sort-Object)
foreach ($removedPath in $removedPaths) {
    if ($NonInteractive) {
        $validationErrors += "$removedPath はマニフェストにありますが settings.gradle(.kts) に存在しません。"
        continue
    }

    Write-Section "削除されたモジュールを検出"
    Write-Host "Project path : $removedPath"
    if (Read-YesNo -Prompt "マニフェストから削除しますか？" -DefaultYes $true) {
        continue
    }
    $validationErrors += "$removedPath は settings.gradle(.kts) に存在しません。"
}

if ($validationErrors.Count -gt 0) {
    Write-Section "検証エラー"
    foreach ($message in $validationErrors) {
        Write-Fail $message
    }
    Write-Fail "マニフェストは更新していません。"
    exit 1
}

$propertiesFile = Join-Path $ProjectRoot "gradle.properties"
$groupId = Get-GradleProperty -PropertiesFile $propertiesFile -PropertyName "andrormGroup"
if ([string]::IsNullOrWhiteSpace($groupId)) {
    $groupId = "io.github.kawanagare-git"
}

$repositoryOwner = "kawanagare-git"
$repositoryName = "AndrORMProject"
$developmentBranch = "mk-develop"
$releaseBranch = "master"
if ($null -ne $existingManifest) {
    if ($null -ne $existingManifest.repository) {
        $repositoryOwner = [string]$existingManifest.repository.owner
        $repositoryName = [string]$existingManifest.repository.name
        $developmentBranch = [string]$existingManifest.repository.developmentBranch
        $releaseBranch = [string]$existingManifest.repository.releaseBranch
    }
}

$manifest = [PSCustomObject][ordered]@{
    schemaVersion = 1
    repository = [PSCustomObject][ordered]@{
        owner = $repositoryOwner
        name = $repositoryName
        developmentBranch = $developmentBranch
        releaseBranch = $releaseBranch
    }
    publication = [PSCustomObject][ordered]@{
        groupId = $groupId
        versionProperty = "andrormVersion"
    }
    modules = [object[]]$updatedEntries
}

$json = $manifest | ConvertTo-Json -Depth 10
$manifestDirectory = Split-Path -Parent $ManifestPath
New-Item -Path $manifestDirectory -ItemType Directory -Force | Out-Null
$utf8WithoutBom = New-Object System.Text.UTF8Encoding($false)
[System.IO.File]::WriteAllText($ManifestPath, $json + [Environment]::NewLine, $utf8WithoutBom)

try {
    Get-Content -LiteralPath $ManifestPath -Raw | ConvertFrom-Json | Out-Null
}
catch {
    Write-Fail "書き込んだマニフェストを JSON として再読込できません。"
    exit 1
}

Write-Section "結果"
Write-Pass "マニフェストを更新しました: $ManifestPath"
$manifest.modules |
    Select-Object projectPath, artifactId, publishRequired, directConsumerDependency, consumerConfiguration |
    Format-Table -AutoSize
