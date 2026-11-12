package com.electrifiedded.luckylegacyaddon.loot;

import net.minecraft.block.Block;
import net.minecraft.block.BlockButton;
import net.minecraft.block.BlockCrops;
import net.minecraft.block.BlockDoor;
import net.minecraft.block.BlockFalling;
import net.minecraft.block.BlockFence;
import net.minecraft.block.BlockFenceGate;
import net.minecraft.block.BlockHorizontal;
import net.minecraft.block.BlockLadder;
import net.minecraft.block.BlockPane;
import net.minecraft.block.BlockPlanks;
import net.minecraft.block.BlockPressurePlate;
import net.minecraft.block.BlockRotatedPillar;
import net.minecraft.block.BlockSlab;
import net.minecraft.block.BlockStairs;
import net.minecraft.block.BlockWall;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.init.SoundEvents;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.ContainerWorkbench;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryHelper;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemDoor;
import net.minecraft.item.ItemSlab;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.FurnaceRecipes;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityFurnace;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.IStringSerializable;
import net.minecraft.util.Mirror;
import net.minecraft.util.NonNullList;
import net.minecraft.util.Rotation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.IInteractionObject;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class AddedBlocks {
    private AddedBlocks() {}

    public interface Added {
        String displayName();

        void lppSound(SoundType s);
    }

    private static String disp(String raw) {
        return LppUtil.display(raw);
    }

    public static final class Props {
        public final String display;
        public final int opacity;
        public final boolean beacon;
        public final String tool;
        public final boolean opaque;

        public Props(String display, int opacity, boolean beacon, String tool) {
            this.display = display;
            this.opacity = opacity;
            this.beacon = beacon;
            this.tool = tool == null || tool.equalsIgnoreCase("none") ? null : tool;
            this.opaque = opacity < 0;
        }

        void apply(Block b, int harvest, float slip) {
            if (harvest != -1 && tool != null) b.setHarvestLevel(tool, harvest);
            b.setLightOpacity(opaque ? 255 : opacity);
            b.slipperiness = slip;
        }
    }


    public static class Generic extends Block implements Added {
        final Props p;

        public Generic(Material m, Props p, int harvest, float slip) {
            super(m);
            this.p = p;
            p.apply(this, harvest, slip);
        }

        @Override public String displayName() { return p.display; }
        @Override public void lppSound(SoundType s) { setSoundType(s); }
        @Override public String getLocalizedName() { return disp(p.display); }
        @Override public boolean isOpaqueCube(IBlockState s) { return p == null || p.opaque; }
        @Override public boolean isFullCube(IBlockState s) { return p == null || p.opaque; }
        @Override @SideOnly(Side.CLIENT) public BlockRenderLayer getRenderLayer() { return p == null || p.opaque ? BlockRenderLayer.SOLID : BlockRenderLayer.TRANSLUCENT; }
        @Override public boolean isBeaconBase(IBlockAccess w, BlockPos pos, BlockPos beacon) { return p != null && p.beacon; }
        @Override public boolean isToolEffective(String type, IBlockState s) { return p != null && type.equals(p.tool); }
        @Nullable @Override public String getHarvestTool(IBlockState s) { return p == null ? null : p.tool; }

        @Override
        @SideOnly(Side.CLIENT)
        public boolean shouldSideBeRendered(IBlockState s, IBlockAccess w, BlockPos pos, EnumFacing side) {
            if (p != null && !p.opaque) return w.getBlockState(pos.offset(side)).getBlock() != this;
            return super.shouldSideBeRendered(s, w, pos, side);
        }
    }

    public static class Falling extends BlockFalling implements Added {
        final Props p;

        public Falling(Material m, Props p, int harvest, float slip) {
            super(m);
            this.p = p;
            p.apply(this, harvest, slip);
        }

        @Override public String displayName() { return p.display; }
        @Override public void lppSound(SoundType s) { setSoundType(s); }
        @Override public String getLocalizedName() { return disp(p.display); }
        @Override public boolean isOpaqueCube(IBlockState s) { return p == null || p.opaque; }
        @Override public boolean isFullCube(IBlockState s) { return p == null || p.opaque; }
        @Override @SideOnly(Side.CLIENT) public BlockRenderLayer getRenderLayer() { return p == null || p.opaque ? BlockRenderLayer.SOLID : BlockRenderLayer.TRANSLUCENT; }
        @Override public boolean isBeaconBase(IBlockAccess w, BlockPos pos, BlockPos beacon) { return p != null && p.beacon; }
        @Override public boolean isToolEffective(String type, IBlockState s) { return p != null && type.equals(p.tool); }
        @Nullable @Override public String getHarvestTool(IBlockState s) { return p == null ? null : p.tool; }

        @Override
        @SideOnly(Side.CLIENT)
        public boolean shouldSideBeRendered(IBlockState s, IBlockAccess w, BlockPos pos, EnumFacing side) {
            if (p != null && !p.opaque) return w.getBlockState(pos.offset(side)).getBlock() != this;
            return super.shouldSideBeRendered(s, w, pos, side);
        }
    }

    public static class Pillar extends BlockRotatedPillar implements Added {
        final Props p;

        public Pillar(Material m, Props p, int harvest, float slip) {
            super(m);
            this.p = p;
            p.apply(this, harvest, slip);
        }

        @Override public String displayName() { return p.display; }
        @Override public void lppSound(SoundType s) { setSoundType(s); }
        @Override public String getLocalizedName() { return disp(p.display); }
        @Override public boolean isOpaqueCube(IBlockState s) { return p == null || p.opaque; }
        @Override public boolean isFullCube(IBlockState s) { return p == null || p.opaque; }
        @Override @SideOnly(Side.CLIENT) public BlockRenderLayer getRenderLayer() { return p == null || p.opaque ? BlockRenderLayer.SOLID : BlockRenderLayer.TRANSLUCENT; }
        @Override public boolean isBeaconBase(IBlockAccess w, BlockPos pos, BlockPos beacon) { return p != null && p.beacon; }
        @Override public boolean isToolEffective(String type, IBlockState s) { return p != null && type.equals(p.tool); }
        @Nullable @Override public String getHarvestTool(IBlockState s) { return p == null ? null : p.tool; }
    }


    public static class Plant extends Generic {
        public Plant(Material m, Props p, int harvest) {
            super(m, p, harvest, 0.6F);
        }

        private static final AxisAlignedBB BOX = new AxisAlignedBB(0.3, 0, 0.3, 0.7, 0.7, 0.7);

        @Override public AxisAlignedBB getBoundingBox(IBlockState s, IBlockAccess w, BlockPos pos) { return BOX; }
        @Nullable @Override public AxisAlignedBB getCollisionBoundingBox(IBlockState s, IBlockAccess w, BlockPos pos) { return NULL_AABB; }
        @Override public boolean isOpaqueCube(IBlockState s) { return false; }
        @Override public boolean isFullCube(IBlockState s) { return false; }
        @Override @SideOnly(Side.CLIENT) public BlockRenderLayer getRenderLayer() { return BlockRenderLayer.CUTOUT; }

        @Override
        public boolean canPlaceBlockAt(World w, BlockPos pos) {
            return super.canPlaceBlockAt(w, pos) && canStay(w, pos);
        }

        boolean canStay(World w, BlockPos pos) {
            IBlockState below = w.getBlockState(pos.down());
            return getDefaultState().getMaterial() == Material.PLANTS
                    ? below.getBlock().canSustainPlant(below, w, pos.down(), EnumFacing.UP, (net.minecraftforge.common.IPlantable) Blocks.TALLGRASS)
                    : below.isSideSolid(w, pos.down(), EnumFacing.UP);
        }

        @Override
        public void neighborChanged(IBlockState s, World w, BlockPos pos, Block b, BlockPos from) {
            if (!canStay(w, pos)) {
                dropBlockAsItem(w, pos, s, 0);
                w.setBlockToAir(pos);
            }
        }
    }

    public static class Tile extends Generic {
        private static final AxisAlignedBB BOX = new AxisAlignedBB(0, 0, 0, 1, 0.0625, 1);

        public Tile(Material m, Props p, int harvest, float slip) {
            super(m, p, harvest, slip);
            setLightOpacity(0);
        }

        @Override public AxisAlignedBB getBoundingBox(IBlockState s, IBlockAccess w, BlockPos pos) { return BOX; }
        @Override public boolean isOpaqueCube(IBlockState s) { return false; }
        @Override public boolean isFullCube(IBlockState s) { return false; }
        @Override @SideOnly(Side.CLIENT) public BlockRenderLayer getRenderLayer() { return p == null || p.opaque ? BlockRenderLayer.SOLID : BlockRenderLayer.TRANSLUCENT; }

        @Override
        public boolean canPlaceBlockAt(World w, BlockPos pos) {
            return super.canPlaceBlockAt(w, pos) && !w.isAirBlock(pos.down());
        }

        @Override
        public void neighborChanged(IBlockState s, World w, BlockPos pos, Block b, BlockPos from) {
            if (w.isAirBlock(pos.down())) {
                dropBlockAsItem(w, pos, s, 0);
                w.setBlockToAir(pos);
            }
        }

        @Override
        @SideOnly(Side.CLIENT)
        public boolean shouldSideBeRendered(IBlockState s, IBlockAccess w, BlockPos pos, EnumFacing side) {
            return side == EnumFacing.UP || super.shouldSideBeRendered(s, w, pos, side);
        }
    }


    public enum SlabVariant implements IStringSerializable {
        ALL;

        @Override public String getName() { return "all"; }
        @Override public String toString() { return getName(); }
    }

    public static final PropertyEnum<SlabVariant> SLAB_VARIANT = PropertyEnum.create("variant", SlabVariant.class);

    public abstract static class SlabBase extends BlockSlab implements Added {
        final String display;
        @Nullable final String tool;

        SlabBase(Material m, String display, @Nullable String tool, int harvest, float slip) {
            super(m);
            this.display = display;
            this.tool = tool;
            if (tool != null && harvest >= 0) setHarvestLevel(tool, harvest);
            slipperiness = slip;
            useNeighborBrightness = true;
        }

        @Override public String displayName() { return display; }
        @Override public void lppSound(SoundType s) { setSoundType(s); }
        @Override public String getLocalizedName() { return disp(display); }
        @Override public String getTranslationKey(int meta) { return getTranslationKey(); }
        @Override public IProperty<?> getVariantProperty() { return SLAB_VARIANT; }
        @Override public Comparable<?> getTypeForItem(ItemStack s) { return SlabVariant.ALL; }
        @Override public boolean isToolEffective(String type, IBlockState s) { return type.equals(tool); }
        @Nullable @Override public String getHarvestTool(IBlockState s) { return tool; }
        @Override public int damageDropped(IBlockState s) { return 0; }
    }

    public static class HalfSlab extends SlabBase {
        public Block full;

        public HalfSlab(Material m, String display, @Nullable String tool, int harvest, float slip) {
            super(m, display, tool, harvest, slip);
            setDefaultState(blockState.getBaseState().withProperty(HALF, EnumBlockHalf.BOTTOM).withProperty(SLAB_VARIANT, SlabVariant.ALL));
        }

        @Override public boolean isDouble() { return false; }
        @Override protected BlockStateContainer createBlockState() { return new BlockStateContainer(this, HALF, SLAB_VARIANT); }
        @Override public IBlockState getStateFromMeta(int meta) {
            return getDefaultState().withProperty(HALF, (meta & 8) == 0 ? EnumBlockHalf.BOTTOM : EnumBlockHalf.TOP);
        }
        @Override public int getMetaFromState(IBlockState s) { return s.getValue(HALF) == EnumBlockHalf.TOP ? 8 : 0; }
    }

    public static class FullSlab extends SlabBase {
        public Block half;

        public FullSlab(Material m, String display, @Nullable String tool, int harvest, float slip) {
            super(m, display, tool, harvest, slip);
            setDefaultState(blockState.getBaseState().withProperty(SLAB_VARIANT, SlabVariant.ALL));
        }

        @Override public boolean isDouble() { return true; }
        @Override protected BlockStateContainer createBlockState() { return new BlockStateContainer(this, SLAB_VARIANT); }
        @Override public IBlockState getStateFromMeta(int meta) { return getDefaultState(); }
        @Override public int getMetaFromState(IBlockState s) { return 0; }
        @Override public Item getItemDropped(IBlockState s, Random r, int f) { return Item.getItemFromBlock(half); }
        @Override public ItemStack getItem(World w, BlockPos pos, IBlockState s) { return new ItemStack(half); }
    }

    public static class SlabItem extends ItemSlab {
        final String display;

        public SlabItem(HalfSlab half, FullSlab full) {
            super(half, half, full);
            this.display = half.display;
        }

        @Override public String getItemStackDisplayName(ItemStack s) { return disp(display); }
    }


    public static class Stairs extends BlockStairs implements Added {
        final String display;

        public Stairs(IBlockState base, String display) {
            super(base);
            this.display = display;
        }

        @Override public String displayName() { return display; }
        @Override public void lppSound(SoundType s) { setSoundType(s); }
        @Override public String getLocalizedName() { return disp(display); }
    }

    public static class Fence extends BlockFence implements Added {
        final String display;

        public Fence(Material m, MapColor c, String display) {
            super(m, c);
            this.display = display;
        }

        @Override public String displayName() { return display; }
        @Override public void lppSound(SoundType s) { setSoundType(s); }
        @Override public String getLocalizedName() { return disp(display); }
    }

    public static class Wall extends BlockWall implements Added {
        final String display;

        public Wall(Block base, String display) {
            super(base);
            this.display = display;
        }

        @Override public String displayName() { return display; }
        @Override public void lppSound(SoundType s) { setSoundType(s); }
        @Override public String getLocalizedName() { return disp(display); }
        @Override public int damageDropped(IBlockState s) { return 0; }
        @Override public void getSubBlocks(CreativeTabs tab, NonNullList<ItemStack> list) { list.add(new ItemStack(this)); }
    }

    public static class Pane extends BlockPane implements Added {
        final String display;

        public Pane(Material m, String display) {
            super(m, true);
            this.display = display;
        }

        @Override public String displayName() { return display; }
        @Override public void lppSound(SoundType s) { setSoundType(s); }
        @Override public String getLocalizedName() { return disp(display); }
    }

    public static class Ladder extends BlockLadder implements Added {
        final String display;
        @Nullable final String tool;

        public Ladder(String tool, int harvest, float slip, String display) {
            this.display = display;
            this.tool = tool == null || tool.equalsIgnoreCase("none") ? null : tool;
            if (this.tool != null && harvest >= 0) setHarvestLevel(this.tool, harvest);
            slipperiness = slip;
        }

        @Override public String displayName() { return display; }
        @Override public void lppSound(SoundType s) { setSoundType(s); }
        @Override public String getLocalizedName() { return disp(display); }
        @Override public boolean isToolEffective(String type, IBlockState s) { return type.equals(tool); }
        @Nullable @Override public String getHarvestTool(IBlockState s) { return tool; }
    }


    public static class Button extends BlockButton implements Added {
        final String display;
        final int time;

        public Button(boolean arrows, int time, String display) {
            super(arrows);
            this.display = display;
            this.time = time;
        }

        @Override public String displayName() { return display; }
        @Override public void lppSound(SoundType s) { setSoundType(s); }
        @Override public String getLocalizedName() { return disp(display); }
        @Override public int tickRate(World w) { return time; }

        @Override
        protected void playClickSound(@Nullable EntityPlayer p, World w, BlockPos pos) {
            w.playSound(p, pos, SoundEvents.BLOCK_STONE_BUTTON_CLICK_ON, SoundCategory.BLOCKS, 0.3F, 0.6F);
        }

        @Override
        protected void playReleaseSound(World w, BlockPos pos) {
            w.playSound(null, pos, SoundEvents.BLOCK_STONE_BUTTON_CLICK_OFF, SoundCategory.BLOCKS, 0.3F, 0.5F);
        }
    }

    public static class Plate extends BlockPressurePlate implements Added {
        final String display;

        public Plate(Material m, Sensitivity s, String display) {
            super(m, s);
            this.display = display;
        }

        @Override public String displayName() { return display; }
        @Override public void lppSound(SoundType s) { setSoundType(s); }
        @Override public String getLocalizedName() { return disp(display); }
    }

    public static class Gate extends BlockFenceGate implements Added {
        final String display;

        public Gate(String display) {
            super(BlockPlanks.EnumType.OAK);
            this.display = display;
        }

        @Override public String displayName() { return display; }
        @Override public void lppSound(SoundType s) { setSoundType(s); }
        @Override public String getLocalizedName() { return disp(display); }
    }

    public static class Door extends BlockDoor implements Added {
        final String display;
        final boolean onlyRedstone;
        public Item doorItem = Items.AIR;

        public Door(Material m, boolean onlyRedstone, String display) {
            super(m);
            this.display = display;
            this.onlyRedstone = onlyRedstone;
        }

        @Override public String displayName() { return display; }
        @Override public void lppSound(SoundType s) { setSoundType(s); }
        @Override public String getLocalizedName() { return disp(display); }
        @Override public Item getItemDropped(IBlockState s, Random r, int f) { return s.getValue(HALF) == EnumDoorHalf.UPPER ? Items.AIR : doorItem; }
        @Override public ItemStack getItem(World w, BlockPos pos, IBlockState s) { return new ItemStack(doorItem); }

        @Override
        public boolean onBlockActivated(World w, BlockPos pos, IBlockState state, EntityPlayer pl, EnumHand hand, EnumFacing side, float x, float y, float z) {
            if (onlyRedstone) return false;
            BlockPos lower = state.getValue(HALF) == EnumDoorHalf.LOWER ? pos : pos.down();
            IBlockState ls = pos.equals(lower) ? state : w.getBlockState(lower);
            if (ls.getBlock() != this) return false;
            ls = ls.cycleProperty(OPEN);
            w.setBlockState(lower, ls, 10);
            w.markBlockRangeForRenderUpdate(lower, pos);
            w.playEvent(pl, ls.getValue(OPEN) ? 1006 : 1012, pos, 0);
            return true;
        }
    }

    public static class DoorItem extends ItemDoor {
        final String display;

        public DoorItem(Door d) {
            super(d);
            this.display = d.display;
        }

        @Override public String getItemStackDisplayName(ItemStack s) { return disp(display); }
    }


    public static class Cake extends Generic {
        private static final PropertyInteger[] BITES = new PropertyInteger[17];
        static int nextBites = 7;

        static PropertyInteger bitesProp(int n) {
            if (n < 2 || n > 16) return null;
            if (BITES[n] == null) BITES[n] = PropertyInteger.create("bites", 0, n - 1);
            return BITES[n];
        }

        final int numBites, hunger;
        final float saturation;
        final boolean alwaysEdible;
        @Nullable final PropertyInteger bites;
        public final List<LppEffects.PotionSpec> effects = new ArrayList<>();

        public Cake(float slip, int numBites, int hunger, float sat, boolean always, String display) {
            super(Material.CAKE, new Props(display, 0, false, null), -1, slip);
            this.numBites = numBites;
            this.hunger = hunger;
            this.saturation = sat;
            this.alwaysEdible = always;
            this.bites = bitesProp(numBites);
            if (bites != null) setDefaultState(blockState.getBaseState().withProperty(bites, 0));
        }

        @Override
        protected BlockStateContainer createBlockState() {
            PropertyInteger pr = bitesProp(nextBites);
            return pr == null ? new BlockStateContainer(this) : new BlockStateContainer(this, pr);
        }

        private AxisAlignedBB box(IBlockState s) {
            float d = 0.0625F;
            int b = bites == null ? 0 : s.getValue(bites);
            float dist = b * (1.0F - 2.0F * d) / numBites;
            return new AxisAlignedBB(d + dist, 0.0, d, 1.0 - d, 0.5, 1.0 - d);
        }

        @Override public AxisAlignedBB getBoundingBox(IBlockState s, IBlockAccess w, BlockPos pos) { return box(s); }
        @Override public AxisAlignedBB getCollisionBoundingBox(IBlockState s, IBlockAccess w, BlockPos pos) { return box(s); }
        @Override public boolean isOpaqueCube(IBlockState s) { return false; }
        @Override public boolean isFullCube(IBlockState s) { return false; }
        @Override @SideOnly(Side.CLIENT) public BlockRenderLayer getRenderLayer() { return BlockRenderLayer.CUTOUT; }
        @Override public int quantityDropped(Random r) { return 0; }
        @Override public Item getItemDropped(IBlockState s, Random r, int f) { return Items.AIR; }
        @Override public boolean hasComparatorInputOverride(IBlockState s) { return true; }
        @Override public int getComparatorInputOverride(IBlockState s, World w, BlockPos pos) { return bites == null ? 0 : numBites + 1 - s.getValue(bites); }
        @Override public IBlockState getStateFromMeta(int meta) { return bites == null ? getDefaultState() : getDefaultState().withProperty(bites, Math.min(meta, numBites - 1)); }
        @Override public int getMetaFromState(IBlockState s) { return bites == null ? 0 : s.getValue(bites); }

        @Override
        public boolean canPlaceBlockAt(World w, BlockPos pos) {
            return super.canPlaceBlockAt(w, pos) && w.getBlockState(pos.down()).getMaterial().isSolid();
        }

        @Override
        public void neighborChanged(IBlockState s, World w, BlockPos pos, Block b, BlockPos from) {
            if (!w.getBlockState(pos.down()).getMaterial().isSolid()) w.setBlockToAir(pos);
        }

        @Override
        public boolean onBlockActivated(World w, BlockPos pos, IBlockState state, EntityPlayer pl, EnumHand hand, EnumFacing side, float x, float y, float z) {
            if (!pl.canEat(alwaysEdible)) return false;
            pl.getFoodStats().addStats(hunger, saturation);
            int taken = bites == null ? 0 : state.getValue(bites);
            if (taken + 1 < numBites && bites != null) w.setBlockState(pos, state.withProperty(bites, taken + 1), 3);
            else w.setBlockToAir(pos);
            if (!w.isRemote) for (LppEffects.PotionSpec e : effects) e.applyIfLucky(pl, true);
            return true;
        }
    }


    public static class Crop extends BlockCrops implements Added {
        final String display;
        public ItemStack seed = ItemStack.EMPTY;
        final boolean bonemeal;

        public Crop(boolean bonemeal, String display) {
            this.display = display;
            this.bonemeal = bonemeal;
        }

        @Override public String displayName() { return display; }
        @Override public void lppSound(SoundType s) { setSoundType(s); }
        @Override public String getLocalizedName() { return disp(display); }
        @Override protected Item getSeed() { return seed.isEmpty() ? Items.WHEAT_SEEDS : seed.getItem(); }
        @Override protected Item getCrop() { return getSeed(); }
        @Override public boolean canUseBonemeal(World w, Random r, BlockPos pos, IBlockState s) { return bonemeal && super.canUseBonemeal(w, r, pos, s); }
    }


    public static class Workbench extends Generic {
        public Workbench(Material m, Props p, int harvest, float slip) {
            super(m, p, harvest, slip);
        }

        @Override
        public boolean onBlockActivated(World w, BlockPos pos, IBlockState state, EntityPlayer pl, EnumHand hand, EnumFacing side, float x, float y, float z) {
            if (w.isRemote) return true;
            pl.displayGui(new BenchGui(w, pos));
            return true;
        }
    }

    public static class BenchContainer extends ContainerWorkbench {
        private final World world;
        private final BlockPos pos;

        public BenchContainer(net.minecraft.entity.player.InventoryPlayer inv, World w, BlockPos pos) {
            super(inv, w, pos);
            this.world = w;
            this.pos = pos;
        }

        @Override
        public boolean canInteractWith(EntityPlayer p) {
            return world.getBlockState(pos).getBlock() instanceof Workbench
                    && p.getDistanceSq(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64.0;
        }
    }

    public static class BenchGui implements IInteractionObject {
        private final World world;
        private final BlockPos pos;

        BenchGui(World w, BlockPos pos) {
            this.world = w;
            this.pos = pos;
        }

        @Override public String getName() { return "crafting_table"; }
        @Override public boolean hasCustomName() { return false; }
        @Override public ITextComponent getDisplayName() { return new TextComponentTranslation("container.crafting"); }
        @Override public Container createContainer(net.minecraft.entity.player.InventoryPlayer inv, EntityPlayer p) { return new BenchContainer(inv, world, pos); }
        @Override public String getGuiID() { return "minecraft:crafting_table"; }
    }


    public static final PropertyDirection FACING = BlockHorizontal.FACING;
    public static final PropertyBool ACTIVE = PropertyBool.create("active");

    public static class Furnace extends Generic {
        public final float speed;

        public Furnace(Material m, Props p, int harvest, float slip, float speed) {
            super(m, p, harvest, slip);
            this.speed = speed <= 0 ? 1.0F : speed;
            setDefaultState(blockState.getBaseState().withProperty(FACING, EnumFacing.NORTH).withProperty(ACTIVE, false));
        }

        @Override protected BlockStateContainer createBlockState() { return new BlockStateContainer(this, FACING, ACTIVE); }
        @Override public boolean hasTileEntity(IBlockState s) { return true; }
        @Override public TileEntity createTileEntity(World w, IBlockState s) { return new FurnaceTile(); }
        @Override public EnumBlockRenderType getRenderType(IBlockState s) { return EnumBlockRenderType.MODEL; }
        @Override public int getLightValue(IBlockState s) { return s.getValue(ACTIVE) ? Math.max(14, super.getLightValue(s)) : super.getLightValue(s); }

        @Override
        public IBlockState getStateFromMeta(int meta) {
            return getDefaultState().withProperty(FACING, EnumFacing.byHorizontalIndex(meta & 3)).withProperty(ACTIVE, (meta & 4) != 0);
        }

        @Override
        public int getMetaFromState(IBlockState s) {
            return s.getValue(FACING).getHorizontalIndex() | (s.getValue(ACTIVE) ? 4 : 0);
        }

        @Override
        public IBlockState getStateForPlacement(World w, BlockPos pos, EnumFacing f, float x, float y, float z, int meta, EntityLivingBase placer, EnumHand hand) {
            return getDefaultState().withProperty(FACING, placer.getHorizontalFacing().getOpposite()).withProperty(ACTIVE, false);
        }

        @Override public IBlockState withRotation(IBlockState s, Rotation r) { return s.withProperty(FACING, r.rotate(s.getValue(FACING))); }
        @Override public IBlockState withMirror(IBlockState s, Mirror m) { return s.withRotation(m.toRotation(s.getValue(FACING))); }

        @Override
        public boolean onBlockActivated(World w, BlockPos pos, IBlockState state, EntityPlayer pl, EnumHand hand, EnumFacing side, float x, float y, float z) {
            if (w.isRemote) return true;
            TileEntity te = w.getTileEntity(pos);
            if (te instanceof FurnaceTile) pl.displayGUIChest((IInventory) te);
            return true;
        }

        @Override
        public void breakBlock(World w, BlockPos pos, IBlockState state) {
            TileEntity te = w.getTileEntity(pos);
            if (te instanceof FurnaceTile) {
                InventoryHelper.dropInventoryItems(w, pos, (IInventory) te);
            }
            super.breakBlock(w, pos, state);
        }
    }

    public static class FurnaceTile extends TileEntityFurnace {
        @Override
        public void update() {
            float speed = getBlockType() instanceof Furnace ? ((Furnace) getBlockType()).speed : 1.0F;
            boolean wasBurning = isBurning();
            boolean dirty = false;
            carry += speed;
            int steps = (int) carry;
            carry -= steps;
            if (world.isRemote) {
                if (isBurning() && steps > 0) setField(0, getField(0) - steps);
                return;
            }
            for (int s = 0; s < steps; s++) {
                if (isBurning()) setField(0, getField(0) - 1);
                ItemStack fuel = getStackInSlot(1);
                if (isBurning() || !fuel.isEmpty() && !getStackInSlot(0).isEmpty()) {
                    if (!isBurning() && canSmeltNow()) {
                        int burn = getItemBurnTime(fuel);
                        setField(0, burn);
                        setField(1, burn);
                        if (isBurning()) {
                            dirty = true;
                            if (!fuel.isEmpty()) {
                                Item item = fuel.getItem();
                                fuel.shrink(1);
                                if (fuel.isEmpty()) setInventorySlotContents(1, item.getContainerItem(fuel));
                            }
                        }
                    }
                    if (isBurning() && canSmeltNow()) {
                        setField(2, getField(2) + 1);
                        if (getField(2) == getField(3)) {
                            setField(2, 0);
                            setField(3, getCookTime(getStackInSlot(0)));
                            smeltNow();
                            dirty = true;
                        }
                    } else {
                        setField(2, 0);
                    }
                } else if (!isBurning() && getField(2) > 0) {
                    setField(2, Math.max(0, getField(2) - 2));
                }
            }
            if (wasBurning != isBurning()) {
                dirty = true;
                IBlockState st = world.getBlockState(pos);
                if (st.getBlock() instanceof Furnace) world.setBlockState(pos, st.withProperty(ACTIVE, isBurning()), 3);
            }
            if (dirty) markDirty();
        }

        private float carry;

        private boolean canSmeltNow() {
            ItemStack in = getStackInSlot(0);
            if (in.isEmpty()) return false;
            ItemStack res = FurnaceRecipes.instance().getSmeltingResult(in);
            if (res.isEmpty()) return false;
            ItemStack out = getStackInSlot(2);
            if (out.isEmpty()) return true;
            if (!out.isItemEqual(res)) return false;
            int n = out.getCount() + res.getCount();
            return n <= getInventoryStackLimit() && n <= out.getMaxStackSize();
        }

        private void smeltNow() {
            if (!canSmeltNow()) return;
            ItemStack in = getStackInSlot(0);
            ItemStack res = FurnaceRecipes.instance().getSmeltingResult(in);
            ItemStack out = getStackInSlot(2);
            if (out.isEmpty()) setInventorySlotContents(2, res.copy());
            else if (out.getItem() == res.getItem()) out.grow(res.getCount());
            in.shrink(1);
        }
    }
}
