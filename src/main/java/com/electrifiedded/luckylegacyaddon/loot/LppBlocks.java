package com.electrifiedded.luckylegacyaddon.loot;

import net.minecraft.block.Block;
import net.minecraft.block.BlockPressurePlate;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;

final class LppBlocks {
    private LppBlocks() {}

    private static final class Base {
        final Block block;
        final IBlockState state;
        final Material material;
        final float hardness, resistance, light, slip;
        final SoundType sound;
        final String tool;
        final int harvest, opacity;

        Base(Block b, int meta) {
            block = b;
            IBlockState st;
            try {
                st = b.getStateFromMeta(Math.max(0, Math.min(15, meta)));
            } catch (Throwable t) {
                st = b.getDefaultState();
            }
            state = st;
            material = st.getMaterial();
            float h = 1.5F;
            try {
                h = st.getBlockHardness(null, BlockPos.ORIGIN);
            } catch (Throwable ignored) {
            }
            hardness = h;
            float r = 5.0F;
            try {
                r = b.getExplosionResistance(null) * 5.0F / 3.0F;
            } catch (Throwable ignored) {
            }
            resistance = r;
            sound = b.getSoundType();
            light = b.getLightValue(st) / 15.0F;
            tool = b.getHarvestTool(st);
            harvest = b.getHarvestLevel(st);
            slip = b.slipperiness;
            opacity = st.isOpaqueCube() ? -1 : b.getLightOpacity(st);
        }

        MapColor mapColor() {
            try {
                return state.getMapColor(null, BlockPos.ORIGIN);
            } catch (Throwable t) {
                return MapColor.STONE;
            }
        }
    }

    private static Base base(String name, String meta) {
        Block b = LppUtil.block(name);
        if (b == null) throw LppUtil.skip("unknown base block '" + name + "'");
        return new Base(b, LppUtil.i(meta));
    }

    private static <T extends Block & AddedBlocks.Added> T common(T b, float hardness, float resistance, SoundType sound, float light, String name) {
        b.setHardness(hardness);
        b.setResistance(resistance);
        b.lppSound(sound);
        b.setLightLevel(MathHelper.clamp(light, 0.0F, 1.0F));
        b.setTranslationKey(name);
        return b;
    }

    private static void commonB(Block b, float hardness, float resistance, SoundType sound, float light, String name) {
        b.setHardness(hardness);
        b.setResistance(resistance);
        ((AddedBlocks.Added) b).lppSound(sound);
        b.setLightLevel(MathHelper.clamp(light, 0.0F, 1.0F));
        b.setTranslationKey(name);
    }

    private static void fire(Block b, int spread, int flammability) {
        if (spread > 0 || flammability > 0) Blocks.FIRE.setFireInfo(b, spread, flammability);
    }

