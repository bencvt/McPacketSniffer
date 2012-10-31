## Build instructions

Getting git and MCP (Minecraft Coder Pack) to play nice together is somewhat
annoying, but here's how:

1. Clone the repo.
   `git clone git@github.com:bencvt/McPacketSniffer.git`

2. Download the [latest MCP release](http://mcp.ocean-labs.de/index.php/MCP_Releases).
   Extract the contents of the zip to `mcp/`. Don't worry about overwriting
   `mcp/src/`; the files are safe in your local cloned git repo and will be
   restored once MCP is set up.

3. Run the `updatemcp` MCP script. Type Yes when prompted.

4. Copy your Minecraft's `bin/` directory to `mcp/jars/bin/`. Your minecraft.jar
   should be vanilla plus ModLoader patched in.

5. Run the `decompile` MCP script.

6. Type `git checkout .` to restore the modified sources.

7. Run the `build-mcpacketsniffer` script.
   There may be issues if MCP has new packet class field mappings.
