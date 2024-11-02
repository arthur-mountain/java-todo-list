#!/usr/bin/env bash
set -e

./gradlew fatJar

JAR_FILE="app/build/libs/app-all.jar"

if [ -f "$JAR_FILE" ]; then
  java -jar "$JAR_FILE"
else
  echo "Error: JAR file not found at $JAR_FILE"
  exit 1
fi
