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

@Mod(modid = "PillarTeleporter", name = "PillarTeleporter", version = "1.3.2.2")
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
            Property teleportationKeyProperty = cfg.getOrCreateIntProperty("teleportationKey", Configuration.CATEGORY_ITEM, 31373);
            teleportationKeyProperty.comment = "Teleportation key";
            teleportationKeyID = teleportationKeyProperty.getInt();
            //teleportationKeyID = cfg.getItem("teleportationKey", 2373).getInt();

            final String category_name = "teleportation_consumables";

            ///if (!cfg.hasCategory(category_name)) {
            if (cfg.categories.get(category_name) == null) {
                ItemStack[] defaultTeleportationConsumables = new ItemStack[]{
                    new ItemStack(Item.dyePowder, 1000, 4),
                    new ItemStack(Item.coal, 300, 0),
                    new ItemStack(Item.coal, 100, 1),
                    new ItemStack(Item.redstone, 500, -1)
                };
                for (int i = 0; i < defaultTeleportationConsumables.length; i++) {
                    ItemStack stack = defaultTeleportationConsumables[i];
                    Property property = cfg.getOrCreateIntProperty("" + stack.getItem().shiftedIndex + "_" + stack.getItemDamage(), category_name, stack.stackSize);
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
        //} catch (Exception e) {
            //FMLLog.log(Level.SEVERE, e, "PillarTeleporter's configuration failed to load.");
        } finally {
            cfg.save();
        }
    }

    @Init
    public void init(FMLInitializationEvent evt)
    {
        teleportationKey = new ItemTeleportationKey(teleportationKeyID).setItemName("teleportationKey").setCreativeTab(CreativeTabs.tabTools).setMaxStackSize(1).setMaxDamage(10000);
        //teleportationKey.setIconIndex(1);
        LanguageRegistry.addName(teleportationKey, "Teleportation Key");
        //teleportationKey.setIconIndex(ModLoader.addOverride("/gui/items.png", "/gui/teleportationKey.png"));
        teleportationKey.setIconIndex(RenderingRegistry.addTextureOverride("/gui/items.png", "/lt/kudlius/pillarteleporter/items.png"));
        //ModLoader.addName(teleportationKey, "Teleportation Key");

        // teleportation key creation recipe
        GameRegistry.addRecipe(new ItemStack(teleportationKey, 1, 10000), new Object[]{"O", "T", Character.valueOf('O'), Item.enderPearl, Character.valueOf('T'), Block.torchRedstoneActive});
        if (!ObfuscationReflectionHelper.obfuscation) {
            GameRegistry.addRecipe(new ItemStack(teleportationKey, 1, 10000), new Object[]{"O", "T", Character.valueOf('O'), Block.dirt, Character.valueOf('T'), Item.stick});
            //GameRegistry.addRecipe(new ItemStack(Item.porkCooked), new Object[]{" O ", "OTO", " O ", Character.valueOf('O'), Block.stone, Character.valueOf('T'), Item.goldNugget});
            // lapis lazuli from cobblestone
            GameRegistry.addRecipe(new ItemStack(Item.dyePowder, 1, 4), new Object[]{"O ", " O", Character.valueOf('O'), Block.cobblestone});
        }
    }
}