    static void create() {
        LppUtil.each("block_additions/generic", (e, p) -> {
            LppUtil.need(e, p, 14);
            Material m = LppUtil.material(p[2]);
            if (m == null) throw LppUtil.skip("unknown material '" + p[2] + "'");
            boolean falls = LppUtil.b(p[3]);
            float slip = MathHelper.clamp(LppUtil.f(p[10]), 0.2F, 0.98F);
            AddedBlocks.Props props = new AddedBlocks.Props(LppUtil.name(p[1]), LppUtil.i(p[13]), LppUtil.b(p[4]), p[7].trim());
            if (!LppRegistry.claim(e, p[0], "block")) return;
            Block b = falls ? new AddedBlocks.Falling(m, props, LppUtil.i(p[8]), slip) : new AddedBlocks.Generic(m, props, LppUtil.i(p[8]), slip);
            commonB(b, LppUtil.f(p[5]), LppUtil.f(p[6]), LppUtil.sounds(p[2]), LppUtil.f(p[9]), p[0]);
            fire(b, LppUtil.i(p[11]), LppUtil.i(p[12]));
            LppRegistry.addBlock(p[0], b);
        });

        LppUtil.each("block_additions/multiple_metadata", (e, p) -> {
            throw LppUtil.skip("multiple_metadata blocks are not supported yet");
        });

        LppUtil.each("block_additions/logs_pillars", (e, p) -> {
            LppUtil.need(e, p, 13);
            Material m = LppUtil.material(p[2]);
            if (m == null) throw LppUtil.skip("unknown material '" + p[2] + "'");
            float slip = MathHelper.clamp(LppUtil.f(p[9]), 0.2F, 0.98F);
            AddedBlocks.Props props = new AddedBlocks.Props(LppUtil.name(p[1]), LppUtil.i(p[12]), LppUtil.b(p[3]), p[6].trim());
            if (!LppRegistry.claim(e, p[0], "block")) return;
            Block b = new AddedBlocks.Pillar(m, props, LppUtil.i(p[7]), slip);
            commonB(b, LppUtil.f(p[4]), LppUtil.f(p[5]), LppUtil.sounds(p[2]), LppUtil.f(p[8]), p[0]);
            fire(b, LppUtil.i(p[10]), LppUtil.i(p[11]));
            LppRegistry.addBlock(p[0], b);
        });

        LppUtil.each("block_additions/plants", (e, p) -> {
            LppUtil.need(e, p, 10);
            Material m = LppUtil.material(p[2]);
            if (m == null) throw LppUtil.skip("unknown material '" + p[2] + "'");
            AddedBlocks.Props props = new AddedBlocks.Props(LppUtil.name(p[1]), 0, false, p[5].trim());
            if (!LppRegistry.claim(e, p[0], "block")) return;
            Block b = new AddedBlocks.Plant(m, props, LppUtil.i(p[6]));
            commonB(b, LppUtil.f(p[3]), LppUtil.f(p[4]), LppUtil.sounds("plants"), LppUtil.f(p[7]), p[0]);
            fire(b, LppUtil.i(p[8]), LppUtil.i(p[9]));
            LppRegistry.addBlock(p[0], b);
        });

        LppUtil.each("block_additions/slabs", (e, p) -> {
            LppUtil.need(e, p, 4);
            Base base = base(p[2], p[3]);
            if (!LppRegistry.claim(e, p[0], "block") || !LppRegistry.claim(e, p[0] + "_double", "block")) return;
            String display = LppUtil.name(p[1]);
            String tool = base.tool;
            AddedBlocks.HalfSlab half = new AddedBlocks.HalfSlab(base.material, display, tool, base.harvest, base.slip);
            AddedBlocks.FullSlab full = new AddedBlocks.FullSlab(base.material, display, tool, base.harvest, base.slip);
            half.full = full;
            full.half = half;
            common(half, base.hardness, base.resistance, base.sound, base.light, p[0]);
            common(full, base.hardness, base.resistance, base.sound, base.light, p[0] + "_double");
            LppRegistry.addBlock(p[0], half, new AddedBlocks.SlabItem(half, full));
            LppRegistry.addBlock(p[0] + "_double", full, null);
        });

        LppUtil.each("block_additions/stairs", (e, p) -> {
            LppUtil.need(e, p, 4);
            Base base = base(p[2], p[3]);
            if (!LppRegistry.claim(e, p[0], "block")) return;
            Block b = new AddedBlocks.Stairs(base.state, LppUtil.name(p[1]));
            b.setTranslationKey(p[0]);
            LppRegistry.addBlock(p[0], b);
        });

        LppUtil.each("block_additions/tiles_carpets", (e, p) -> {
            LppUtil.need(e, p, 4);
            Base base = base(p[2], p[3]);
            if (!LppRegistry.claim(e, p[0], "block")) return;
            AddedBlocks.Props props = new AddedBlocks.Props(LppUtil.name(p[1]), base.opacity < 0 ? 0 : base.opacity, false, base.tool);
            Block b = new AddedBlocks.Tile(base.material, props, base.harvest, base.slip);
            commonB(b, base.hardness, base.resistance, base.sound, base.light, p[0]);
            LppRegistry.addBlock(p[0], b);
        });

        LppUtil.each("block_additions/panes_bars", (e, p) -> {
            LppUtil.need(e, p, 4);
            Base base = base(p[2], p[3]);
            if (!LppRegistry.claim(e, p[0], "block")) return;
            Block b = common(new AddedBlocks.Pane(base.material, LppUtil.name(p[1])), base.hardness, base.resistance, base.sound, base.light, p[0]);
            LppRegistry.addBlock(p[0], b);
        });

        LppUtil.each("block_additions/walls", (e, p) -> {
            LppUtil.need(e, p, 4);
            Base base = base(p[2], p[3]);
            if (!LppRegistry.claim(e, p[0], "block")) return;
            Block b = common(new AddedBlocks.Wall(base.block, LppUtil.name(p[1])), base.hardness, base.resistance, base.sound, base.light, p[0]);
            LppRegistry.addBlock(p[0], b);
        });

        LppUtil.each("block_additions/fences", (e, p) -> {
            LppUtil.need(e, p, 4);
            Base base = base(p[2], p[3]);
            if (!LppRegistry.claim(e, p[0], "block")) return;
            Block b = common(new AddedBlocks.Fence(base.material, base.mapColor(), LppUtil.name(p[1])), base.hardness, base.resistance,
                    base.sound, base.light, p[0]);
            LppRegistry.addBlock(p[0], b);
        });

        LppUtil.each("block_additions/buttons", (e, p) -> {
            LppUtil.need(e, p, 6);
            Base base = base(p[2], p[3]);
            if (!LppRegistry.claim(e, p[0], "block")) return;
            Block b = common(new AddedBlocks.Button(LppUtil.b(p[5]), LppUtil.i(p[4]), LppUtil.name(p[1])), base.hardness, base.resistance,
                    base.sound, base.light, p[0]);
            LppRegistry.addBlock(p[0], b);
        });

        LppUtil.each("block_additions/pressure_plates", (e, p) -> {
            LppUtil.need(e, p, 7);
            Base base = base(p[2], p[3]);
            if (!LppRegistry.claim(e, p[0], "block")) return;
            BlockPressurePlate.Sensitivity s = p[6].trim().equalsIgnoreCase("mobs") ? BlockPressurePlate.Sensitivity.MOBS
                    : BlockPressurePlate.Sensitivity.EVERYTHING;
            Block b = common(new AddedBlocks.Plate(base.material, s, LppUtil.name(p[1])), base.hardness, base.resistance, base.sound, base.light, p[0]);
            LppRegistry.addBlock(p[0], b);
        });

        LppUtil.each("block_additions/doors", (e, p) -> {
            LppUtil.need(e, p, 5);
            Base base = base(p[2], p[3]);
            if (!LppRegistry.claim(e, p[0], "block")) return;
            AddedBlocks.Door d = common(new AddedBlocks.Door(base.material, LppUtil.b(p[4]), LppUtil.name(p[1])), base.hardness, base.resistance,
                    base.sound, base.light, p[0]);
            AddedBlocks.DoorItem item = new AddedBlocks.DoorItem(d);
            d.doorItem = item;
            LppRegistry.addBlock(p[0], d, null);
            LppRegistry.addItem(p[0], item);
        });

        LppUtil.each("block_additions/fence_gates", (e, p) -> {
            LppUtil.need(e, p, 5);
            Base base = base(p[2], p[3]);
            if (!LppRegistry.claim(e, p[0], "block")) return;
            Block b = common(new AddedBlocks.Gate(LppUtil.name(p[1])), base.hardness, base.resistance, base.sound, base.light, p[0]);
            LppRegistry.addBlock(p[0], b);
        });

        LppUtil.each("block_additions/ladders", (e, p) -> {
            LppUtil.need(e, p, 10);
            if (!LppRegistry.claim(e, p[0], "block")) return;
            float slip = LppUtil.f(p[9]);
            Block b = common(new AddedBlocks.Ladder(p[4].trim(), LppUtil.i(p[5]), slip, LppUtil.name(p[1])), LppUtil.f(p[2]), LppUtil.f(p[3]),
                    SoundType.LADDER, LppUtil.f(p[6]), p[0]);
            fire(b, LppUtil.i(p[7]), LppUtil.i(p[8]));
            LppRegistry.addBlock(p[0], b);
        });

        LppUtil.each("block_additions/furnaces", (e, p) -> {
            LppUtil.need(e, p, 11);
            Material m = LppUtil.material(p[2]);
            if (m == null) throw LppUtil.skip("unknown material '" + p[2] + "'");
            AddedBlocks.Props props = new AddedBlocks.Props(LppUtil.name(p[1]), LppUtil.i(p[10]), false, p[5].trim());
            if (!LppRegistry.claim(e, p[0], "block")) return;
            Block b = new AddedBlocks.Furnace(m, props, LppUtil.i(p[6]), MathHelper.clamp(LppUtil.f(p[8]), 0.2F, 0.98F), LppUtil.f(p[9]));
            commonB(b, LppUtil.f(p[3]), LppUtil.f(p[4]), LppUtil.sounds(p[2]), LppUtil.f(p[7]), p[0]);
            LppRegistry.addBlock(p[0], b);
        });

        LppUtil.each("block_additions/crafting_tables", (e, p) -> {
            LppUtil.need(e, p, 12);
            Material m = LppUtil.material(p[2]);
            if (m == null) throw LppUtil.skip("unknown material '" + p[2] + "'");
            AddedBlocks.Props props = new AddedBlocks.Props(LppUtil.name(p[1]), LppUtil.i(p[11]), false, p[5].trim());
            if (!LppRegistry.claim(e, p[0], "block")) return;
            Block b = new AddedBlocks.Workbench(m, props, LppUtil.i(p[6]), MathHelper.clamp(LppUtil.f(p[8]), 0.2F, 0.98F));
            commonB(b, LppUtil.f(p[3]), LppUtil.f(p[4]), LppUtil.sounds(p[2]), LppUtil.f(p[7]), p[0]);
            fire(b, LppUtil.i(p[9]), LppUtil.i(p[10]));
            LppRegistry.addBlock(p[0], b);
        });

        LppUtil.each("block_additions/cakes", (e, p) -> {
            LppUtil.needMin(e, p, 12);
            if (!LppRegistry.claim(e, p[0], "block")) return;
            int slices = Math.max(1, Math.min(16, LppUtil.i(p[8])));
            AddedBlocks.Cake.nextBites = slices;
            AddedBlocks.Cake cake = new AddedBlocks.Cake(MathHelper.clamp(LppUtil.f(p[5]), 0.2F, 0.98F), slices, LppUtil.i(p[9]),
                    LppUtil.f(p[10]), LppUtil.b(p[11]), LppUtil.name(p[1]));
            common(cake, LppUtil.f(p[2]), LppUtil.f(p[3]), SoundType.CLOTH, LppUtil.f(p[4]), p[0]);
            fire(cake, LppUtil.i(p[6]), LppUtil.i(p[7]));
            cake.effects.addAll(LppEffects.parseAll(e, p, 12));
            LppRegistry.addBlock(p[0], cake);
        });

        LppUtil.each("block_additions/crops", (e, p) -> {
            LppUtil.needMin(e, p, 10);
            Item seed = LppUtil.item(p[2]);
            if (seed == null) {
                if (LppRegistry.items.containsKey(LppUtil.regName(p[2].substring(p[2].indexOf(':') + 1)))) seed = LppRegistry.items.get(LppUtil.regName(p[2].substring(p[2].indexOf(':') + 1)));
            }
            if (!LppRegistry.claim(e, p[0], "block")) return;
            AddedBlocks.Crop crop = new AddedBlocks.Crop(LppUtil.b(p[7]), LppUtil.name(p[1]));
            crop.lppSound(LppUtil.sounds("plants"));
            crop.setLightLevel(MathHelper.clamp(LppUtil.f(p[4]), 0.0F, 1.0F));
            crop.setTranslationKey(p[0]);
            fire(crop, LppUtil.i(p[5]), LppUtil.i(p[6]));
            LppRegistry.addBlock(p[0], crop, null);
            LppRegistry.pendingCrops.add(new Object[]{crop, p[2], LppUtil.i(p[3]), LppUtil.b(p[8]), LppUtil.b(p[9]), e});
        });
    }
}
