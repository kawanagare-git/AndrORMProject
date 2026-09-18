[CmdletBinding()]
param(
    [string]$ProjectRoot,
    [string]$ManifestPath,
    [string]$ExternalProjectRoot = "E:\Projects\AndrOrmExternalTest",
    [int]$MaxAttempts = 12,
    [int]$RetrySeconds = 30
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"
[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12

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

function Test-CentralFile {
    param([Parameter(Mandatory = $true)][string]$Url)

    $tempFile = [System.IO.Path]::GetTempFileName()
    try {
        Invoke-WebRequest -Uri $Url -UseBasicParsing -OutFile $tempFile | Out-Null
        return ((Get-Item -LiteralPath $tempFile).Length -gt 0)
    }
    catch {
        return $false
    }
    finally {
        Remove-Item -LiteralPath $tempFile -Force -ErrorAction SilentlyContinue
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

try {
    Write-Section "AndrORM Maven Central 公開後検査"
    Write-Host "AndrORMProject      : $ProjectRoot"
    Write-Host "外部検証プロジェクト: $ExternalProjectRoot"

    $manifest = Get-Content -LiteralPath $ManifestPath -Raw | ConvertFrom-Json
    $propertiesPath = Join-Path $ProjectRoot "gradle.properties"
    $version = Get-GradleProperty -PropertiesFile $propertiesPath -PropertyName ([string]$manifest.publication.versionProperty)
    if ([string]::IsNullOrWhiteSpace($version)) {
        throw "リリースバージョンを取得できません。"
    }
    $publishedModules = @($manifest.modules | Where-Object { [bool]$_.publishRequired })
    $directModules = @($manifest.modules | Where-Object { [bool]$_.directConsumerDependency })

    Write-Section "Maven Central の5成果物"
    $groupUrl = ([string]$manifest.publication.groupId).Replace(".", "/")
    foreach ($module in $publishedModules) {
        $artifactId = [string]$module.artifactId
        $baseUrl = "https://repo1.maven.org/maven2/$groupUrl/$artifactId/$version"
        $pomUrl = "$baseUrl/$artifactId-$version.pom"
        $found = $false
        for ($attempt = 1; $attempt -le $MaxAttempts; $attempt++) {
            if (Test-CentralFile -Url $pomUrl) {
                $found = $true
                break
            }
            if ($attempt -lt $MaxAttempts) {
                Write-Warn "$artifactId は未反映です。$RetrySeconds 秒後に再確認します。($attempt/$MaxAttempts)"
                Start-Sleep -Seconds $RetrySeconds
            }
        }
        if ($found -eq $false) {
            throw "Maven Central に POM がありません: $pomUrl"
        }

        $jarUrl = "$baseUrl/$artifactId-$version.jar"
        $aarUrl = "$baseUrl/$artifactId-$version.aar"
        $mainUrl = $null
        if (Test-CentralFile -Url $jarUrl) {
            $mainUrl = $jarUrl
        }
        elseif (Test-CentralFile -Url $aarUrl) {
            $mainUrl = $aarUrl
        }
        if ([string]::IsNullOrWhiteSpace($mainUrl)) {
            throw "Maven Central にメイン JAR/AAR がありません: $artifactId"
        }

        $requiredUrls = @(
            "$pomUrl.asc",
            "$mainUrl.asc",
            "$baseUrl/$artifactId-$version.module",
            "$baseUrl/$artifactId-$version.module.asc",
            "$baseUrl/$artifactId-$version-sources.jar",
            "$baseUrl/$artifactId-$version-sources.jar.asc",
            "$baseUrl/$artifactId-$version-javadoc.jar",
            "$baseUrl/$artifactId-$version-javadoc.jar.asc"
        )
        foreach ($requiredUrl in $requiredUrls) {
            if ((Test-CentralFile -Url $requiredUrl) -eq $false) {
                throw "Maven Central に必須成果物または署名がありません: $requiredUrl"
            }
        }
        Write-Pass "${artifactId}:$version / main・POM・module・sources・javadoc・署名"
    }

    Write-Section "外部プロジェクトを Maven Central 専用にする"
    if ((Test-Path -LiteralPath $ExternalProjectRoot -PathType Container) -eq $false) {
        throw "外部検証プロジェクトがありません: $ExternalProjectRoot"
    }
    $settingsCandidates = @(
        (Join-Path $ExternalProjectRoot "settings.gradle.kts"),
        (Join-Path $ExternalProjectRoot "settings.gradle")
    )
    $settingsFile = $settingsCandidates | Where-Object { Test-Path -LiteralPath $_ -PathType Leaf } | Select-Object -First 1
    if ([string]::IsNullOrWhiteSpace($settingsFile)) {
        throw "外部検証プロジェクトの settings.gradle(.kts) がありません。"
    }
    if ((Get-Content -LiteralPath $settingsFile -Raw) -match "\bmavenLocal\s*\(") {
        throw "Maven Central 公開後検査では mavenLocal() を削除してください: $settingsFile"
    }
    Write-Pass "mavenLocal() なし"

    $buildFiles = @(Get-ChildItem -LiteralPath $ExternalProjectRoot -Recurse -File | Where-Object { $_.Name -eq "build.gradle.kts" -or $_.Name -eq "build.gradle" })
    $buildText = ($buildFiles | ForEach-Object { Get-Content -LiteralPath $_.FullName -Raw }) -join [Environment]::NewLine
    foreach ($module in $directModules) {
        $coordinate = "$($manifest.publication.groupId):$($module.artifactId):$version"
        if ($buildText.Contains($coordinate) -eq $false) {
            throw "外部検証プロジェクトに必須依存がありません: $coordinate"
        }
        Write-Pass $coordinate
    }

    $externalGradle = Get-GradleWrapper -Root $ExternalProjectRoot
    Invoke-CommandChecked -FilePath $externalGradle -Arguments @("--stop") -Description "外部プロジェクトの Gradle Daemon 停止" -WorkingDirectory $ExternalProjectRoot

    $cachePath = Join-Path $HOME ".gradle\caches\modules-2\files-2.1\io.github.kawanagare-git"
    if (Test-Path -LiteralPath $cachePath -PathType Container) {
        Remove-Item -LiteralPath $cachePath -Recurse -Force
        Write-Pass "AndrORM の Gradle 依存キャッシュを削除しました。"
    }

    $reportDirectory = Join-Path $ExternalProjectRoot "build\reports"
    New-Item -Path $reportDirectory -ItemType Directory -Force | Out-Null
    $logPath = Join-Path $reportDirectory "androrm-maven-central-verification.log"
    $previousLocation = Get-Location
    try {
        Set-Location $ExternalProjectRoot
        & $externalGradle ":app:dependencies" "--console=plain" "--refresh-dependencies" 2>&1 | Tee-Object -FilePath $logPath | Out-Host
        if ($LASTEXITCODE -ne 0) {
            throw "外部プロジェクトの依存関係取得に失敗しました。"
        }
    }
    finally {
        Set-Location $previousLocation
    }

    $dependencyText = Get-Content -LiteralPath $logPath -Raw
    foreach ($module in $directModules) {
        $coordinate = "$($manifest.publication.groupId):$($module.artifactId):$version"
        if ($dependencyText.Contains($coordinate) -eq $false) {
            throw "依存関係レポートに必須成果物がありません: $coordinate"
        }
        Write-Pass "依存関係レポート: $coordinate"
    }

    Invoke-CommandChecked -FilePath $externalGradle -Arguments @("`:app`:clean", "`:app`:assembleDebug", "`:app`:detekt", "--refresh-dependencies") -Description "Maven Central 版の assembleDebug と Detekt" -WorkingDirectory $ExternalProjectRoot

    $adbPath = Get-AdbPath
    if (Test-AndroidDeviceConnected -AdbPath $adbPath) {
        $testClass = "jp.pgw.lab78.andrormexternaltest.AndrOrmExternalAndroidTest"
        Invoke-CommandChecked -FilePath $externalGradle -Arguments @("`:app`:connectedDebugAndroidTest", "-Pandroid.testInstrumentationRunnerArguments.class=$testClass") -Description "Maven Central 版の Android Test" -WorkingDirectory $ExternalProjectRoot
    }
    else {
        throw "接続済み Android 端末または AVD がありません。公開後の最終検査では Android Test を省略できません。"
    }

    Write-Section "公開後検査結果"
    Write-Pass "5成果物の Maven Central 反映、必須3依存の取得、ビルド、Detekt、Android Test に成功しました。"
}
catch {
    Write-Host ""
    Write-Fail $_.Exception.Message
    exit 1
}
