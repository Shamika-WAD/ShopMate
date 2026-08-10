#!/bin/sh
# Gradle start up script for POSIX (Linux/macOS).
# Android Studio handles Gradle itself — this is only needed for command-line builds.

APP_HOME="$(cd "$(dirname "$0")" >/dev/null 2>&1 && pwd -P)"
CLASSPATH="$APP_HOME/gradle/wrapper/gradle-wrapper.jar"

if [ -n "$JAVA_HOME" ]; then
    JAVACMD="$JAVA_HOME/bin/java"
    if [ ! -x "$JAVACMD" ]; then
        echo "ERROR: JAVA_HOME is set to an invalid directory: $JAVA_HOME" >&2
        exit 1
    fi
else
    JAVACMD="java"
    command -v java >/dev/null 2>&1 || {
        echo "ERROR: 'java' not found. Install JDK 17 or set JAVA_HOME." >&2
        exit 1
    }
fi

exec "$JAVACMD" $DEFAULT_JVM_OPTS $JAVA_OPTS $GRADLE_OPTS \
    -classpath "$CLASSPATH" \
    org.gradle.wrapper.GradleWrapperMain "$@"
