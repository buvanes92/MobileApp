#!/usr/bin/env sh

# Determine APP_HOME
PRG="$0"
while [ -h "$PRG" ] ; do
    ls=`ls -ld "$PRG"`
    link=`expr "$ls" : '.*-> \(.*\)$'`
    if expr "$link" : '/.*' > /dev/null; then
        PRG="$link"
    else
        PRG=`dirname "$PRG"`/"$link"
    fi
done
APP_HOME=`dirname "$PRG"`
APP_HOME=`cd "$APP_HOME" && pwd`

CLASSPATH="$APP_HOME/gradle/wrapper/gradle-wrapper.jar"

# Find Java
if [ -n "$JAVA_HOME" ] ; then
    if [ -x "$JAVA_HOME/bin/java" ] ; then
        JAVACMD="$JAVA_HOME/bin/java"
    else
        echo "ERROR: JAVA_HOME is set to an invalid directory: $JAVA_HOME" >&2
        exit 1
    fi
else
    JAVACMD="java"
    which java >/dev/null 2>&1 || {
        echo "ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH." >&2
        exit 1
    }
fi

# JVM options
DEFAULT_JVM_OPTS="-Xmx2048m -XX:MaxMetaspaceSize=512m"

# Execute Gradle Wrapper
exec "$JAVACMD" $DEFAULT_JVM_OPTS $JAVA_OPTS $GRADLE_OPTS \
    "-Dorg.gradle.appname=gradlew" \
    -classpath "$CLASSPATH" \
    org.gradle.wrapper.GradleWrapperMain "$@"
