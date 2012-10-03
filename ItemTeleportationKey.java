package net.minecraft.src;

import java.io.*;
import java.util.Properties;

public class ItemTeleportationKey extends Item
{
    public ItemTeleportationKey(int i)
    {
        super(i);
    }

    public boolean onItemUse(ItemStack itemstack, EntityPlayer entityplayer, World world, int i, int j, int k, int l)
    {
        System.out.println(new StringBuilder().append("teleportation key used! i=").append(i).append(", j=").append(j).append(", k=").append(k).append(", l=").append(l).toString());

        TileEntity tileentity = world.getBlockTileEntity(i, j, k);
        if ((tileentity != null) && (tileentity instanceof TileEntitySign)) {
          System.out.println(new StringBuilder().append("metadata: ").append(world.getBlockMetadata(i, j, k)).toString());
          TileEntitySign sign = (TileEntitySign) tileentity;
          for (int a = 0; a < sign.signText.length; a++)
          {
              System.out.println(sign.signText[a]);
          }
          tryToTeleport(sign, entityplayer, world, i, j, k, l);
        }
        return false;
    }

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

    public File getSaveDirectory(World world) {
      try {
        SaveHandler saveHandler = (SaveHandler) ModLoader.getPrivateValue(MapStorage.class, world.mapStorage, 0); //"saveHandler"
        File saveDirectory = (File) ModLoader.getPrivateValue(SaveHandler.class, saveHandler, 1); //"saveDirectory"
        return saveDirectory;
      }
      catch(NoSuchFieldException e) {
        System.out.println("no such field exception");
        return null;
      }
    }

    private boolean ensureLocationsAreLoaded(World world, TileEntitySign sign) {
      File dir = getSaveDirectory(world);
      if (dir.getPath() == currentSaveDirName) {
        return true;
      }
      currentSaveDirName = dir.getPath();

      File file = new File(dir, "KudliusTeleporter.teleportLocations.cfg");
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
        signFeedback(sign, "IO exception while loading locations");
        return false;
      }
      return true;
    }

    private void saveLocations(World world, TileEntitySign sign, int distance) {
      File dir = getSaveDirectory(world);
      File file = new File(dir, "KudliusTeleporter.teleportLocations.cfg");
      // of course file exists
      try {
        FileOutputStream fileoutputstream = new FileOutputStream(file);
        teleportLocations.store(fileoutputstream, "KudliusTeleporter locations");
        fileoutputstream.close();
      }
      catch(IOException e) {
        //System.out.println("IO exception while saving locations");
        signFeedback(sign, "IO exception while saving locations");
        return;
      }
      //System.out.println("locations saved successfully!");
      sign.signText[2] = new StringBuilder().append("Distance: ").append(distance).toString();
      signFeedback(sign, "Activated");
    }
    
    private void tryToTeleport(TileEntitySign sign, EntityPlayer entityplayer, World world, int i, int j, int k, int l)
    {
      if (sign.signText[0].equals("LOCATION")) {
        System.out.println("LOCATION");
        if (!ensureLocationsAreLoaded(world, sign)) {
          signFeedback(sign, "failure loading locations");
          return;
        }

        // write new location
        teleportLocations.setProperty(sign.signText[1], new StringBuilder().append(i).append(",").append(j).append(",").append(k).toString());
        saveLocations(world, sign, teleportStrengthAt(world, i, j, k));
      }
      else if (sign.signText[0].equals("TELEPORT")) {
        System.out.println("TELEPORT");
        if (!ensureLocationsAreLoaded(world, sign)) {
          signFeedback(sign, "failure loading locations");
          return;
        }

        if (!teleportLocations.containsKey(sign.signText[1])) {
          signFeedback(sign, "no such location");
          return;
        }

        String[] unparsedCoordinates = teleportLocations.getProperty(sign.signText[1]).split(",");
        if (unparsedCoordinates.length != 3)
        {
            signFeedback(sign, new StringBuilder().append("expected 3 coordinates, got ").append(unparsedCoordinates.length).toString());
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
            signFeedback(sign, "invalid number");
            return;
        }
        double distance = Math.sqrt(Math.pow(x - entityplayer.posX, 2) + Math.pow(y - entityplayer.posY, 2) + Math.pow(z - entityplayer.posZ, 2));
        System.out.println(new StringBuilder().append("distance: ").append(distance).toString());
        int localStrength = teleportStrengthAt(world, i, j, k);
        System.out.println(new StringBuilder().append("local strength: ").append(localStrength).toString());
        int maxDistance = teleportStrengthAt(world, x, y, z) + localStrength;
        System.out.println(new StringBuilder().append("max distance: ").append(maxDistance).toString());
        if (distance <= maxDistance) {
            entityplayer.setPosition(x + 0.5, y + 1.7, z + 0.5);
        }
        else {
            signFeedback(sign, new StringBuilder().append("too weak by ").append(distance - maxDistance).toString());
        }

      }
      else if (sign.signText[0].equals("TELEPORT BY")) {
        String[] unparsedCoordinates = sign.signText[1].split(",");
        if (unparsedCoordinates.length != 3)
        {
            signFeedback(sign, new StringBuilder().append("expected 3 coordinates, got ").append(unparsedCoordinates.length).toString());
            return;
        }
        int dx, dy, dz;
        try
        {
            dx = Integer.parseInt(unparsedCoordinates[0]);
            dy = Integer.parseInt(unparsedCoordinates[1]);
            dz = Integer.parseInt(unparsedCoordinates[2]);
        }
        catch (NumberFormatException e)
        {
            signFeedback(sign, "invalid number");
            return;
        }
        entityplayer.setPosition(entityplayer.posX + dx, entityplayer.posY + dy, entityplayer.posZ + dz);
        signFeedback(sign, "");
      }
    }

    private void signFeedback(TileEntitySign sign, String message) {
      sign.signText[3] = message;
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

    public static boolean isBlockSpecial(int blockId) {
        return blockId == Block.blockSteel.blockID || blockId == Block.blockGold.blockID || blockId == Block.blockDiamond.blockID || blockId == Block.blockLapis.blockID || blockId == Block.glowStone.blockID || blockId == Block.pumpkin.blockID || blockId == Block.pumpkinLantern.blockID;
    }

    private static String currentSaveDirName; // safeguard for multiple worlds
    private static Properties teleportLocations = new Properties();
}
