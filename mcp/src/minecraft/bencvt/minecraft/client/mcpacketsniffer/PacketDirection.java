package bencvt.minecraft.client.mcpacketsniffer;

public enum PacketDirection {
    C2S("client to server"),
    S2C("server to client");

    public final String description;
    private PacketDirection(String description) {
        this.description = description;
    }
}
