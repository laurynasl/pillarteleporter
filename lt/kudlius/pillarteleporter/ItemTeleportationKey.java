package lt.kudlius.pillarteleporter;

import java.io.*;
import java.util.Properties;
import net.minecraft.client.Minecraft;
import net.minecraft.src.Block;
import net.minecraft.src.World;
import net.minecraft.src.WorldClient;
import net.minecraft.src.TileEntity;
import net.minecraft.src.TileEntitySign;
import net.minecraft.src.EntityPlayer;
import net.minecraft.src.EntityPlayerMP;
import net.minecraft.src.ItemStack;
import net.minecraft.src.Item;
import net.minecraft.src.SaveHandler;
import net.minecraft.src.ISaveHandler;
import net.minecraft.src.MapStorage;
import net.minecraft.src.Packet130UpdateSign;
import net.minecraft.src.InventoryPlayer;
import net.minecraft.src.NBTTagCompound;
import net.minecraft.src.Potion;
import net.minecraft.src.PotionEffect;
import cpw.mods.fml.common.ObfuscationReflectionHelper;

import java.lang.reflect.Field;
import java.lang.IllegalAccessException;
import java.util.TreeMap;

public class ItemTeleportationKey extends Item
{
    public static TreeMap<String, Integer> chargesMap;

    public ItemTeleportationKey(int i)
    {
        super(i);
    }

    @Override
    public boolean onItemUse(ItemStack itemstack, EntityPlayer entityplayer, World world, int i, int j, int k, int l, float par8, float par9, float par10)
    {
        //if (!world.isRemote) {
        if (world instanceof WorldClient) {
            //System.out.println("current world is not remote. returning from onItemUse");
            return true;
        }
        System.out.println("current world is remote: " + world.isRemote);
        //System.out.println(new StringBuilder().append("teleportation key used! i=").append(i).append(", j=").append(j).append(", k=").append(k).append(", l=").append(l).toString());

        TileEntity tileentity = world.getBlockTileEntity(i, j, k);
        if ((tileentity != null) && (tileentity instanceof TileEntitySign)) {
            //System.out.println(new StringBuilder().append("metadata: ").append(world.getBlockMetadata(i, j, k)).toString());
            TileEntitySign sign = (TileEntitySign) tileentity;
            tryToTeleport(sign, entityplayer, itemstack, world, i, j, k, l);
        }
        return true;
    }

    @Override
    public ItemStack onItemRightClick(ItemStack itemstack, World world, EntityPlayer entityplayer)
    {
        int consumableIndex = entityplayer.inventory.currentItem + 3 * InventoryPlayer.func_70451_h();
        ItemStack consumableStack = entityplayer.inventory.mainInventory[consumableIndex];
        if (consumableStack == null) {
            if (!world.isRemote) {
                entityplayer.addChatMessage("No consumable");
            }
        }
        else {
            int charge = 0;

            String exactKey = "" + consumableStack.getItem().shiftedIndex + "_" + consumableStack.getItemDamage();
            String generalKey = "" + consumableStack.getItem().shiftedIndex + "_-1";
            if (chargesMap.containsKey(exactKey)) {
                charge = chargesMap.get(exactKey);
            }
            else if (chargesMap.containsKey(generalKey)) {
                charge = chargesMap.get(generalKey);
            }

            if (charge > 0) {
                if (increaseCharge(charge, itemstack)) {
                    if (!world.isRemote) {
                        entityplayer.addChatMessage("Consuming " + consumableStack.getItem().getItemDisplayName(consumableStack));
                    }
                    --consumableStack.stackSize;
                    if (consumableStack.stackSize <= 0) {
                        entityplayer.inventory.mainInventory[consumableIndex] = null;
                    }
                }
                else {
                    if (!world.isRemote) {
                        entityplayer.addChatMessage("teleportation key is full");
                    }
                }
            }
        }

        return itemstack;
    }

    @Override
    public String getItemDisplayName(ItemStack itemstack) {
        if (ObfuscationReflectionHelper.obfuscation) {
            return super.getItemDisplayName(itemstack);
        }
        else {
            return super.getItemDisplayName(itemstack) + " charge: " + getCharge(itemstack) + " damage: " + itemstack.getItemDamage();
        }
    }

    /*
    @Override
    public void onCreated(ItemStack itemstack, World world, EntityPlayer entityplayer) {
        setCharge(0, itemstack);
    }
    */


    private boolean decreaseCharge(int amount, ItemStack itemstack) {
        int charge = getCharge(itemstack);
        if (amount > charge) {
            return false;
        }
        else {
            setCharge(charge - amount, itemstack);
            return true;
        }
    }

