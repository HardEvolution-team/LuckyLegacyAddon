package com.electrifiedded.luckylegacyaddon.loot;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public final class LppEvents {
    @SubscribeEvent
    public void onRightClickBlock(PlayerInteractEvent.RightClickBlock ev) {
        try {
            if (LppRegistry.seedToCrop.isEmpty()) return;
            EntityPlayer p = ev.getEntityPlayer();
            ItemStack held = ev.getItemStack();
            if (held.isEmpty()) return;
            AddedBlocks.Crop crop = LppRegistry.seedToCrop.get(held.getItem());
            if (crop == null || ev.getFace() != EnumFacing.UP) return;
            World w = ev.getWorld();
            BlockPos pos = ev.getPos();
            BlockPos above = pos.up();
            IBlockState soil = w.getBlockState(pos);
            if (!p.canPlayerEdit(above, EnumFacing.UP, held)) return;
            if (!w.isAirBlock(above)) return;
            if (!soil.getBlock().canSustainPlant(soil, w, pos, EnumFacing.UP, crop)) return;
            if (!w.isRemote) {
                w.setBlockState(above, crop.getDefaultState());
                if (!p.capabilities.isCreativeMode) held.shrink(1);
            }
            ev.setCancellationResult(EnumActionResult.SUCCESS);
            ev.setCanceled(true);
        } catch (Throwable t) {
            LootPlusPlus.LOG.warn("[Loot++] planting failed: {}", t.toString());
        }
    }
}
