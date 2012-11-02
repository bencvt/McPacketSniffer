#!/bin/bash
set -e

# replace with version number if creating a release
# don't forget to update Controller.VERSION and mcmod.info
JAR=$PWD/McPacketSniffer-SNAPSHOT.zip

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
rm -rf META-INF

# resources
cp -v ../../src/minecraft/*.info .
cp -v ../../src/minecraft/com/bencvt/minecraft/mcpacketsniffer/*.properties com/bencvt/minecraft/mcpacketsniffer/

#jar cfv $JAR ./
zip -r $JAR *
