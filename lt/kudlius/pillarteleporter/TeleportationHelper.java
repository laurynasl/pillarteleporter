package lt.kudlius.pillarteleporter;

import java.io.*;
import java.util.Properties;
import net.minecraft.src.World;
import net.minecraft.src.Block;
import net.minecraft.src.SaveHandler;
import net.minecraft.src.ISaveHandler;
import net.minecraft.src.MapStorage;
import net.minecraft.src.EntityPlayer;
import net.minecraft.src.Potion;
import net.minecraft.src.PotionEffect;
import java.lang.reflect.Field;

public class TeleportationHelper {

    private static String currentSaveDirName; // safeguard for multiple worlds
    public static Properties teleportLocations = new Properties();
    private static final String LOCATIONS_FILENAME = "PillarTeleporterLocations.cfg";

    public static int[] getLocationCoordinates(String location, int dimension, EntityPlayer entityplayer) {
        String name = location + "," + dimension;
        if (!teleportLocations.containsKey(name)) {
            entityplayer.addChatMessage("No such location");
            return null;
        }

        String[] unparsedCoordinates = TeleportationHelper.teleportLocations.getProperty(name).split(",");
        if (unparsedCoordinates.length != 3)
        {
            entityplayer.addChatMessage("Broken save file: expected 3 coordinates, got " + unparsedCoordinates.length);
            return null;
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
            return null;
        }
        return new int[]{x, y, z};
    }

    public static int calculateDistance(EntityPlayer entityplayer, int x, int y, int z) {
        return (int) Math.ceil(Math.sqrt(Math.pow(x - entityplayer.posX, 2) + Math.pow(y - entityplayer.posY, 2) + Math.pow(z - entityplayer.posZ, 2)));
    }

    public static void teleport(EntityPlayer entityplayer, int x, int y, int z) {
        int distance = calculateDistance(entityplayer, x, y, z);
        entityplayer.setPositionAndUpdate(x + 0.5, y + 0.0, z + 0.5);
        entityplayer.addPotionEffect(new PotionEffect(Potion.confusion.id, distance, 0));
    }

    public static boolean ensureLocationsAreLoaded(World world) {
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
            return false;
        }
        return true;
    }

    public static boolean saveLocations(World world) {
        File dir = getSaveDirectory(world);
        if (dir == null) {
            System.out.println("failed to save locations - dir not found");
            return false;
        }
        File file = new File(dir, LOCATIONS_FILENAME);
        // of course file exists
        try {
            FileOutputStream fileoutputstream = new FileOutputStream(file);
            TeleportationHelper.teleportLocations.store(fileoutputstream, "PillarTeleporter locations");
            fileoutputstream.close();
        }
        catch(IOException e) {
            return false;
        }
        System.out.println("locations saved");
        return true;
    }

    private static File getSaveDirectory(World world) {
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

    public static int teleportStrengthAt(World world, int x, int y, int z) {
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
    
}
