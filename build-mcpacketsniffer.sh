#!/bin/bash
set -e

JAR=$PWD/McPacketSniffer-3.0-SNAPSHOT.jar

rm $JAR || true

# ugh, these scripts exit with success status even if they failed...
# as a workaround check their output directories

echo "== Compiling =="
./recompile.sh
[ "$(ls bin/minecraft/net/minecraft/src/)" ] || exit 1

echo "== Reobfuscating =="
./reobfuscate.sh
[ "$(ls reobf/minecraft/)" ] || exit 1

echo "== Packaging $JAR =="
cd reobf/minecraft/
cp src/minecraft/com/bencvt/minecraft/mcpacketsniffer/*.properties com/bencvt/minecraft/mcpacketsniffer/
jar cfv $JAR ./
