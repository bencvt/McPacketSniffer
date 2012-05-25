This is a client-side mod for Minecraft that logs every packet sent and received.

Why bother modding the client when you could just use one of dozens of external utilities?
Two main reasons:

1. Game state can be readily accessed. For example, instead of just printing an entity's ID as it's
sent in the packet, this mod can look up the entity, outputting its type and current coordinates.

2. In 1.3 the protocol will be encrypted, which will make packet sniffing with external utilities
more difficult. See http://mc.kev009.com/Protocol_Encryption for details.
