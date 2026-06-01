@echo off
echo === 角斗场：百层之镜 ===
echo Compiling...

if not exist build mkdir build
javac -d build -encoding UTF-8 src\arenamirror\Main.java src\arenamirror\core\*.java src\arenamirror\player\*.java src\arenamirror\enemies\*.java src\arenamirror\data\*.java src\arenamirror\skills\*.java src\arenamirror\weapons\*.java src\arenamirror\progression\*.java src\arenamirror\traps\*.java src\arenamirror\rendering\*.java

if %ERRORLEVEL% == 0 (
    echo Build successful!
    echo Running...
    java -cp build arenamirror.Main
) else (
    echo Build failed.
    pause
)
