package org.makershaven.signshopmysterybox.operations;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.wargamer2010.signshop.configuration.SignShopConfig;
import org.wargamer2010.signshop.operations.SignShopArguments;
import org.wargamer2010.signshop.operations.SignShopOperation;
import org.wargamer2010.signshop.util.itemUtil;
import org.wargamer2010.signshop.util.signshopUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class givePlayerItemsFromMysteryBox implements SignShopOperation {
    @Override
    public Boolean setupOperation(SignShopArguments ssArgs) {
        if (ssArgs.getContainables().isEmpty()) {
            ssArgs.getPlayer().get().sendMessage(SignShopConfig.getError("chest_missing", ssArgs.getMessageParts()));
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
        int prizeAmount = getPrizeAmount(ssArgs);
        List<ItemStack> mysteryItems = getItemsInContainables(ssArgs);
        Collections.shuffle(mysteryItems, new Random(System.currentTimeMillis()));
        ItemStack[] prizes = new ItemStack[prizeAmount];
        for (int i = 0; i < prizeAmount; i++) {
            ItemStack thisItem = mysteryItems.get(new Random(System.currentTimeMillis() + i).nextInt(mysteryItems.size()));
            thisItem.setAmount(1);
            boolean shopIsInfinite = ssArgs.isOperationParameter("infinite");

            if (shopIsInfinite) {
                prizes[i] = thisItem;
            }
            else {
                if (removeItemFromContainables(ssArgs, thisItem)) {
                    prizes[i] = thisItem;
                }
                else {
                    return false;
                }
            }

        }
        ssArgs.getPlayer().get().givePlayerItems(prizes);
        ssArgs.setMessagePart("!items",itemUtil.itemStackToString(prizes));
        return true;
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
                    if (item != null && item.getType() != Material.AIR){
                        ItemStack itemClone = item.clone();
                        int itemStackAmount = itemClone.getAmount();
                        for (int i = 1; i <= itemStackAmount ; i++) {
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
        if (Holder == null)
            return false;
        Holder.getInventory().removeItem(item);
        ssArgs.setMessagePart("!items", itemUtil.itemStackToString(ssArgs.getItems().get()));
        return true;
    }

}
