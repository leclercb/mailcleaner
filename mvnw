#!/bin/sh
#
# Maven Wrapper
#

MVN_VERSION=3.9.6
BASE_DIR="$(cd "$(dirname "$0")" && pwd)"
MVN_DIR="$BASE_DIR/.mvn"
MVN_WRAPPER="$MVN_DIR/wrapper"
MVN_EXEC="$MVN_DIR/apache-maven-$MVN_VERSION/bin/mvn"

if [ ! -f "$MVN_EXEC" ]; then
  echo "Downloading Maven $MVN_VERSION..."
  mkdir -p "$MVN_DIR"
  curl -sL https://archive.apache.org/dist/maven/maven-3/$MVN_VERSION/binaries/apache-maven-$MVN_VERSION-bin.zip -o "$MVN_DIR/maven.zip"
  unzip -q "$MVN_DIR/maven.zip" -d "$MVN_DIR"
  rm "$MVN_DIR/maven.zip"
fi

exec "$MVN_EXEC" "$@"
