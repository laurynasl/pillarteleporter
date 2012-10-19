package lt.kudlius.pillarteleporter;

import net.minecraft.src.Item;
import net.minecraft.src.World;
import net.minecraft.src.ItemStack;
import net.minecraft.src.EntityPlayer;
import net.minecraft.src.NBTTagCompound;
import net.minecraft.src.TileEntity;
import net.minecraft.src.TileEntitySign;

public class ItemEscapeKey extends Item {

    public ItemEscapeKey(int i)
    {
        super(i);
    }

    @Override
    public String getTextureFile() {
        return "/lt/kudlius/pillarteleporter/items.png";
    }

    /* Bind to location
     * */
    @Override
    public boolean onItemUse(ItemStack itemstack, EntityPlayer entityplayer, World world, int i, int j, int k, int l, float par8, float par9, float par10) {
        if (world.isRemote) {
            return true;
        }
        ensureNBT(itemstack);
        NBTTagCompound tag = itemstack.getTagCompound();
        if (tag.hasKey("location")) {
            // this makes bound key work even when using on block. a side effect - cannot rebind key
            tryToTeleport(itemstack, world, entityplayer);
            return true;
        }

        if (!TeleportationHelper.ensureLocationsAreLoaded(world)) {
            entityplayer.addChatMessage("Error loading locations");
            return true;
        }

        TileEntity tileentity = world.getBlockTileEntity(i, j, k);
        if (!((tileentity != null) && (tileentity instanceof TileEntitySign))) {
            entityplayer.addChatMessage("Can only bind to LOCATION sign");
            return true;
        }

        TileEntitySign sign = (TileEntitySign) tileentity;
        if (!sign.signText[0].equals("LOCATION")) {
            entityplayer.addChatMessage("Sign's first row must be \"LOCATION\"");
            return true;
        }

        if(TeleportationHelper.getLocationCoordinates(sign.signText[1], entityplayer.dimension, entityplayer) == null) {
            return true;
        }

        tag.setString("location", sign.signText[1]);

        return true;
    }

    /* teleport and consume escape key
     * */
    @Override
    public ItemStack onItemRightClick(ItemStack itemstack, World world, EntityPlayer entityplayer) {
        tryToTeleport(itemstack, world, entityplayer);
        return itemstack;
    }

    private void tryToTeleport(ItemStack itemstack, World world, EntityPlayer entityplayer) {
        if (world.isRemote) {
            return;
        }
        if (!TeleportationHelper.ensureLocationsAreLoaded(world)) {
            entityplayer.addChatMessage("Error loading locations");
            return;
        }
        ensureNBT(itemstack);

        NBTTagCompound tag = itemstack.getTagCompound();
        if (!tag.hasKey("location")) {
            entityplayer.addChatMessage("Bind this key to LOCATION by right clicking on LOCATION sign");
            return;
        }

        String location = tag.getString("location");

        int[] coordinates = TeleportationHelper.getLocationCoordinates(location, entityplayer.dimension, entityplayer);
        if (coordinates == null) {
            entityplayer.addChatMessage("No such location at this dimension");
            return;
        }
        int x = coordinates[0];
        int y = coordinates[1];
        int z = coordinates[2];

        int distance = TeleportationHelper.calculateDistance(entityplayer, x, y, z);
        int maxDistance = TeleportationHelper.teleportStrengthAt(world, x, y, z);
        if (distance > 1000) {
            entityplayer.addChatMessage("You are too far from pillar by " + (distance - 1000) + " meters. Sorry if you will die soon!");
            return;
        }
        if (distance <= maxDistance) {
            TeleportationHelper.teleport(entityplayer, x, y, z);
            itemstack.stackSize--;
        }
        else {
            entityplayer.addChatMessage("The pillar is too weak. Strengten it by " + (distance - maxDistance));
        }
    }

    private void ensureNBT(ItemStack itemstack) {
        if (!itemstack.hasTagCompound()) {
            itemstack.setTagCompound(new NBTTagCompound());
        }
    }

    @Override
    public String getItemDisplayName(ItemStack itemstack) {
        NBTTagCompound tag = itemstack.getTagCompound();
        if (tag == null) {
            return super.getItemDisplayName(itemstack);
        }
        else {
            String location = tag.getString("location");
            return "Escape to " + location;
        }
    }
}

