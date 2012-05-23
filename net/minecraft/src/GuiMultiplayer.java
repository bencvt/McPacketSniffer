package net.minecraft.src;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import org.lwjgl.input.Keyboard;

public class GuiMultiplayer extends GuiScreen
{
    /** Number of outstanding ThreadPollServers threads */
    private static int threadsPending = 0;

    /** Lock object for use with synchronized() */
    private static Object lock = new Object();

    /**
     * A reference to the screen object that created this. Used for navigating between screens.
     */
    private GuiScreen parentScreen;

    /** Slot container for the server list */
    private GuiSlotServer serverSlotContainer;

    /** List of ServerNBTStorage objects */
    private List serverList;

    /** Index of the currently selected server */
    private int selectedServer;

    /** The 'Edit' button */
    private GuiButton buttonEdit;

    /** The 'Join Server' button */
    private GuiButton buttonSelect;

    /** The 'Delete' button */
    private GuiButton buttonDelete;

    /** The 'Delete' button was clicked */
    private boolean deleteClicked;

    /** The 'Add server' button was clicked */
    private boolean addClicked;

    /** The 'Edit' button was clicked */
    private boolean editClicked;

    /** The 'Direct Connect' button was clicked */
    private boolean directClicked;

    /** This GUI's lag tooltip text or null if no lag icon is being hovered. */
    private String lagTooltip;

    /**
     * Temporary ServerNBTStorage used by the Edit/Add/Direct Connect dialogs
     */
    private ServerNBTStorage tempServer;

    public GuiMultiplayer(GuiScreen par1GuiScreen)
    {
        serverList = new ArrayList();
        selectedServer = -1;
        deleteClicked = false;
        addClicked = false;
        editClicked = false;
        directClicked = false;
        lagTooltip = null;
        tempServer = null;
        parentScreen = par1GuiScreen;
    }

    /**
     * Called from the main game loop to update the screen.
     */
    public void updateScreen()
    {
    }

    /**
     * Adds the buttons (and other controls) to the screen in question.
     */
    public void initGui()
    {
        loadServerList();
        Keyboard.enableRepeatEvents(true);
        controlList.clear();
        serverSlotContainer = new GuiSlotServer(this);
        initGuiControls();
    }

    /**
     * Load the server list from servers.dat
     */
    private void loadServerList()
    {
        try
        {
            NBTTagCompound nbttagcompound = CompressedStreamTools.read(new File(mc.mcDataDir, "servers.dat"));
            NBTTagList nbttaglist = nbttagcompound.getTagList("servers");
            serverList.clear();

            for (int i = 0; i < nbttaglist.tagCount(); i++)
            {
                serverList.add(ServerNBTStorage.createServerNBTStorage((NBTTagCompound)nbttaglist.tagAt(i)));
            }
        }
        catch (Exception exception)
        {
            exception.printStackTrace();
        }
    }

    /**
     * Save the server list to servers.dat
     */
    private void saveServerList()
    {
        try
        {
            NBTTagList nbttaglist = new NBTTagList();

            for (int i = 0; i < serverList.size(); i++)
            {
                nbttaglist.appendTag(((ServerNBTStorage)serverList.get(i)).getCompoundTag());
            }

            NBTTagCompound nbttagcompound = new NBTTagCompound();
            nbttagcompound.setTag("servers", nbttaglist);
            CompressedStreamTools.safeWrite(nbttagcompound, new File(mc.mcDataDir, "servers.dat"));
        }
        catch (Exception exception)
        {
            exception.printStackTrace();
        }
    }

    /**
     * Populate the GuiScreen controlList
     */
    public void initGuiControls()
    {
        StringTranslate stringtranslate = StringTranslate.getInstance();
        controlList.add(buttonEdit = new GuiButton(7, width / 2 - 154, height - 28, 70, 20, stringtranslate.translateKey("selectServer.edit")));
        controlList.add(buttonDelete = new GuiButton(2, width / 2 - 74, height - 28, 70, 20, stringtranslate.translateKey("selectServer.delete")));
        controlList.add(buttonSelect = new GuiButton(1, width / 2 - 154, height - 52, 100, 20, stringtranslate.translateKey("selectServer.select")));
        controlList.add(new GuiButton(4, width / 2 - 50, height - 52, 100, 20, stringtranslate.translateKey("selectServer.direct")));
        controlList.add(new GuiButton(3, width / 2 + 4 + 50, height - 52, 100, 20, stringtranslate.translateKey("selectServer.add")));
        controlList.add(new GuiButton(8, width / 2 + 4, height - 28, 70, 20, stringtranslate.translateKey("selectServer.refresh")));
        controlList.add(new GuiButton(0, width / 2 + 4 + 76, height - 28, 75, 20, stringtranslate.translateKey("gui.cancel")));
        boolean flag = selectedServer >= 0 && selectedServer < serverSlotContainer.getSize();
        buttonSelect.enabled = flag;
        buttonEdit.enabled = flag;
        buttonDelete.enabled = flag;
    }

