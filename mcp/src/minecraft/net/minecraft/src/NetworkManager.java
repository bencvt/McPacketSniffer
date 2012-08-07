package net.minecraft.src;

import java.io.*;
import java.net.*;
import java.util.*;

public class NetworkManager
{
    /** Synchronization object used for read and write threads. */
    public static final Object threadSyncObject = new Object();

    /** The number of read threads spawned. Not really used on client side. */
    public static int numReadThreads;

    /** The number of write threads spawned. Not really used on client side. */
    public static int numWriteThreads;

    /** The object used for synchronization on the send queue. */
    private Object sendQueueLock;

    /** The socket used by this network manager. */
    private Socket networkSocket;
    private final SocketAddress remoteSocketAddress;

    /** The input stream connected to the socket. */
    private DataInputStream socketInputStream;

    /** The output stream connected to the socket. */
    private DataOutputStream socketOutputStream;

    /** Whether the network is currently operational. */
    private boolean isRunning;

    /**
     * Linked list of packets that have been read and are awaiting processing.
     */
    private List readPackets;

    /** Linked list of packets awaiting sending. */
    private List dataPackets;

    /** Linked list of packets with chunk data that are awaiting sending. */
    private List chunkDataPackets;

    /** A reference to the NetHandler object. */
    private NetHandler netHandler;

    /**
     * Whether this server is currently terminating. If this is a client, this is always false.
     */
    private boolean isServerTerminating;

    /** The thread used for writing. */
    private Thread writeThread;

    /** The thread used for reading. */
    private Thread readThread;

    /**
     * Whether this network manager is currently terminating (and should ignore further errors).
     */
    private boolean isTerminating;

    /** A String indicating why the network has shutdown. */
    private String terminationReason;
    private Object field_20101_t[];

    /**
     * Counter used to detect read timeouts after 1200 failed attempts to read a packet.
     */
    private int timeSinceLastRead;

    /**
     * The length in bytes of the packets in both send queues (data and chunkData).
     */
    private int sendQueueByteLength;
    public static int field_28145_d[] = new int[256];
    public static int field_28144_e[] = new int[256];

    /**
     * Counter used to prevent us from sending too many chunk data packets one after another. The delay appears to be
     * set to 50.
     */
    public int chunkDataSendCounter;
    private int field_20100_w;

    // Begin modified code
    public static PacketHooks packetHooks = new PacketHooks();
    // End modified code

    public NetworkManager(Socket par1Socket, String par2Str, NetHandler par3NetHandler) throws IOException
    {
        sendQueueLock = new Object();
        isRunning = true;
        readPackets = Collections.synchronizedList(new ArrayList());
        dataPackets = Collections.synchronizedList(new ArrayList());
        chunkDataPackets = Collections.synchronizedList(new ArrayList());
        isServerTerminating = false;
        isTerminating = false;
        terminationReason = "";
        timeSinceLastRead = 0;
        sendQueueByteLength = 0;
        chunkDataSendCounter = 0;
        field_20100_w = 50;
        networkSocket = par1Socket;
        remoteSocketAddress = par1Socket.getRemoteSocketAddress();
        netHandler = par3NetHandler;

        try
        {
            par1Socket.setSoTimeout(30000);
            par1Socket.setTrafficClass(24);
        }
        catch (SocketException socketexception)
        {
            System.err.println(socketexception.getMessage());
        }

        socketInputStream = new DataInputStream(par1Socket.getInputStream());
        socketOutputStream = new DataOutputStream(new BufferedOutputStream(par1Socket.getOutputStream(), 5120));
        readThread = new NetworkReaderThread(this, (new StringBuilder()).append(par2Str).append(" read thread").toString());
        writeThread = new NetworkWriterThread(this, (new StringBuilder()).append(par2Str).append(" write thread").toString());
        readThread.start();
        writeThread.start();
    }

    /**
     * Adds the packet to the correct send queue (chunk data packets go to a separate queue).
     */
    public void addToSendQueue(Packet par1Packet)
    {
        if (isServerTerminating)
        {
            return;
        }

        synchronized (sendQueueLock)
        {
            sendQueueByteLength += par1Packet.getPacketSize() + 1;

            if (par1Packet.isChunkDataPacket)
            {
                chunkDataPackets.add(par1Packet);
            }
            else
            {
                dataPackets.add(par1Packet);
            }
        }
    }

    /**
     * Sends a data packet if there is one to send, or sends a chunk data packet if there is one and the counter is up,
     * or does nothing. If it sends a packet, it sleeps for 10ms.
     */
    private boolean sendPacket()
    {
        boolean flag = false;

        try
        {
            if (!dataPackets.isEmpty() && (chunkDataSendCounter == 0 || System.currentTimeMillis() - ((Packet)dataPackets.get(0)).creationTimeMillis >= (long)chunkDataSendCounter))
            {
                Packet packet;

                synchronized (sendQueueLock)
                {
                    packet = (Packet)dataPackets.remove(0);
                    sendQueueByteLength -= packet.getPacketSize() + 1;
                }

                // ==== Begin modified code
                packetHooks.dispatchEvent(packet, true);
                // ==== End modified code
                Packet.writePacket(packet, socketOutputStream);
                field_28144_e[packet.getPacketId()] += packet.getPacketSize() + 1;
                flag = true;
            }

            if (field_20100_w-- <= 0 && !chunkDataPackets.isEmpty() && (chunkDataSendCounter == 0 || System.currentTimeMillis() - ((Packet)chunkDataPackets.get(0)).creationTimeMillis >= (long)chunkDataSendCounter))
            {
                Packet packet1;

                synchronized (sendQueueLock)
                {
                    packet1 = (Packet)chunkDataPackets.remove(0);
                    sendQueueByteLength -= packet1.getPacketSize() + 1;
                }

                // ==== Begin modified code
                packetHooks.dispatchEvent(packet1, true);
                // ==== End modified code
                Packet.writePacket(packet1, socketOutputStream);
                field_28144_e[packet1.getPacketId()] += packet1.getPacketSize() + 1;
                field_20100_w = 0;
                flag = true;
            }
        }
        catch (Exception exception)
        {
            if (!isTerminating)
            {
                onNetworkError(exception);
            }

            return false;
        }

        return flag;
    }

