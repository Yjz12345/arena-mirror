Write-Host "=== 角斗场：百层之镜 ===" -ForegroundColor Cyan
Write-Host "Compiling..."

$src = Get-ChildItem -Path "src" -Recurse -Filter "*.java" | Select-Object -ExpandProperty FullName
$null = New-Item -ItemType Directory -Force -Path "build"

& javac -d build -encoding UTF-8 $src

if ($LASTEXITCODE -eq 0) {
    Write-Host "Build successful!" -ForegroundColor Green
    Write-Host "Running..."
    & java -cp build arenamirror.Main
} else {
    Write-Host "Build failed." -ForegroundColor Red
}
