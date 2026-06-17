$dir = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $dir
Start-Process -FilePath "java" -ArgumentList "-jar","ArenaMirror.jar"