    /**
     * Called when the screen is unloaded. Used to disable keyboard repeat events
     */
    public void onGuiClosed()
    {
        Keyboard.enableRepeatEvents(false);
    }

    /**
     * Fired when a control is clicked. This is the equivalent of ActionListener.actionPerformed(ActionEvent e).
     */
    protected void actionPerformed(GuiButton par1GuiButton)
    {
        if (!par1GuiButton.enabled)
        {
            return;
        }

        if (par1GuiButton.id == 2)
        {
            String s = ((ServerNBTStorage)serverList.get(selectedServer)).name;

            if (s != null)
            {
                deleteClicked = true;
                StringTranslate stringtranslate = StringTranslate.getInstance();
                String s1 = stringtranslate.translateKey("selectServer.deleteQuestion");
                String s2 = (new StringBuilder()).append("'").append(s).append("' ").append(stringtranslate.translateKey("selectServer.deleteWarning")).toString();
                String s3 = stringtranslate.translateKey("selectServer.deleteButton");
                String s4 = stringtranslate.translateKey("gui.cancel");
                GuiYesNo guiyesno = new GuiYesNo(this, s1, s2, s3, s4, selectedServer);
                mc.displayGuiScreen(guiyesno);
            }
        }
        else if (par1GuiButton.id == 1)
        {
            joinServer(selectedServer);
        }
        else if (par1GuiButton.id == 4)
        {
            directClicked = true;
            mc.displayGuiScreen(new GuiScreenServerList(this, tempServer = new ServerNBTStorage(StatCollector.translateToLocal("selectServer.defaultName"), "")));
        }
        else if (par1GuiButton.id == 3)
        {
            addClicked = true;
            mc.displayGuiScreen(new GuiScreenAddServer(this, tempServer = new ServerNBTStorage(StatCollector.translateToLocal("selectServer.defaultName"), "")));
        }
        else if (par1GuiButton.id == 7)
        {
            editClicked = true;
            ServerNBTStorage servernbtstorage = (ServerNBTStorage)serverList.get(selectedServer);
            mc.displayGuiScreen(new GuiScreenAddServer(this, tempServer = new ServerNBTStorage(servernbtstorage.name, servernbtstorage.host)));
        }
        else if (par1GuiButton.id == 0)
        {
            mc.displayGuiScreen(parentScreen);
        }
        else if (par1GuiButton.id == 8)
        {
            mc.displayGuiScreen(new GuiMultiplayer(parentScreen));
        }
        else
        {
            serverSlotContainer.actionPerformed(par1GuiButton);
        }
    }

    public void confirmClicked(boolean par1, int par2)
    {
        if (deleteClicked)
        {
            deleteClicked = false;

            if (par1)
            {
                serverList.remove(par2);
                saveServerList();
            }

            mc.displayGuiScreen(this);
        }
        else if (directClicked)
        {
            directClicked = false;

            if (par1)
            {
                joinServer(tempServer);
            }
            else
            {
                mc.displayGuiScreen(this);
            }
        }
        else if (addClicked)
        {
            addClicked = false;

            if (par1)
            {
                serverList.add(tempServer);
                saveServerList();
            }

            mc.displayGuiScreen(this);
        }
        else if (editClicked)
        {
            editClicked = false;

            if (par1)
            {
                ServerNBTStorage servernbtstorage = (ServerNBTStorage)serverList.get(selectedServer);
                servernbtstorage.name = tempServer.name;
                servernbtstorage.host = tempServer.host;
                saveServerList();
            }

            mc.displayGuiScreen(this);
        }
    }

    private int parseIntWithDefault(String par1Str, int par2)
    {
        try
        {
            return Integer.parseInt(par1Str.trim());
        }
        catch (Exception exception)
        {
            return par2;
        }
    }

