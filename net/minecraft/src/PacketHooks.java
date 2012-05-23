package net.minecraft.src;

import java.util.LinkedHashSet;

public class PacketHooks {
	public interface IPacketEventListener {
		public void onPacket(Packet packet, boolean isSend);
	}

	private static LinkedHashSet<IPacketEventListener> packetEventListeners = new LinkedHashSet();

	public static void register(IPacketEventListener listener) {
		try {
			// make sure our modifications to NetworkManager and GuiMultiplayer are present
			if (!NetworkManager.packetHooks.getClass().equals(PacketHooks.class) ||
					!GuiMultiplayer.packetHooks.getClass().equals(PacketHooks.class)) {
				throw new RuntimeException("internal error");
			}
		} catch (LinkageError e) {
			throw new RuntimeException("Unable to register packet events. This is most likely due to a mod overwriting lg.class (NetworkManager.java) or acp.class (GuiMultiplayer.java).", e);
		}
		packetEventListeners.add(listener);
	}

	public static void unregister(IPacketEventListener listener) {
		packetEventListeners.remove(listener);
	}

    // ====
	// ==== Dispatch functions, should only be called from modified vanilla classes
	// ====

	protected void dispatchEvent(Packet packet, boolean isSend) {
		for (IPacketEventListener listener : packetEventListeners) {
			listener.onPacket(packet, isSend);
		}
	}

	/** Called when there are multiple sockets open (i.e. when pinging each server on the server list) */
	protected synchronized void dispatchEventSynchronized(Packet packet, boolean isSend) {
		for (IPacketEventListener listener : packetEventListeners) {
			listener.onPacket(packet, isSend);
		}
	}
}
