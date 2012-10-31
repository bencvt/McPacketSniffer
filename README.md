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

## Installation

Installing this mod works exactly the same as any other Minecraft client mod.

1.  Make sure that either [ModLoader](http://www.minecraftforum.net/topic/75440-modloader/)
    or [Forge](http://www.minecraftforge.net/forum/) is installed, as this is a
    base requirement.
2.  Download and extract the zip for the latest release.
3.  Patch the contents of the zip file into your `minecraft.jar`, being sure to
    remove the `META-INF` folder.

Utilities like [Magic Launcher](http://www.minecraftforum.net/topic/939149-/)
can automate this process. Highly recommended! Manually copying `.class` files
is for the birds.

## Compatibility

TODO

## Usage

TODO

## More info

McPacketSniffer is open source! Visit the official project page at
[github.com/bencvt/McPacketSniffer](https://github.com/bencvt/McPacketSniffer).
