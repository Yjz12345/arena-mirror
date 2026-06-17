@echo off
cd /d "%~dp0"
echo === 角斗场：百层之镜 [Neon] ===
echo Compiling...
if not exist build mkdir build
javac -d build -encoding UTF-8 src\arenamirror\Main.java src\arenamirror\core\*.java src\arenamirror\player\*.java src\arenamirror\enemies\*.java src\arenamirror\data\*.java src\arenamirror\skills\*.java src\arenamirror\weapons\*.java src\arenamirror\progression\*.java src\arenamirror\traps\*.java src\arenamirror\rendering\*.java
if %ERRORLEVEL% NEQ 0 (
    echo Build FAILED
    pause
    exit /b 1
)
echo Build OK!
javaw -Dsun.java2d.opengl=true -cp build arenamirror.Main
