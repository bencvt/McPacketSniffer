McPacketSniffer is a client-side mod for Minecraft that logs packets sent and
received between the client and a Minecraft server. Packets are logged to a text
file, one packet per line, one file per connection.

Why bother modding the client when you could just use one of dozens of
[external utilities](http://mc.kev009.com/Utility_List)? Several reasons:

1.  Inspecting Minecraft's internal Packet objects rather than sequences of raw
    bytes is less error-prone. Why reinvent the wheel when you can reuse the
    original?

2.  Game state can be readily accessed. For example, instead of just printing an
    entity's ID as it's sent in the packet, this mod can look up the entity,
    outputting its type and current coordinates.

3.  As of 1.3 the protocol is [encrypted](http://mc.kev009.com/Protocol_Encryption).
    This limits external utilities: they either have to be a full-fledged
    Minecraft protocol proxy or they have to interface with the Minecraft client
    somehow (i.e., read memory) to get at the key.

4.  The memory connection, used by the Minecraft client in single player mode to
    talk to the integrated server, uses the protocol too (minus the crypto).
    This internal connection can be inspected using McPacketSniffer as well,
    something you wouldn't be able to do at all using TCP-based utilities.

## Installation

Installing this mod works exactly the same as any other Minecraft client mod.

1.  Download and extract the zip for the latest release.
2.  Patch the contents of the zip file into your `minecraft.jar`, being sure to
    remove the `META-INF` folder.

Utilities like [Magic Launcher](http://www.minecraftforum.net/topic/939149-/)
can automate this process. Highly recommended! Manually copying `.class` files
is for the birds.

Neither [ModLoader](http://www.minecraftforum.net/topic/75440-modloader/) nor
[Forge](http://www.minecraftforge.net/forum/) is required.

## Compatibility

McPacketSniffer modifies `bw.class` (MemoryConnection) and `bx.class`
(TcpConnection). Any other mod that modifies these classes will potentially be
incompatible with McPacketSniffer.

Note that Forge *does* modify these classes. However, McPacketSniffer provides a
compatibility layer, verified working as of Forge 6.0.1. Just make sure to use
McPacketSniffer's version of these classes in your final `minecraft.jar`.

## Usage

Start Minecraft with this mod installed, which will automatically create
`(minecraft dir)/mods/McPacketSniffer/options.txt`. You can open this file in a
text editor to specify exactly what you want to log. Any changes you make will
take effect within a few seconds; no need to restart Minecraft, though some
settings won't take effect until you open a new server connection.

The packet logs are located in `(minecraft dir)/mods/McPacketSniffer/logs`.

Note that the packet logs capture normal client <-> server traffic. This does
*not* include every piece of data sent and received by the Minecraft client.
Known exceptions:

 *  Server pings (client <-> server)
 *  Server login verification (client <-> session.minecraft.net)
 *  Account login (client launcher <-> login.minecraft.net)
 *  Resource downloads (client <-> s3.amazonaws.com)
 *  Skins and capes (client <-> skins.minecraft.net)
 *  Usage reporting (client <-> snoop.minecraft.net)
 *  Mod-specific connections (client <-> wherever, e.g. the mod's website to
    automatically check for updates)

## More info

McPacketSniffer is open source! Visit the official project page at
[github.com/bencvt/McPacketSniffer](https://github.com/bencvt/McPacketSniffer).
Build instructions are located in `README-dev.md`.
