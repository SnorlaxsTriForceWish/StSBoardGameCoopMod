#!/bin/sh
# ----------------------------------------------------------------------------
# Maven Wrapper for Unix-like systems
# ----------------------------------------------------------------------------

# Attempt to set APP_HOME
# Resolve links: $0 may be a link
PRG="$0"
while [ -h "$PRG" ] ; do
    ls=`ls -ld "$PRG"`
    link=`expr "$ls" : '.*-> \(.*\)$'`
    if expr "$link" : '/.*' > /dev/null; then
        PRG="$link"
    else
        PRG=`dirname "$PRG"`"/$link"
    fi
done
SAVED="`pwd`"
cd "`dirname \"$PRG\"`/" >/dev/null
APP_HOME="`pwd -P`"
cd "$SAVED" >/dev/null

# Determine the default Maven location based on OS
case "`uname`" in
    Darwin*)
        MAVEN_HOME="$HOME/Library/Application Support/JetBrains/IntelliJIdea2025.2/plugins/maven/lib/maven3"
        ;;
    Linux*)
        MAVEN_HOME="$HOME/.local/share/JetBrains/IntelliJIdea2025.2/plugins/maven/lib/maven3"
        ;;
    *)
        echo "Unsupported OS. Please install Maven manually or update mvnw script."
        exit 1
        ;;
esac

if [ ! -f "$MAVEN_HOME/bin/mvn" ]; then
    echo "Error: Maven not found at $MAVEN_HOME"
    echo "Please install Maven or update MAVEN_HOME in mvnw script"
    exit 1
fi

exec "$MAVEN_HOME/bin/mvn" "$@"
