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

import java.lang.reflect.Field;
import java.lang.IllegalAccessException;

public class ItemTeleportationKey extends Item
{
    public ItemTeleportationKey(int i)
    {
        super(i);
    }

    @Override
    public boolean onItemUse(ItemStack itemstack, EntityPlayer entityplayer, World world, int i, int j, int k, int l, float par8, float par9, float par10)
    {
        //if (!world.isRemote) {
        if (world instanceof WorldClient) {
            System.out.println("current world is not remote. returning from onItemUse");
            return false;
        }
        System.out.println("current world is remote: " + world.isRemote);
        //System.out.println(new StringBuilder().append("teleportation key used! i=").append(i).append(", j=").append(j).append(", k=").append(k).append(", l=").append(l).toString());

        TileEntity tileentity = world.getBlockTileEntity(i, j, k);
        if ((tileentity != null) && (tileentity instanceof TileEntitySign)) {
            System.out.println(new StringBuilder().append("metadata: ").append(world.getBlockMetadata(i, j, k)).toString());
            TileEntitySign sign = (TileEntitySign) tileentity;
            tryToTeleport(sign, entityplayer, world, i, j, k, l);
        }
        return false;
    }

    /*
    @Override
    public ItemStack onItemRightClick(ItemStack itemstack, World world, EntityPlayer entityplayer)
    {
        System.out.println(new StringBuilder().append("teleportation key right clicked!").toString());

        System.out.println(getSaveDirectory(world).getPath());

        ItemStack[] inventory = entityplayer.inventory.mainInventory;
        for (int i = 0; i < inventory.length; i++) {
            if(inventory[i] != null) {
                if (inventory[i].itemID == Item.coal.shiftedIndex) {
                    System.out.println(new StringBuilder().append(inventory[i].stackSize).append(" coal!").toString());
                }
                //System.out.println(new StringBuilder().append("found").append(inventory[i].itemID).append(" at ").append(i).toString());
          }
          else {
              //System.out.println(new StringBuilder().append("none at ").append(i).toString());
          }
        }

        return itemstack;
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
    
    private void tryToTeleport(TileEntitySign sign, EntityPlayer entityplayer, World world, int i, int j, int k, int l)
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
        double distance = Math.sqrt(Math.pow(x - entityplayer.posX, 2) + Math.pow(y - entityplayer.posY, 2) + Math.pow(z - entityplayer.posZ, 2));
        int localStrength = teleportStrengthAt(world, i, j, k);
        int maxDistance = teleportStrengthAt(world, x, y, z) + localStrength;
        if (distance <= maxDistance) {
            entityplayer.setPositionAndUpdate(x + 0.5, y + 0.0, z + 0.5);
        }
        else {

            entityplayer.addChatMessage(new StringBuilder().append("The pillar is too weak. Strengten it by ").append((int) Math.ceil(distance - maxDistance)).toString());
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
