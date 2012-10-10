package lt.kudlius.pillarteleporter;

import java.util.logging.Level;
import java.io.IOException;
import net.minecraft.client.Minecraft;
import net.minecraft.src.Item;
import net.minecraft.src.Block;
import net.minecraft.src.ItemStack;

import net.minecraftforge.common.Configuration;
import cpw.mods.fml.common.FMLLog;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.network.NetworkMod;
import cpw.mods.fml.common.Mod.Init;
import cpw.mods.fml.common.Mod.PreInit;
import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.common.registry.LanguageRegistry;
import cpw.mods.fml.client.registry.RenderingRegistry;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLInitializationEvent;

@Mod(modid = "PillarTeleporter", name = "PillarTeleporter", version = "1.3.2.1")
@NetworkMod(clientSideRequired = true, serverSideRequired = true)
public class PillarTeleporter
{
    public static Item teleportationKey;
    private int teleportationKeyID;

    public PillarTeleporter()
    {
    }

    @PreInit
    public void preInit(FMLPreInitializationEvent event)
    {
        // Loading in Configuration Data
        Configuration cfg = new Configuration(event.getSuggestedConfigurationFile());
        try {
            cfg.load();
            teleportationKeyID = cfg.getOrCreateIntProperty("teleportationKey", Configuration.CATEGORY_ITEM, 31373).getInt();
            //teleportationKeyID = cfg.getItem("teleportationKey", 2373).getInt();
        } catch (Exception e) {
            FMLLog.log(Level.SEVERE, e, "PillarTeleporter's configuration failed to load.");
        } finally {
            cfg.save();
        }
    }

    @Init
    public void init(FMLInitializationEvent evt)
    {
        teleportationKey = new ItemTeleportationKey(teleportationKeyID).setItemName("teleportationKey");
        //teleportationKey.setIconIndex(1);
        LanguageRegistry.addName(teleportationKey, "Teleportation Key");
        //teleportationKey.setIconIndex(ModLoader.addOverride("/gui/items.png", "/gui/teleportationKey.png"));
        teleportationKey.setIconIndex(RenderingRegistry.addTextureOverride("/gui/items.png", "/gui/teleportationKey.png"));
        //ModLoader.addName(teleportationKey, "Teleportation Key");

        // teleportation key creation recipe
        GameRegistry.addRecipe(new ItemStack(teleportationKey), new Object[]{"O", "T", Character.valueOf('O'), Item.enderPearl, Character.valueOf('T'), Block.torchRedstoneActive});
        //GameRegistry.addRecipe(new ItemStack(teleportationKey), new Object[]{"O", "T", Character.valueOf('O'), Block.dirt, Character.valueOf('T'), Item.stick});
        //GameRegistry.addRecipe(new ItemStack(Item.porkCooked), new Object[]{" O ", "OTO", " O ", Character.valueOf('O'), Block.stone, Character.valueOf('T'), Item.goldNugget});
    }
}