    private boolean increaseCharge(int amount, ItemStack itemstack) {
        int charge = getCharge(itemstack);
        if (charge + amount <= getMaxDamage()) {
            setCharge(charge + amount, itemstack);
            return true;
        }
        else {
            return false;
        }
    }

    private int getCharge(ItemStack itemstack) {
        /*
        ensureChargeIsInCompound(itemstack);
        return itemstack.getTagCompound().getInteger("charge");
        */
        return getMaxDamage() - itemstack.getItemDamage();
    }

    private void setCharge(int amount, ItemStack itemstack) {
        /*
        ensureChargeIsInCompound(itemstack);
        itemstack.getTagCompound().setInteger("charge", amount);
        */
        itemstack.setItemDamage(getMaxDamage() - amount);
    }

    /*
    private void ensureChargeIsInCompound(ItemStack itemstack) {
        if (!itemstack.hasTagCompound()) {
            itemstack.setTagCompound(new NBTTagCompound());
        }
        NBTTagCompound tag = itemstack.getTagCompound();
        if (!tag.hasKey("charge")) {
            tag.setInteger("charge", 0);
        }
    }
    */

    private File getSaveDirectory(World world) {
        ISaveHandler saveHandler = world.getSaveHandler();
        if (saveHandler == null) {
            System.out.println("saveHandler is null");
            return null;
        }
        else {
            System.out.println("saveHandler looks ok");
        }
        Field f = SaveHandler.class.getDeclaredFields()[1];
        f.setAccessible(true);
        try {
            return (File) f.get(saveHandler);
        }
        catch (IllegalAccessException e) {
            return null;
        }
    }

    private boolean ensureLocationsAreLoaded(World world, EntityPlayer entityplayer) {
        File dir = getSaveDirectory(world);
        if (dir == null) {
            return false;
        }
        if (dir.getPath() == currentSaveDirName) {
            return true;
        }
        currentSaveDirName = dir.getPath();
        System.out.println("Save dir name: " + currentSaveDirName);

        File file = new File(dir, LOCATIONS_FILENAME);
        try {
            if(!file.exists())
            {
                file.createNewFile();
            }
            if(file.canRead())
            {
                FileInputStream fileinputstream = new FileInputStream(file);
                teleportLocations.load(fileinputstream);
                fileinputstream.close();
            }
        }
        catch(IOException e) {
            //System.out.println("IO exception while loading locations");
            entityplayer.addChatMessage("IO exception while loading locations");
            return false;
        }
        return true;
    }

    private void saveLocations(World world, TileEntitySign sign, EntityPlayer entityplayer, int distance) {
        File dir = getSaveDirectory(world);
        if (dir == null) {
            System.out.println("failed to save locations - dir not found");
        }
        else {
            System.out.println("locations saved");
        }
        File file = new File(dir, LOCATIONS_FILENAME);
        // of course file exists
        try {
            FileOutputStream fileoutputstream = new FileOutputStream(file);
            teleportLocations.store(fileoutputstream, "KudliusTeleporter locations");
            fileoutputstream.close();
        }
        catch(IOException e) {
            entityplayer.addChatMessage("IO exception while saving locations");
            return;
        }
        sign.signText[2] = new StringBuilder().append("Distance: ").append(distance).toString();
        sign.signText[3] = "Activated";

        //System.out.println("is sign editable: " + sign.isEditable());

        sign.setEditable(true);
        ((EntityPlayerMP) entityplayer).playerNetServerHandler.handleUpdateSign(new Packet130UpdateSign(sign.xCoord, sign.yCoord, sign.zCoord, sign.signText));
        sign.setEditable(false);
    }
    
