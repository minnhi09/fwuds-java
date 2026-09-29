#!/bin/sh
set -eu
MAVEN_VERSION=3.9.9
MAVEN_HOME="${HOME}/.m2/wrapper/dists/apache-maven-${MAVEN_VERSION}"
if ! command -v java >/dev/null 2>&1; then
  echo "ERROR: Java 21 was not found. Install a JDK and set JAVA_HOME." >&2
  exit 1
fi
if [ ! -x "${MAVEN_HOME}/bin/mvn" ]; then
  archive="${TMPDIR:-/tmp}/apache-maven-${MAVEN_VERSION}-bin.zip"
  url="https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/${MAVEN_VERSION}/apache-maven-${MAVEN_VERSION}-bin.zip"
  mkdir -p "$(dirname "${MAVEN_HOME}")"
  if command -v curl >/dev/null 2>&1; then curl -fsSL "$url" -o "$archive"; else wget -q "$url" -O "$archive"; fi
  actual=$(sha256sum "$archive" | awk '{print $1}')
  [ "$actual" = "4ec3f26fb1a692473aea0235c300bd20f0f9fe741947c82c1234cefd76ac3a3c" ] || { echo "Maven archive checksum mismatch" >&2; exit 1; }
  unzip -q -o "$archive" -d "$(dirname "${MAVEN_HOME}")"
fi
exec "${MAVEN_HOME}/bin/mvn" -f "$(dirname "$0")/pom.xml" "$@"
