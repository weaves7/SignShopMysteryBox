package org.makershaven.signshopmysterybox.operations;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Container;
import org.bukkit.block.data.type.Hopper;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.makershaven.signshopmysterybox.SignShopMysteryBox;
import org.wargamer2010.signshop.SignShop;
import org.wargamer2010.signshop.operations.SignShopArguments;
import org.wargamer2010.signshop.operations.SignShopOperation;
import org.wargamer2010.signshop.util.itemUtil;
import org.wargamer2010.signshop.util.signshopUtil;

import java.util.*;
import java.util.logging.Level;

public class givePlayerItemsFromMysteryBox implements SignShopOperation {
    @Override
    public Boolean setupOperation(SignShopArguments ssArgs) {
        if (ssArgs.getContainables().isEmpty()) {
            ssArgs.getPlayer().get().sendMessage(SignShop.getInstance().getSignShopConfig().getError("chest_missing", ssArgs.getMessageParts()));
            return false;
        }

        ssArgs.setMessagePart("!amount", String.valueOf(Math.max(1, signshopUtil.getNumberFromLine(ssArgs.getSign().get(), 1).intValue())));
        return true;
    }

    @Override
    public Boolean checkRequirements(SignShopArguments ssArgs, Boolean activeCheck) {
        boolean bStockOK;
        int prizeAmount = getPrizeAmount(ssArgs);
        List<ItemStack> mysteryItems = getItemsInContainables(ssArgs);

        bStockOK = mysteryItems.size() >= prizeAmount;

        ssArgs.setMessagePart("!amount", String.valueOf(prizeAmount));

        if (!bStockOK)
            ssArgs.sendFailedRequirementsMessage("out_of_stock");
        if (!bStockOK && activeCheck)
            itemUtil.updateStockStatus(ssArgs.getSign().get(), ChatColor.DARK_RED);
        else if (activeCheck)
            itemUtil.updateStockStatus(ssArgs.getSign().get(), ChatColor.DARK_BLUE);

        return bStockOK;

    }

    @Override
    public Boolean runOperation(SignShopArguments ssArgs) {
        long start = System.currentTimeMillis();
        int prizeAmount = getPrizeAmount(ssArgs);
       // Map<Block, Boolean> connectedHoppers = lockConnectedHoppers(ssArgs);
        List<ItemStack> mysteryItems = getItemsInContainables(ssArgs);
        Collections.shuffle(mysteryItems, new Random(System.currentTimeMillis()));
        long afterShuffle = System.currentTimeMillis();
        ItemStack[] prizes = new ItemStack[prizeAmount];
        List<Integer> usedInts = new ArrayList<>();

        for (int i = 0; i < prizeAmount; i++) {
            int randomInt = randomInt(i,mysteryItems.size());
            if (usedInts.contains(randomInt)){
                i--;
                continue;
            }
            usedInts.add(randomInt);
            ItemStack thisItem = mysteryItems.get(randomInt);
            thisItem.setAmount(1);
            prizes[i] = thisItem;
        }

        long afterPrizeLoop = System.currentTimeMillis();
        if (!ssArgs.getPlayer().get().getVirtualInventory().isStockOK(prizes, false)) {
            ssArgs.sendFailedRequirementsMessage("player_overstocked");
           // unlockConnectedHoppers(connectedHoppers);
            return false;
        }
        long afterInvCheck = System.currentTimeMillis();
        if (!ssArgs.isOperationParameter("infinite")) {
            for (ItemStack item : prizes) {
                if (!removeItemFromContainables(ssArgs, item)) {
                    ssArgs.sendFailedRequirementsMessage("could_not_complete_operation");
                    SignShopMysteryBox.log("Items did not exist while attempting to remove them from the shop!", Level.WARNING);
                    SignShopMysteryBox.log("This should not happen. Please check shop at " + ssArgs.getSign().get().getLocation()
                            + " and report to the developer.", Level.WARNING);
                   // unlockConnectedHoppers(connectedHoppers);
                    return false;
                }
            }
        }
        long afterChestLoop = System.currentTimeMillis();
        ssArgs.getPlayer().get().givePlayerItems(prizes);
        ssArgs.setMessagePart("!items", itemUtil.itemStackToString(prizes));
       /* SignShopMysteryBox.debug("GPIFMB Shuffle, "+(afterShuffle-start)+"ms");
        SignShopMysteryBox.debug("GPIFMB Prize Loop, "+(afterPrizeLoop-afterShuffle)+"ms");
        SignShopMysteryBox.debug("GPIFMB Check Inv, "+(afterInvCheck-afterPrizeLoop)+"ms");
        SignShopMysteryBox.debug("GPIFMB Remove Items, "+(afterChestLoop-afterInvCheck)+"ms");*/
       // unlockConnectedHoppers(connectedHoppers);
        return true;
    }

