package com.electrifiedded.luckylegacyaddon.loot;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
final class LppCreativeTab extends CreativeTabs {
    static final LppCreativeTab TAB = new LppCreativeTab();

    private LppCreativeTab() {
        super(LootPlusPlus.MODID);
    }

    @Override
    public ItemStack createIcon() {
        for (Item item : LppRegistry.items.values()) return new ItemStack(item);
        return new ItemStack(net.minecraft.init.Items.DIAMOND);
    }

    @Override
    public void displayAllRelevantItems(NonNullList<ItemStack> items) {
        for (Item item : LppRegistry.items.values()) {
            try {
                if (item.getHasSubtypes()) {
                    item.getSubItems(this, items);
                } else {
                    items.add(new ItemStack(item));
                }
            } catch (Throwable t) {
                LootPlusPlus.LOG.warn("[Loot++] Could not list {} in the creative tab: {}", item.getRegistryName(), t.toString());
            }
        }
    }
}
