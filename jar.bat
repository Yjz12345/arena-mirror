@echo off
echo === 打包 ArenaMirror.jar ===
if not exist build (
    echo 请先运行 build.bat 编译
    pause
    exit /b 1
)
"C:\Program Files\Java\jdk-21\bin\jar" cfe ArenaMirror.jar arenamirror.Main -C build .
if %ERRORLEVEL%==0 (
    echo ArenaMirror.jar 打包完成！
    echo java -jar ArenaMirror.jar 即可运行
) else (
    echo 打包失败
    pause
)