    /**
     * Fired when a key is typed. This is the equivalent of KeyListener.keyTyped(KeyEvent e).
     */
    protected void keyTyped(char par1, int par2)
    {
        if (par1 == '\r')
        {
            actionPerformed((GuiButton)controlList.get(2));
        }
    }

    /**
     * Called when the mouse is clicked.
     */
    protected void mouseClicked(int par1, int par2, int par3)
    {
        super.mouseClicked(par1, par2, par3);
    }

    /**
     * Draws the screen and all the components in it.
     */
    public void drawScreen(int par1, int par2, float par3)
    {
        lagTooltip = null;
        StringTranslate stringtranslate = StringTranslate.getInstance();
        drawDefaultBackground();
        serverSlotContainer.drawScreen(par1, par2, par3);
        drawCenteredString(fontRenderer, stringtranslate.translateKey("multiplayer.title"), width / 2, 20, 0xffffff);
        super.drawScreen(par1, par2, par3);

        if (lagTooltip != null)
        {
            func_35325_a(lagTooltip, par1, par2);
        }
    }

    /**
     * Join server by slot index
     */
    private void joinServer(int par1)
    {
        joinServer((ServerNBTStorage)serverList.get(par1));
    }

    /**
     * Join server by ServerNBTStorage
     */
    private void joinServer(ServerNBTStorage par1ServerNBTStorage)
    {
        String s = par1ServerNBTStorage.host;
        String as[] = s.split(":");

        if (s.startsWith("["))
        {
            int i = s.indexOf("]");

            if (i > 0)
            {
                String s1 = s.substring(1, i);
                String s2 = s.substring(i + 1).trim();

                if (s2.startsWith(":") && s2.length() > 0)
                {
                    s2 = s2.substring(1);
                    as = new String[2];
                    as[0] = s1;
                    as[1] = s2;
                }
                else
                {
                    as = new String[1];
                    as[0] = s1;
                }
            }
        }

        if (as.length > 2)
        {
            as = new String[1];
            as[0] = s;
        }

        mc.displayGuiScreen(new GuiConnecting(mc, as[0], as.length <= 1 ? 25565 : parseIntWithDefault(as[1], 25565)));
    }

    /**
     * Poll server for MOTD, lag, and player count/max
     */
    private void pollServer(ServerNBTStorage par1ServerNBTStorage) throws IOException
    {
        String s = par1ServerNBTStorage.host;
        String as[] = s.split(":");

        if (s.startsWith("["))
        {
            int i = s.indexOf("]");

            if (i > 0)
            {
                String s2 = s.substring(1, i);
                String s3 = s.substring(i + 1).trim();

                if (s3.startsWith(":") && s3.length() > 0)
                {
                    s3 = s3.substring(1);
                    as = new String[2];
                    as[0] = s2;
                    as[1] = s3;
                }
                else
                {
                    as = new String[1];
                    as[0] = s2;
                }
            }
        }

        if (as.length > 2)
        {
            as = new String[1];
            as[0] = s;
        }

        String s1 = as[0];
        int j = as.length <= 1 ? 25565 : parseIntWithDefault(as[1], 25565);
        Socket socket = null;
        DataInputStream datainputstream = null;
        DataOutputStream dataoutputstream = null;

        try
        {
            socket = new Socket();
            socket.setSoTimeout(3000);
            socket.setTcpNoDelay(true);
            socket.setTrafficClass(18);
            socket.connect(new InetSocketAddress(s1, j), 3000);
            datainputstream = new DataInputStream(socket.getInputStream());
            dataoutputstream = new DataOutputStream(socket.getOutputStream());
            // ==== Begin modified code
            mod_McPacketSniffer.instance.onPacketThreadSafe(new Packet254ServerPing(), true);
            // ==== End modified code
            dataoutputstream.write(254);

            if (datainputstream.read() != 255)
            {
                throw new IOException("Bad message");
            }

            String s4 = Packet.readString(datainputstream, 256);
            // ==== Begin modified code
            mod_McPacketSniffer.instance.onPacketThreadSafe(new Packet255KickDisconnect(s4), false);
            // ==== End modified code
            char ac[] = s4.toCharArray();

            for (int k = 0; k < ac.length; k++)
            {
                if (ac[k] != '\247' && ChatAllowedCharacters.allowedCharacters.indexOf(ac[k]) < 0)
                {
                    ac[k] = '?';
                }
            }

            s4 = new String(ac);
            String as1[] = s4.split("\247");
            s4 = as1[0];
            int l = -1;
            int i1 = -1;

            try
            {
                l = Integer.parseInt(as1[1]);
                i1 = Integer.parseInt(as1[2]);
            }
            catch (Exception exception) { }

            par1ServerNBTStorage.motd = (new StringBuilder()).append("\2477").append(s4).toString();

            if (l >= 0 && i1 > 0)
            {
                par1ServerNBTStorage.playerCount = (new StringBuilder()).append("\2477").append(l).append("\2478/\2477").append(i1).toString();
            }
            else
            {
                par1ServerNBTStorage.playerCount = "\2478???";
            }
        }
        finally
        {
            try
            {
                if (datainputstream != null)
                {
                    datainputstream.close();
                }
            }
            catch (Throwable throwable) { }

            try
            {
                if (dataoutputstream != null)
                {
                    dataoutputstream.close();
                }
            }
            catch (Throwable throwable1) { }

            try
            {
                if (socket != null)
                {
                    socket.close();
                }
            }
            catch (Throwable throwable2) { }
        }
    }