    private int randomInt(int iteration,int size){
       return new Random(System.currentTimeMillis() + iteration).nextInt(size);
    }

    private Map<Block, Boolean> lockConnectedHoppers(SignShopArguments ssArgs) {
        Map<Block, Boolean> connectedHoppers = new HashMap<>();
        for (Block block : ssArgs.getContainables().get()) {
            if (block instanceof Container) {
                for (BlockFace face : BlockFace.values()){
                    switch (face){
                        case NORTH:
                        case WEST:
                        case EAST:
                        case SOUTH:
                        case UP:
                        case DOWN:
                        case SELF:{
                            Block relativeBlock = block.getRelative(face);
                            if (relativeBlock.getBlockData() instanceof Hopper){
                                Hopper hopper = (Hopper) relativeBlock.getBlockData();
                                connectedHoppers.put(relativeBlock,hopper.isEnabled());
                                hopper.setEnabled(false);
                            }
                        }
                           break;
                        case NORTH_EAST:
                        case NORTH_WEST:
                        case SOUTH_EAST:
                        case SOUTH_WEST:
                        case WEST_NORTH_WEST:
                        case NORTH_NORTH_WEST:
                        case NORTH_NORTH_EAST:
                        case EAST_NORTH_EAST:
                        case EAST_SOUTH_EAST:
                        case SOUTH_SOUTH_EAST:
                        case SOUTH_SOUTH_WEST:
                        case WEST_SOUTH_WEST:
                            break;
                    }
                }
            }
        }
        return connectedHoppers;
    }

    private void unlockConnectedHoppers(Map<Block, Boolean> hopperMap) {
        for (Block block : hopperMap.keySet()) {
            if (block.getBlockData() instanceof Hopper){
                Hopper hopper = (Hopper) block.getBlockData();
                hopper.setEnabled(hopperMap.get(block));
            }
        }
    }

    int getPrizeAmount(SignShopArguments ssArgs) {
        return Math.max(1, signshopUtil.getNumberFromLine(ssArgs.getSign().get(), 1).intValue());
    }

    List<ItemStack> getItemsInContainables(SignShopArguments ssArgs) {
        List<ItemStack> items = new ArrayList<>();
        for (Block block : ssArgs.getContainables().get()) {
            if (block.getState() instanceof InventoryHolder) {
                InventoryHolder container = (InventoryHolder) block.getState();
                for (ItemStack item : container.getInventory().getContents()) {
                    if (item != null && item.getType() != Material.AIR) {
                        ItemStack itemClone = item.clone();
                        int itemStackAmount = itemClone.getAmount();
                        for (int i = 1; i <= itemStackAmount; i++) {
                            items.add(itemClone);
                        }
                    }
                }
            }
        }
        return items;
    }

    boolean removeItemFromContainables(SignShopArguments ssArgs, ItemStack item) {
        ItemStack[] itemStacks = {item};
        InventoryHolder Holder = itemUtil.getFirstStockOKForContainables(ssArgs.getContainables().get(), itemStacks, true);
        if (Holder == null) {
            return false;
        }
        Holder.getInventory().removeItem(item);
        ssArgs.setMessagePart("!items", itemUtil.itemStackToString(ssArgs.getItems().get()));
        return true;
    }


}
