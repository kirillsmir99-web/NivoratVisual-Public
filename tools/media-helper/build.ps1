param([switch]$Test)
$ErrorActionPreference = 'Stop'
$nvRoot = (Resolve-Path (Join-Path $PSScriptRoot '..\..')).Path
$nvFramework = Join-Path $env:WINDIR 'Microsoft.NET\Framework64\v4.0.30319'
$nvRuntime = Get-ChildItem (Join-Path $env:WINDIR 'Microsoft.NET\assembly\GAC_MSIL\System.Runtime') -Recurse -Filter System.Runtime.dll | Select-Object -First 1
$nvReferences = @('Windows.Media.winmd','Windows.Foundation.winmd','Windows.Storage.winmd') | ForEach-Object { '/r:' + (Join-Path $env:WINDIR ('System32\WinMetadata\' + $_)) }
$nvReferences += @(('/r:' + $nvRuntime.FullName), ('/r:' + (Join-Path $nvFramework 'System.Runtime.WindowsRuntime.dll')), ('/r:' + (Join-Path $nvFramework 'System.Runtime.InteropServices.WindowsRuntime.dll')), '/r:System.Web.Extensions.dll')
$nvOutput = Join-Path $nvRoot 'src\main\resources\helper\NvMediaHelper.exe'
& (Join-Path $nvFramework 'csc.exe') /nologo /optimize+ /target:exe ('/out:' + $nvOutput) @nvReferences (Join-Path $PSScriptRoot 'SessionPolicy.cs') (Join-Path $PSScriptRoot 'AudioSpectrum.cs') (Join-Path $PSScriptRoot 'Program.cs')
if ($LASTEXITCODE -ne 0) { throw 'NV Media compilation failed' }
Get-FileHash -LiteralPath $nvOutput -Algorithm SHA256
if ($Test) {
    $nvTests = Join-Path $nvRoot 'build\MediaPolicyTests.exe'
    & (Join-Path $nvFramework 'csc.exe') /nologo /optimize+ ('/out:' + $nvTests) (Join-Path $PSScriptRoot 'SessionPolicy.cs') (Join-Path $PSScriptRoot 'PolicyTests.cs')
    if ($LASTEXITCODE -ne 0) { throw 'Media policy tests did not compile' }
    & $nvTests
    if ($LASTEXITCODE -ne 0) { throw 'Media policy tests failed' }
}
