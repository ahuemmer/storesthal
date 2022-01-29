#!/bin/bash

echo "version = $1"

# Get version number from version tag
JAR_VERSION=`echo $1 | cut -d'v' -f2`
echo "jar = $JAR_VERSION"

./gradlew -si -Pversion=${JAR_VERSION} build sourcesJar javadocJar publish
