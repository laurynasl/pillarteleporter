package lt.kudlius.pillarteleporter;

import java.io.*;
import net.minecraft.client.Minecraft;
import net.minecraft.src.Block;
import net.minecraft.src.World;
import net.minecraft.src.WorldClient;
import net.minecraft.src.TileEntity;
import net.minecraft.src.TileEntitySign;
import net.minecraft.src.EntityPlayer;
import net.minecraft.src.EntityPlayerMP;
import net.minecraft.src.Packet130UpdateSign;
import net.minecraft.src.ItemStack;
import net.minecraft.src.Item;
import net.minecraft.src.InventoryPlayer;

import cpw.mods.fml.common.ObfuscationReflectionHelper;

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
    public String getTextureFile() {
        return "/lt/kudlius/pillarteleporter/items.png";
    }

    /* Charge up by consuming "fuel item"
     * */
    @Override
    public boolean onItemUse(ItemStack itemstack, EntityPlayer entityplayer, World world, int i, int j, int k, int l, float par8, float par9, float par10)
    {
        if (world.isRemote) {
            return true;
        }
        System.out.println("current world is remote: " + world.isRemote);

        TileEntity tileentity = world.getBlockTileEntity(i, j, k);
        if ((tileentity != null) && (tileentity instanceof TileEntitySign)) {
            TileEntitySign sign = (TileEntitySign) tileentity;
            tryToTeleport(sign, entityplayer, itemstack, world, i, j, k, l);
        }
        return true;
    }

    /* Manipulate signs. Either store location or teleport
     * */
    @Override
    public ItemStack onItemRightClick(ItemStack itemstack, World world, EntityPlayer entityplayer)
    {
        consumeFuel(itemstack, world, entityplayer, 1);
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

    /* consumes fuel directly above teleportation key. minimumCharge - minimum charge to consume
     * */
    private void consumeFuel(ItemStack itemstack, World world, EntityPlayer entityplayer, int minimumCharge) {
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
                if (minimumCharge > getMaxDamage()) {
                    minimumCharge = getMaxDamage();
                }
                int count = (minimumCharge + charge - 1) / charge;
                if (count > consumableStack.stackSize) {
                    count = consumableStack.stackSize;
                }

                if (count > 0) {
                    if (increaseCharge(charge, itemstack)) {
                        if (!world.isRemote) {
                            entityplayer.addChatMessage("Consuming " + count + " " + consumableStack.getItem().getItemDisplayName(consumableStack));
                        }
                        consumableStack.stackSize -= count;
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
                else {
                    if (!world.isRemote) {
                        entityplayer.addChatMessage("count to consume is 0 (this should not happen in a normal game)");
                    }
                }
            }
        }
    }

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
        return getMaxDamage() - itemstack.getItemDamage();
    }

    private void setCharge(int amount, ItemStack itemstack) {
        itemstack.setItemDamage(getMaxDamage() - amount);
    }

    private void tryToTeleport(TileEntitySign sign, EntityPlayer entityplayer, ItemStack teleportationKeyStack, World world, int i, int j, int k, int l)
    {
        if (sign.signText[0].equals("LOCATION")) {
            if (!TeleportationHelper.ensureLocationsAreLoaded(world)) {
                entityplayer.addChatMessage("Error loading locations");
                return;
            }

            // write new location
            String name = sign.signText[1] + "," + entityplayer.dimension;
            TeleportationHelper.teleportLocations.setProperty(name, "" + i + "," + j + "," + k);

            if (TeleportationHelper.saveLocations(world)) {
                int distance = TeleportationHelper.teleportStrengthAt(world, i, j, k);
                sign.signText[2] = "Distance: " + distance;
                sign.signText[3] = "Activated";
                sign.setEditable(true);
                ((EntityPlayerMP) entityplayer).playerNetServerHandler.handleUpdateSign(new Packet130UpdateSign(sign.xCoord, sign.yCoord, sign.zCoord, sign.signText));
                sign.setEditable(false);
            }
            else {
                entityplayer.addChatMessage("Error while saving locations");
            }
        }
        else if (sign.signText[0].equals("TELEPORT")) {
            if (!TeleportationHelper.ensureLocationsAreLoaded(world)) {
                entityplayer.addChatMessage("Error loading locations");
                return;
            }

            int[] coordinates = TeleportationHelper.getLocationCoordinates(sign.signText[1], entityplayer.dimension, entityplayer);
            if (coordinates == null) {
                return;
            }
            int x = coordinates[0];
            int y = coordinates[1];
            int z = coordinates[2];

            int distance = TeleportationHelper.calculateDistance(entityplayer, x, y, z);
            int maxDistance = 
                TeleportationHelper.teleportStrengthAt(world, i, j, k) + 
                TeleportationHelper.teleportStrengthAt(world, x, y, z);
            if (distance <= maxDistance) {
                int charge = getCharge(teleportationKeyStack);
                if (distance > charge) {
                    consumeFuel(teleportationKeyStack, world, entityplayer, distance - charge);
                }
                if (decreaseCharge(distance, teleportationKeyStack)) {
                    TeleportationHelper.teleport(entityplayer, x, y, z);
                }
                else {
                    entityplayer.addChatMessage("Not enough charge at teleportation key");
                }
            }
            else {
                entityplayer.addChatMessage("The pillar is too weak. Strengten it by " + (distance - maxDistance));
            }

        }
        else {
            entityplayer.addChatMessage("Only \"LOCATION\" and \"TELEPORT\" commands are supported (as first line of a sign)");
        }
    }
}
