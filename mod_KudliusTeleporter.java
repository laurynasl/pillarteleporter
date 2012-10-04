package net.minecraft.src;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;
import net.minecraft.client.Minecraft;

public class mod_KudliusTeleporter extends BaseMod
{
    
    public mod_KudliusTeleporter()
    {
    }

    @Override
    public String getVersion()
    {
        return "1.3.2";
    }

    @Override
    public void load()
    {
        try {
            System.out.println("I am loaded!");
            loadConfig();

            // id for teleportation key
            int itemTeleportationKeyID = generateOrLoadItemID("teleportation_key");
            teleportationKey = new ItemTeleportationKey(itemTeleportationKeyID).setItemName("teleportationKey");
            teleportationKey.iconIndex = ModLoader.addOverride("/gui/items.png", "/gui/teleportationKey.png");
            ModLoader.addName(teleportationKey, "Teleportation Key");

            // teleportation key creation recipe
            ModLoader.addRecipe(new ItemStack(teleportationKey), new Object[]{"O", "T", Character.valueOf('O'), Item.enderPearl, Character.valueOf('T'), Block.torchRedstoneActive});
            ModLoader.addRecipe(new ItemStack(teleportationKey), new Object[]{"O", "T", Character.valueOf('O'), Block.sapling, Character.valueOf('T'), Item.stick});

            saveConfig();
        }
        catch (IOException e) {
            System.out.println("IO exception...");
        }
    }

    private int generateOrLoadItemID(String name)
    {
        String key_name = "item_" + name;
        if (props.containsKey(key_name)) {
            try {
                int candidate = Integer.parseInt(props.getProperty(key_name));
                if (candidate >= 2256 && candidate < 32000) 
                {
                    if (Item.itemsList[candidate] == null)
                    {
                        return candidate - 256;
                    }
                }
            }
            catch (NumberFormatException e) {
                System.out.println("number format exception while loading value of " + name);
            }
        }
        for (int i=2256; i<32000; i++) {
            if (Item.itemsList[i] == null)
            {
                props.setProperty(key_name, Integer.toString(i));
                return i - 256;
            }
        }
        return -1; // extremely unlikely
    }

    // copied from ModLoader

    private static void loadConfig()
        throws IOException
    {
        cfgdir.mkdir();
        if(!cfgfile.exists() && !cfgfile.createNewFile())
        {
            return;
        }
        if(cfgfile.canRead())
        {
            FileInputStream fileinputstream = new FileInputStream(cfgfile);
            props.load(fileinputstream);
            fileinputstream.close();
        }
    }

    private static void saveConfig()
        throws IOException
    {
        cfgdir.mkdir();
        if(!cfgfile.exists() && !cfgfile.createNewFile())
        {
            return;
        }
        if(cfgfile.canWrite())
        {
            FileOutputStream fileoutputstream = new FileOutputStream(cfgfile);
            props.store(fileoutputstream, "KudliusTeleporter Config");
            fileoutputstream.close();
        }
    }

    public static Item teleportationKey;

    public static final Properties props = new Properties();
    private static final File cfgdir;
    private static final File cfgfile;
    static 
    {
        cfgdir = new File(Minecraft.getMinecraftDir(), "/config/");
        cfgfile = new File(cfgdir, "KudliusTeleporter.cfg");
    }
}