    private void tryToTeleport(TileEntitySign sign, EntityPlayer entityplayer, ItemStack teleportationKeyStack, World world, int i, int j, int k, int l)
    {
      if (sign.signText[0].equals("LOCATION")) {
        if (!ensureLocationsAreLoaded(world, entityplayer)) {
          entityplayer.addChatMessage("failure loading locations");
          return;
        }

        // write new location
        String name = sign.signText[1] + "," + entityplayer.dimension;
        teleportLocations.setProperty(name, new StringBuilder().append(i).append(",").append(j).append(",").append(k).toString());
        saveLocations(world, sign, entityplayer, teleportStrengthAt(world, i, j, k));
      }
      else if (sign.signText[0].equals("TELEPORT")) {
        if (!ensureLocationsAreLoaded(world, entityplayer)) {
          entityplayer.addChatMessage("failure loading locations");
          return;
        }

        String name = sign.signText[1] + "," + entityplayer.dimension;
        if (!teleportLocations.containsKey(name)) {
          entityplayer.addChatMessage("no such location");
          return;
        }

        String[] unparsedCoordinates = teleportLocations.getProperty(name).split(",");
        if (unparsedCoordinates.length != 3)
        {
            entityplayer.addChatMessage(new StringBuilder().append("Broken save file: expected 3 coordinates, got ").append(unparsedCoordinates.length).toString());
            return;
        }
        int x, y, z;
        try
        {
            x = Integer.parseInt(unparsedCoordinates[0]);
            y = Integer.parseInt(unparsedCoordinates[1]);
            z = Integer.parseInt(unparsedCoordinates[2]);
        }
        catch (NumberFormatException e)
        {
            entityplayer.addChatMessage("Broken save file: number format exception");
            return;
        }
        int distance = (int) Math.ceil(Math.sqrt(Math.pow(x - entityplayer.posX, 2) + Math.pow(y - entityplayer.posY, 2) + Math.pow(z - entityplayer.posZ, 2)));
        int localStrength = teleportStrengthAt(world, i, j, k);
        int maxDistance = teleportStrengthAt(world, x, y, z) + localStrength;
        if (distance <= maxDistance) {
            if (decreaseCharge(distance, teleportationKeyStack)) {
                entityplayer.setPositionAndUpdate(x + 0.5, y + 0.0, z + 0.5);
                entityplayer.addPotionEffect(new PotionEffect(Potion.confusion.id, distance, 0));
            }
            else {
                entityplayer.addChatMessage("Not enough charge at teleportation key");
            }
        }
        else {

            entityplayer.addChatMessage(new StringBuilder().append("The pillar is too weak. Strengten it by ").append(distance - maxDistance).toString());
        }

      }
      else {
          entityplayer.addChatMessage("Only \"LOCATION\" and \"TELEPORT\" commands are supported (as first line of a sign)");
      }
    }

    private static int teleportStrengthAt(World world, int x, int y, int z) {
        int maxDistance = 0;
        int targetBlockId = world.getBlockId(x, y, z);
        if (targetBlockId == Block.signWall.blockID) {
            int metadata = world.getBlockMetadata(x, y, z);
            if(metadata == 2)
            {
                maxDistance = countMaxDistanceAt(world, x, y, z + 1);
            }
            if(metadata == 3)
            {
                maxDistance = countMaxDistanceAt(world, x, y, z - 1);
            }
            if(metadata == 4)
            {
                maxDistance = countMaxDistanceAt(world, x + 1, y, z);
            }
            if(metadata == 5)
            {
                maxDistance = countMaxDistanceAt(world, x - 1, y, z);
            }
        }
        else if (targetBlockId == Block.signPost.blockID) {
            maxDistance = countMaxDistanceAt(world, x, y - 1, z);
        }
        else {
            return 0;
        }
        return maxDistance;
    }

    public static int countMaxDistanceAt(World world, int i, int j, int k) {
        int count = 0;
        while(true) {
             if(isBlockSpecial(world.getBlockId(i, j + 1, k))) {
                 j += 1;
             }
             else {
                 break;
             }
        }
        while(true) {
            int blockId = world.getBlockId(i, j, k);
            int power = 0;
            if (blockId == Block.blockSteel.blockID) {
                power = 2304;
            }
            else if (blockId == Block.blockGold.blockID) {
                power = 18432;
            }
            else if (blockId == Block.blockDiamond.blockID) {
                power = 73728;
            }
            else if (blockId == Block.blockLapis.blockID) {
                power = 7776;
            }
            else if (blockId == Block.glowStone.blockID) {
                power = 1536;
            }
            else if (blockId == Block.pumpkin.blockID) {
                power = 144;
            }
            else if (blockId == Block.pumpkinLantern.blockID) {
                power = 153;
            }
            else {
                break;
            }
            j -= 1;
            count += power;
        }
        return (int)Math.sqrt(count * 16);
    }

    private static boolean isBlockSpecial(int blockId) {
        return blockId == Block.blockSteel.blockID || blockId == Block.blockGold.blockID || blockId == Block.blockDiamond.blockID || blockId == Block.blockLapis.blockID || blockId == Block.glowStone.blockID || blockId == Block.pumpkin.blockID || blockId == Block.pumpkinLantern.blockID;
    }

    private static String currentSaveDirName; // safeguard for multiple worlds
    private static Properties teleportLocations = new Properties();
    private static final String LOCATIONS_FILENAME = "PillarTeleporterLocations.cfg";
}
