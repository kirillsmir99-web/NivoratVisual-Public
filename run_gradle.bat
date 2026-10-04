@echo off
setlocal
if "%JAVA_HOME%"=="" (
    echo Error: JAVA_HOME environment variable is not set. Please set JAVA_HOME to JDK 21.
    exit /b 1
)
if not exist "%JAVA_HOME%\bin\java.exe" (
    echo Error: java.exe was not found in JAVA_HOME: %JAVA_HOME%
    exit /b 1
)
set "PATH=%JAVA_HOME%\bin;%PATH%"
call "%~dp0gradlew.bat" %*
