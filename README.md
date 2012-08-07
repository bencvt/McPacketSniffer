This is a client-side mod for Minecraft that logs packets sent and received
between the client and a Minecraft server. Packets are logged to a text file,
one packet per line, one file per connection.

Why bother modding the client when you could just use one of dozens of external
utilities? Two main reasons:

1. Game state can be readily accessed. For example, instead of just printing an
   entity's ID as it's sent in the packet, this mod can look up the entity,
   outputting its type and current coordinates.

2. As of 1.3 the protocol is [encrypted](http://mc.kev009.com/Protocol_Encryption).
   This limits external utilities: they either have to be a full-fledged
   Minecraft protocol proxy or they have to interface with the Minecraft client
   somehow (i.e., read memory) to get at the key.

Usage
-----

First of all, get the jar.
It's [located here](https://www.dropbox.com/sh/rrg4n7phfksvz9z/Rt3yLMYh7W/minecraft/clientmods/McPacketSniffer),
or you can build it from source (see below).

Patch the jar into your minecraft.jar like you would for any other client mod.
I recommend a utility like
[Magic Launcher](http://www.minecraftforum.net/topic/939149-launcher-magic-launcher-098-mods-options-news/);
manually copying .class files is for the birds.
Make sure you also have ModLoader installed as well.

The config and output files can be found in
`<minecraft directory>/mods/McPacketSniffer/`.

Build instructions
------------------

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