    protected void func_35325_a(String par1Str, int par2, int par3)
    {
        if (par1Str == null)
        {
            return;
        }
        else
        {
            int i = par2 + 12;
            int j = par3 - 12;
            int k = fontRenderer.getStringWidth(par1Str);
            drawGradientRect(i - 3, j - 3, i + k + 3, j + 8 + 3, 0xc0000000, 0xc0000000);
            fontRenderer.drawStringWithShadow(par1Str, i, j, -1);
            return;
        }
    }

    /**
     * Return the List of ServerNBTStorage objects
     */
    static List getServerList(GuiMultiplayer par0GuiMultiplayer)
    {
        return par0GuiMultiplayer.serverList;
    }

    /**
     * Set index of the currently selected server
     */
    static int setSelectedServer(GuiMultiplayer par0GuiMultiplayer, int par1)
    {
        return par0GuiMultiplayer.selectedServer = par1;
    }

    /**
     * Return index of the currently selected server
     */
    static int getSelectedServer(GuiMultiplayer par0GuiMultiplayer)
    {
        return par0GuiMultiplayer.selectedServer;
    }

    /**
     * Return buttonSelect GuiButton
     */
    static GuiButton getButtonSelect(GuiMultiplayer par0GuiMultiplayer)
    {
        return par0GuiMultiplayer.buttonSelect;
    }

    /**
     * Return buttonEdit GuiButton
     */
    static GuiButton getButtonEdit(GuiMultiplayer par0GuiMultiplayer)
    {
        return par0GuiMultiplayer.buttonEdit;
    }

    /**
     * Return buttonDelete GuiButton
     */
    static GuiButton getButtonDelete(GuiMultiplayer par0GuiMultiplayer)
    {
        return par0GuiMultiplayer.buttonDelete;
    }

    /**
     * Join server by slot index (called on double click from GuiSlotServer)
     */
    static void joinServer(GuiMultiplayer par0GuiMultiplayer, int par1)
    {
        par0GuiMultiplayer.joinServer(par1);
    }

    /**
     * Get lock object for use with synchronized()
     */
    static Object getLock()
    {
        return lock;
    }

    /**
     * Return number of outstanding ThreadPollServers threads
     */
    static int getThreadsPending()
    {
        return threadsPending;
    }

    /**
     * Increment number of outstanding ThreadPollServers threads by 1
     */
    static int incrementThreadsPending()
    {
        return threadsPending++;
    }

    /**
     * Poll server for MOTD, lag, and player count/max
     */
    static void pollServer(GuiMultiplayer par0GuiMultiplayer, ServerNBTStorage par1ServerNBTStorage) throws IOException
    {
        par0GuiMultiplayer.pollServer(par1ServerNBTStorage);
    }

    /**
     * Decrement number of outstanding ThreadPollServers threads by 1
     */
    static int decrementThreadsPending()
    {
        return threadsPending--;
    }

    /**
     * Sets a GUI's lag tooltip text.
     */
    static String setTooltipText(GuiMultiplayer par0GuiMultiplayer, String par1Str)
    {
        return par0GuiMultiplayer.lagTooltip = par1Str;
    }
}
