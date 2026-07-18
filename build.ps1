param(
    [string]$StarsectorHome = $(if ($env:STARSECTOR_HOME) { $env:STARSECTOR_HOME } else { "D:\Starsector" }),
    [string]$OutputRoot = "$(Join-Path $PSScriptRoot 'build')"
)

$ErrorActionPreference = "Stop"

$api = Join-Path $StarsectorHome "starsector-core\starfarer.api.jar"
$classes = Join-Path $OutputRoot "classes"
$release = Join-Path $OutputRoot "Colony Upkeep Rankings"
$jarDir = Join-Path $release "jars"

$javacCommand = Get-Command javac.exe -ErrorAction SilentlyContinue
$jarCommand = Get-Command jar.exe -ErrorAction SilentlyContinue
$javacPath = if ($javacCommand) { $javacCommand.Source } else { $null }
$jarPathTool = if ($jarCommand) { $jarCommand.Source } else { $null }

if ($env:JAVA_HOME) {
    $javaHomeJavac = Join-Path $env:JAVA_HOME "bin\javac.exe"
    $javaHomeJar = Join-Path $env:JAVA_HOME "bin\jar.exe"
    if (Test-Path -LiteralPath $javaHomeJavac) { $javacPath = $javaHomeJavac }
    if (Test-Path -LiteralPath $javaHomeJar) { $jarPathTool = $javaHomeJar }
}

if (-not $javacPath -or -not $jarPathTool) {
    throw "JDK 17 tools were not found. Set JAVA_HOME or add javac.exe and jar.exe to PATH."
}
if (-not (Test-Path -LiteralPath $api)) {
    throw "Starsector API not found at $api"
}

New-Item -ItemType Directory -Force -Path $classes, $jarDir | Out-Null
$outputFull = [System.IO.Path]::GetFullPath($OutputRoot)
$classesFull = [System.IO.Path]::GetFullPath($classes)
$safePrefix = $outputFull.TrimEnd([System.IO.Path]::DirectorySeparatorChar) + [System.IO.Path]::DirectorySeparatorChar
if (-not $classesFull.StartsWith($safePrefix, [System.StringComparison]::OrdinalIgnoreCase) -or
    [System.IO.Path]::GetFileName($classesFull) -ne "classes") {
    throw "Refusing unsafe build cleanup path: $classesFull"
}

Remove-Item -LiteralPath $classes -Recurse -Force
New-Item -ItemType Directory -Force -Path $classes | Out-Null

$sources = Get-ChildItem -LiteralPath (Join-Path $PSScriptRoot "src") -Recurse -Filter "*.java" | Select-Object -ExpandProperty FullName
& $javacPath --release 17 -Xlint:all -classpath $api -d $classes $sources
if ($LASTEXITCODE -ne 0) {
    throw "Compilation failed"
}

$jarPath = Join-Path $jarDir "ColonyUpkeepRankings.jar"
& $jarPathTool cf $jarPath -C $classes .
if ($LASTEXITCODE -ne 0) {
    throw "Jar creation failed"
}

Copy-Item -LiteralPath (Join-Path $PSScriptRoot "mod_info.json") -Destination $release -Force
Copy-Item -LiteralPath (Join-Path $PSScriptRoot "README.md") -Destination $release -Force

Write-Output $release
