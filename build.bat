@echo off
echo === 角斗场：百层之镜 ===
echo Cleaning...
if exist build rmdir /s /q build
mkdir build
echo Compiling...
javac -d build -encoding UTF-8 src\arenamirror\Main.java src\arenamirror\core\*.java src\arenamirror\player\*.java src\arenamirror\enemies\*.java src\arenamirror\data\*.java src\arenamirror\skills\*.java src\arenamirror\weapons\*.java src\arenamirror\progression\*.java src\arenamirror\traps\*.java src\arenamirror\rendering\*.java
if %ERRORLEVEL% NEQ 0 (
    echo Build failed.
    pause
    exit /b 1
)
echo Build successful!
echo Packing JAR...
jar cfe ArenaMirror.jar arenamirror.Main -C build .
if %ERRORLEVEL% EQU 0 (
    echo JAR created: ArenaMirror.jar
    echo Running...
    java -jar ArenaMirror.jar
) else (
    echo JAR creation failed.
    pause
)
