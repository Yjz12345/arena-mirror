$dir = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $dir

Write-Host "=== 角斗场：百层之镜 [Neon] ==="
Write-Host "Compiling..."

if (-not (Test-Path build)) { New-Item -ItemType Directory -Path build }

$src = @(
    "src\arenamirror\Main.java",
    "src\arenamirror\core\*.java",
    "src\arenamirror\player\*.java",
    "src\arenamirror\enemies\*.java",
    "src\arenamirror\data\*.java",
    "src\arenamirror\skills\*.java",
    "src\arenamirror\weapons\*.java",
    "src\arenamirror\progression\*.java",
    "src\arenamirror\traps\*.java",
    "src\arenamirror\rendering\*.java"
)

$result = javac -d build -encoding UTF8 $src 2>&1
if ($LASTEXITCODE -ne 0) {
    Write-Host "Build FAILED"
    Write-Host $result
    Read-Host
    exit 1
}

Write-Host "Build OK! Running..."
Start-Process -FilePath "C:\Program Files\Java\jdk-21\bin\java.exe" -ArgumentList "-cp","build","arenamirror.Main"
