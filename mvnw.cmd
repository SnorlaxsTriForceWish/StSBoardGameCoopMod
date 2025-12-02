@REM ----------------------------------------------------------------------------
@REM Maven Wrapper for Windows (uses IntelliJ's bundled Maven)
@REM ----------------------------------------------------------------------------

@echo off

set MAVEN_HOME=D:\Program Files\JetBrains\IntelliJ IDEA 2025.2.5\plugins\maven\lib\maven3
set MAVEN_CMD=%MAVEN_HOME%\bin\mvn.cmd

if not exist "%MAVEN_CMD%" (
    echo Error: Maven not found at %MAVEN_CMD%
    echo Please update the MAVEN_HOME path in mvnw.cmd
    exit /b 1
)

"%MAVEN_CMD%" %*
