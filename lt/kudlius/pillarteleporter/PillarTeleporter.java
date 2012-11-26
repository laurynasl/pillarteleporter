package lt.kudlius.pillarteleporter;

import java.util.logging.Level;
import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;

import net.minecraft.client.Minecraft;
import net.minecraft.src.Item;
import net.minecraft.src.Block;
import net.minecraft.src.ItemStack;
import net.minecraft.src.CreativeTabs;

import net.minecraftforge.common.Configuration;
import net.minecraftforge.common.Property;
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
import cpw.mods.fml.common.ObfuscationReflectionHelper;
import cpw.mods.fml.common.SidedProxy;

@Mod(modid = "PillarTeleporter", name = "PillarTeleporter", version = "1.4.5.8")
@NetworkMod(clientSideRequired = true, serverSideRequired = true)
public class PillarTeleporter
{
    public static Item teleportationKey;
    public static Item escapeKey;

    private int teleportationKeyID;
    private int escapeKeyID;

    @SidedProxy(clientSide = "lt.kudlius.pillarteleporter.PillarClient", serverSide= "lt.kudlius.pillarteleporter.PillarProxy")
    public static PillarProxy proxy;

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
            Property teleportationKeyProperty = cfg.get(Configuration.CATEGORY_ITEM, "teleportationKey", 31373);
            teleportationKeyProperty.comment = "Teleportation key";
            teleportationKeyID = teleportationKeyProperty.getInt();

            Property escapeKeyProperty = cfg.get(Configuration.CATEGORY_ITEM, "escapeKey", 31374);
            escapeKeyProperty.comment = "Escape key";
            escapeKeyID = escapeKeyProperty.getInt();

            final String category_name = "teleportation_consumables";

            if (!cfg.hasCategory(category_name)) { // BACKPORT
            //if (cfg.categories.get(category_name) == null) {
                ItemStack[] defaultTeleportationConsumables = new ItemStack[]{
                    new ItemStack(Item.dyePowder, 1000, 4),
                    new ItemStack(Item.coal, 300, 0),
                    new ItemStack(Item.coal, 100, 1),
                    new ItemStack(Item.redstone, 500, -1)
                };
                for (int i = 0; i < defaultTeleportationConsumables.length; i++) {
                    ItemStack stack = defaultTeleportationConsumables[i];
                    Property property = cfg.get(category_name, "" + stack.getItem().shiftedIndex + "_" + stack.getItemDamage(), stack.stackSize);
                }
            }

            ItemTeleportationKey.chargesMap = new TreeMap<String, Integer>();
            for(Map.Entry<String,Property> entry : cfg.categories.get(category_name).entrySet()) {
                String key = entry.getKey();
                Property property = entry.getValue();

                String[] splitKey = key.split("_");
                int shiftedIndex = Integer.parseInt(splitKey[0]);
                int damage = Integer.parseInt(splitKey[1]);
                ItemStack stack = new ItemStack(shiftedIndex, 1, damage);
                property.comment = stack.getItem().getItemDisplayName(stack);

                ItemTeleportationKey.chargesMap.put(key, property.getInt());
            }
        } finally {
            cfg.save();
        }
    }

    @Init
    public void init(FMLInitializationEvent evt)
    {
        proxy.registerRenderInformation();

        teleportationKey = new ItemTeleportationKey(teleportationKeyID).setItemName("teleportationKey").setCreativeTab(CreativeTabs.tabTools).setMaxStackSize(1).setMaxDamage(10000);
        LanguageRegistry.addName(teleportationKey, "Teleportation Key");
        teleportationKey.setIconCoord(0, 0);

        escapeKey = new ItemEscapeKey(escapeKeyID).setItemName("escapeKey").setCreativeTab(CreativeTabs.tabTools).setMaxStackSize(64);
        LanguageRegistry.addName(escapeKey, "Escape Key");
        escapeKey.setIconCoord(1, 0);
        

        // teleportation key creation recipe
        GameRegistry.addRecipe(new ItemStack(teleportationKey, 1, 10000), new Object[]{"O", "T", Character.valueOf('O'), Item.enderPearl, Character.valueOf('T'), Block.torchRedstoneActive});
        GameRegistry.addRecipe(new ItemStack(escapeKey), new Object[]{" R ", "VEV", " V ", Character.valueOf('R'), new ItemStack(Item.dyePowder, 1, 1), Character.valueOf('V'), Block.vine, Character.valueOf('E'), Item.emerald});

        // debug recipes
        if (!ObfuscationReflectionHelper.obfuscation) {
            GameRegistry.addRecipe(new ItemStack(teleportationKey, 1, 10000), new Object[]{"O", "T", Character.valueOf('O'), Block.dirt, Character.valueOf('T'), Item.stick});
            // lapis lazuli from cobblestone
            GameRegistry.addRecipe(new ItemStack(Item.dyePowder, 1, 4), new Object[]{"O ", " O", Character.valueOf('O'), Block.cobblestone});

            GameRegistry.addRecipe(new ItemStack(escapeKey), new Object[]{"O", "T", Character.valueOf('O'), Block.cobblestone, Character.valueOf('T'), Item.stick});
        }
    }
}
