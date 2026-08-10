@echo off
rem Gradle start up script for Windows.
rem Android Studio handles Gradle itself - this is only needed for command-line builds.

set DIR=%~dp0
set CLASSPATH=%DIR%\gradle\wrapper\gradle-wrapper.jar

if defined JAVA_HOME (
    set JAVA_EXE=%JAVA_HOME%\bin\java.exe
) else (
    set JAVA_EXE=java.exe
)

"%JAVA_EXE%" %DEFAULT_JVM_OPTS% %JAVA_OPTS% %GRADLE_OPTS% -classpath "%CLASSPATH%" org.gradle.wrapper.GradleWrapperMain %*