    /**
     * Wakes reader and writer threads
     */
    public void wakeThreads()
    {
        readThread.interrupt();
        writeThread.interrupt();
    }

    /**
     * Reads a single packet from the input stream and adds it to the read queue. If no packet is read, it shuts down
     * the network.
     */
    private boolean readPacket()
    {
        boolean flag = false;

        try
        {
            Packet packet = Packet.readPacket(socketInputStream, netHandler.isServerHandler());

            if (packet != null)
            {
                field_28145_d[packet.getPacketId()] += packet.getPacketSize() + 1;

                if (!isServerTerminating)
                {
                    readPackets.add(packet);
                }

                flag = true;
            }
            else
            {
                networkShutdown("disconnect.endOfStream", new Object[0]);
            }
        }
        catch (Exception exception)
        {
            if (!isTerminating)
            {
                onNetworkError(exception);
            }

            return false;
        }

        return flag;
    }

    /**
     * Used to report network errors and causes a network shutdown.
     */
    private void onNetworkError(Exception par1Exception)
    {
        par1Exception.printStackTrace();
        networkShutdown("disconnect.genericReason", new Object[]
                {
                    (new StringBuilder()).append("Internal exception: ").append(par1Exception.toString()).toString()
                });
    }

    /**
     * Shuts down the network with the specified reason. Closes all streams and sockets, spawns NetworkMasterThread to
     * stop reading and writing threads.
     */
    public void networkShutdown(String par1Str, Object par2ArrayOfObj[])
    {
        if (!isRunning)
        {
            return;
        }

        isTerminating = true;
        terminationReason = par1Str;
        field_20101_t = par2ArrayOfObj;
        (new NetworkMasterThread(this)).start();
        isRunning = false;

        try
        {
            socketInputStream.close();
            socketInputStream = null;
        }
        catch (Throwable throwable) { }

        try
        {
            socketOutputStream.close();
            socketOutputStream = null;
        }
        catch (Throwable throwable1) { }

        try
        {
            networkSocket.close();
            networkSocket = null;
        }
        catch (Throwable throwable2) { }
    }

    /**
     * Checks timeouts and processes all pending read packets.
     */
    public void processReadPackets()
    {
        if (sendQueueByteLength > 0x100000)
        {
            networkShutdown("disconnect.overflow", new Object[0]);
        }

        if (readPackets.isEmpty())
        {
            if (timeSinceLastRead++ == 1200)
            {
                networkShutdown("disconnect.timeout", new Object[0]);
            }
        }
        else
        {
            timeSinceLastRead = 0;
        }

        Packet packet;

        for (int i = 1000; !readPackets.isEmpty() && i-- >= 0; packet.processPacket(netHandler))
        {
            packet = (Packet)readPackets.remove(0);
            // ==== Begin modified code
            packetHooks.dispatchEvent(packet, false);
            // ==== End modified code
        }

        wakeThreads();

        if (isTerminating && readPackets.isEmpty())
        {
            netHandler.handleErrorMessage(terminationReason, field_20101_t);
        }
    }

    /**
     * Shuts down the server. (Only actually used on the server)
     */
    public void serverShutdown()
    {
        if (isServerTerminating)
        {
            return;
        }
        else
        {
            wakeThreads();
            isServerTerminating = true;
            readThread.interrupt();
            (new ThreadMonitorConnection(this)).start();
            return;
        }
    }

    /**
     * Whether the network is operational.
     */
    static boolean isRunning(NetworkManager par0NetworkManager)
    {
        return par0NetworkManager.isRunning;
    }

    /**
     * Is the server terminating? Client side aways returns false.
     */
    static boolean isServerTerminating(NetworkManager par0NetworkManager)
    {
        return par0NetworkManager.isServerTerminating;
    }

    /**
     * Static accessor to readPacket.
     */
    static boolean readNetworkPacket(NetworkManager par0NetworkManager)
    {
        return par0NetworkManager.readPacket();
    }

    /**
     * Static accessor to sendPacket.
     */
    static boolean sendNetworkPacket(NetworkManager par0NetworkManager)
    {
        return par0NetworkManager.sendPacket();
    }

    static DataOutputStream getOutputStream(NetworkManager par0NetworkManager)
    {
        return par0NetworkManager.socketOutputStream;
    }

    /**
     * Gets whether the Network manager is terminating.
     */
    static boolean isTerminating(NetworkManager par0NetworkManager)
    {
        return par0NetworkManager.isTerminating;
    }

    /**
     * Sends the network manager an error
     */
    static void sendError(NetworkManager par0NetworkManager, Exception par1Exception)
    {
        par0NetworkManager.onNetworkError(par1Exception);
    }

    /**
     * Returns the read thread.
     */
    static Thread getReadThread(NetworkManager par0NetworkManager)
    {
        return par0NetworkManager.readThread;
    }

    /**
     * Returns the write thread.
     */
    static Thread getWriteThread(NetworkManager par0NetworkManager)
    {
        return par0NetworkManager.writeThread;
    }
}
