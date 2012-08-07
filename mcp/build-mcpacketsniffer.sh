#!/bin/bash
set -e

JAR=$PWD/McPacketSniffer-2.0-SNAPSHOT.jar

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
cp src/minecraft/bencvt/minecraft/client/mcpacketsniffer/*.properties reobf/minecraft/bencvt/minecraft/client/mcpacketsniffer/
cd reobf/minecraft/
jar cfv $JAR ./
