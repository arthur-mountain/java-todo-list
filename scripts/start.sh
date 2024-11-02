#!/usr/bin/env bash

output="app/bin/main/java"
input=("app/src/main/java/$1"/*.java)

javac "${input[@]}" -d "$output"

java -cp "$output" "$1.$2"
