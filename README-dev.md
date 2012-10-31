## Build instructions

If you just want to install the mod, skip this section and refer to the
instructions in the main `README.md` document.

### Stuff you'll need:

 +  The Java Development Kit (JDK)
 +  A copy of `minecraft.jar` from your Minecraft installation
 +  [Minecraft Coder Pack (MCP)](http://mcp.ocean-labs.de/index.php/MCP_Releases)
 +  [ModLoader](http://www.minecraftforum.net/topic/75440-modloader/)

### Step-by-step

1.  Patch `minecraft.jar` to include ModLoader, then install and configure MCP
    using the modified `minecraft.jar`.  
2.  Make sure you're able to recompile Minecraft before the next step.
3.  Copy everything from this repo's `src/` directory to `src/minecraft/`.
4.  Recompile, reobfuscate, and package the jar/zip. If you're on a Unix-based
    system, you can use the `build-mcpacketsniffer.sh` script for this.
