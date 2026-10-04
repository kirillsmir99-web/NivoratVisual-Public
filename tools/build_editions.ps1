param([string]$JavaHome = $env:JAVA_HOME)
$ErrorActionPreference = 'Stop'
if (-not $JavaHome) {
    throw "Error: JAVA_HOME is not set and -JavaHome parameter was not provided. Please set JAVA_HOME to JDK 21."
}
if (-not (Test-Path "$JavaHome/bin/java.exe")) {
    throw "Error: java.exe not found under '$JavaHome/bin/java.exe'."
}
$projectRoot = Split-Path -Parent $PSScriptRoot
Push-Location $projectRoot
$oldJavaOptions = $env:JAVA_TOOL_OPTIONS
try {
    $socketTemp = Join-Path $projectRoot 'build/socket-temp'
    New-Item -ItemType Directory -Path $socketTemp -Force | Out-Null
    $env:JAVA_TOOL_OPTIONS = "-Djdk.net.unixdomain.tmpdir=$($socketTemp.Replace('\','/'))"
    foreach ($edition in @('free', 'pro')) {
        & "$JavaHome/bin/java.exe" -classpath gradle/wrapper/gradle-wrapper.jar org.gradle.wrapper.GradleWrapperMain assemble "-PnvEdition=$edition" --no-daemon
        if ($LASTEXITCODE -ne 0) { throw "Build failed: $edition" }
    }
    $releases = Join-Path $projectRoot 'build/releases'
    New-Item -ItemType Directory -Path $releases -Force | Out-Null
    $verMatch = Get-Content (Join-Path $projectRoot 'gradle.properties') | Where-Object { $_ -match '^mod_version\s*=\s*(.+)$' }
    $version = if ($verMatch) { $Matches[1].Trim() } else { '1.0.1' }
    Copy-Item "build/libs/NivoratFreeVisual-$version.jar" -Destination "$releases/NivoratFreeVisual-$version.jar" -Force
    Copy-Item "build/libs/NivoratVisual-$version.jar" -Destination "$releases/NivoratVisual-$version.jar" -Force
    Get-FileHash "$releases/NivoratFreeVisual-$version.jar","$releases/NivoratVisual-$version.jar"
} finally {
    $env:JAVA_TOOL_OPTIONS = $oldJavaOptions
    Pop-Location
}
