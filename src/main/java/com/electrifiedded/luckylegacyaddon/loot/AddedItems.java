package com.electrifiedded.luckylegacyaddon.loot;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.block.state.IBlockState;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.entity.projectile.EntityThrowable;
import net.minecraft.entity.projectile.EntityTippedArrow;
import net.minecraft.init.Blocks;
import net.minecraft.init.Enchantments;
import net.minecraft.init.Items;
import net.minecraft.init.SoundEvents;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.EnumAction;
import net.minecraft.item.Item;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemAxe;
import net.minecraft.item.ItemFood;
import net.minecraft.item.ItemHoe;
import net.minecraft.item.ItemPickaxe;
import net.minecraft.item.ItemSpade;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.ActionResult;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EntityDamageSourceIndirect;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import com.electrifiedded.archaiclib.legacy.sound.LegacySounds;

import javax.annotation.Nullable;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

public final class AddedItems {
    private AddedItems() {}

    public static final DecimalFormat FMT = new DecimalFormat("#.###");

    public interface Added {
        String displayName();
    }

    static String disp(String raw) {
        return LppUtil.display(raw);
    }

    private static SoundEvent sound(String legacy, SoundEvent def) {
        SoundEvent s = null;
        try {
            s = LegacySounds.get(legacy);
        } catch (java.lang.Throwable ignored) {
        }
        return s == null ? def : s;
    }


    public static class Basic extends Item implements Added {
        public final String display;
        public final boolean shines;

        public Basic(boolean shines, String display) {
            this.shines = shines;
            this.display = display;
        }

        @Override public String displayName() { return display; }
        @Override public String getItemStackDisplayName(ItemStack s) { return disp(display); }
        @Override public boolean hasEffect(ItemStack s) { return shines || super.hasEffect(s); }
    }

    public static class Food extends ItemFood implements Added {
        public final String display;
        public final boolean shines;
        public final int eatTime;
        public final List<LppEffects.PotionSpec> effects = new ArrayList<>();

        public Food(int hunger, float sat, boolean wolves, boolean always, int eatTime, boolean shines, String display) {
            super(hunger, sat, wolves);
            if (always) setAlwaysEdible();
            this.eatTime = eatTime;
            this.shines = shines;
            this.display = display;
        }

        @Override public String displayName() { return display; }
        @Override public String getItemStackDisplayName(ItemStack s) { return disp(display); }
        @Override public boolean hasEffect(ItemStack s) { return shines; }
        @Override public int getMaxItemUseDuration(ItemStack s) { return eatTime; }

        @Override
        protected void onFoodEaten(ItemStack stack, World world, EntityPlayer player) {
            if (!world.isRemote) {
                for (LppEffects.PotionSpec e : effects) e.applyIfLucky(player, true);
            }
        }
    }


    public static class ThrowableItem extends Basic {
        public final float damage, velocity, gravity, inaccuracy, chance;
        public final LppDrops.DropList drops;

        public ThrowableItem(boolean shines, float damage, float velocity, float gravity, float inaccuracy, float chance,
                         LppDrops.DropList drops, String display) {
            super(shines, display);
            this.damage = damage;
            this.velocity = velocity;
            this.gravity = gravity;
            this.inaccuracy = inaccuracy;
            this.chance = chance;
            this.drops = drops;
        }

        @Override
        public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
            ItemStack stack = player.getHeldItem(hand);
            if (!player.capabilities.isCreativeMode) stack.shrink(1);
            world.playSound(null, player.posX, player.posY, player.posZ, SoundEvents.ENTITY_ARROW_SHOOT, SoundCategory.NEUTRAL,
                    0.5F, 0.4F / (itemRand.nextFloat() * 0.4F + 0.8F));
            if (!world.isRemote) {
                Thrown t = new Thrown(world, player, this);
                t.shoot(player, player.rotationPitch, player.rotationYaw, 0.0F, velocity, 1.0F); // 1.8 EntityThrowable: spread always 1.0
                world.spawnEntity(t);
            }
            return new ActionResult<>(EnumActionResult.SUCCESS, stack);
        }
    }

    public static class Thrown extends EntityThrowable {
        private static final DataParameter<String> ITEM = EntityDataManager.createKey(Thrown.class, DataSerializers.STRING);
        public float damage;
        public float punch;
        public float gravity = 0.03F;
        @Nullable private ThrowableItem item;

        public Thrown(World w) {
            super(w);
        }

        public Thrown(World w, EntityLivingBase thrower, @Nullable ThrowableItem item) {
            super(w, thrower);
            setItem(item);
        }

        @Override
        protected void entityInit() {
            super.entityInit();
            dataManager.register(ITEM, "");
        }

        public void setItem(@Nullable ThrowableItem it) {
            this.item = it;
            if (it != null) {
                damage = it.damage;
                gravity = it.gravity;
                dataManager.set(ITEM, it.getRegistryName().toString());
            }
        }

        @Nullable
        public ThrowableItem getItem() {
            if (item == null) {
                String n = dataManager.get(ITEM);
                if (!n.isEmpty()) {
                    Item i = Item.REGISTRY.getObject(new ResourceLocation(n));
                    if (i instanceof ThrowableItem) item = (ThrowableItem) i;
                }
            }
            return item;
        }

        @Override
        protected float getGravityVelocity() {
            return gravity;
        }

        @Override
        protected void onImpact(RayTraceResult r) {
            if (world.isRemote) return;
            ThrowableItem it = getItem();
            if (r.entityHit != null && damage >= 0.0F) {
                r.entityHit.attackEntityFrom(new EntityDamageSourceIndirect("lppthrown", this, getThrower()).setProjectile(), damage);
                if (punch > 0.0F) {
                    float f = MathHelper.sqrt(motionX * motionX + motionZ * motionZ);
                    if (f > 0.0F) r.entityHit.addVelocity(motionX * punch * 0.6F / f, 0.1, motionZ * punch * 0.6F / f);
                }
            }
            if (it != null && it.drops != null && !it.drops.isEmpty() && rand.nextFloat() <= it.chance) {
                it.drops.run(world, posX, posY, posZ, getName(), 0, rand);
            }
            if (world instanceof WorldServer) {
                ((WorldServer) world).spawnParticle(EnumParticleTypes.ITEM_CRACK, posX, posY, posZ, 8, 0.0, 0.0, 0.0, 0.04,
                        Item.getIdFromItem(it != null ? it : net.minecraft.init.Items.SNOWBALL));
            }
            setDead();
        }

        @Override
        public void writeEntityToNBT(NBTTagCompound c) {
            super.writeEntityToNBT(c);
            c.setString("ItemThrown", dataManager.get(ITEM));
            c.setFloat("Damage", damage);
            c.setFloat("Punch", punch);
            c.setFloat("Gravity", gravity);
        }

        @Override
        public void readEntityFromNBT(NBTTagCompound c) {
            super.readEntityFromNBT(c);
            dataManager.set(ITEM, c.getString("ItemThrown"));
            item = null;
            getItem();
            // 1.8 Loot++ semantics: missing keys read as 0 (Lucky drops spawning lootplusplus.ThrownItem fly straight, no damage)
            damage = c.getFloat("Damage");
            punch = c.getFloat("Punch");
            gravity = c.getFloat("Gravity");
        }
    }


    static boolean has(EntityPlayer p, Item ammo) {
        for (ItemStack s : p.inventory.mainInventory) if (!s.isEmpty() && s.getItem() == ammo) return true;
        for (ItemStack s : p.inventory.offHandInventory) if (!s.isEmpty() && s.getItem() == ammo) return true;
        return false;
    }

    static void consume(EntityPlayer p, Item ammo) {
        for (ItemStack s : p.inventory.mainInventory) {
            if (!s.isEmpty() && s.getItem() == ammo) {
                s.shrink(1);
                return;
            }
        }
        for (ItemStack s : p.inventory.offHandInventory) {
            if (!s.isEmpty() && s.getItem() == ammo) {
                s.shrink(1);
                return;
            }
        }
    }

    static boolean repairable(@Nullable ItemStack repair, ItemStack material) {
        return repair != null && !repair.isEmpty() && repair.getItem() == material.getItem()
                && (repair.getMetadata() == 32767 || repair.getMetadata() == material.getMetadata());
    }

    public static class Bow extends Basic {
        public final float baseDamage;
        public final int drawTime, arrowCount, enchantability;
        @Nullable public final ItemStack repair;
        public final Item ammo;
        public final SoundEvent shootSound;

        public Bow(String display, float damage, int time, int durability, int arrows, int enchant, @Nullable ItemStack repair, Item ammo, String sound) {
            super(false, display);
            setMaxDamage(durability);
            setMaxStackSize(1);
            this.baseDamage = damage;
            this.drawTime = time;
            this.arrowCount = arrows;
            this.enchantability = enchant;
            this.repair = repair;
            this.ammo = ammo;
            this.shootSound = AddedItems.sound(sound, SoundEvents.ENTITY_ARROW_SHOOT);
        }

        @Override public int getMaxItemUseDuration(ItemStack s) { return 72000; }
        @Override public EnumAction getItemUseAction(ItemStack s) { return EnumAction.BOW; }
        @Override public int getItemEnchantability() { return enchantability; }
        @Override public boolean getIsRepairable(ItemStack tool, ItemStack mat) { return repairable(repair, mat); }

        public int getDrawTime(ItemStack stack) {
            return drawTime;
        }

        @Override
        @SideOnly(Side.CLIENT)
        public void addInformation(ItemStack stack, @Nullable World w, List<String> tip, ITooltipFlag f) {
            tip.add("");
            tip.add(TextFormatting.BLUE + "+" + FMT.format(getDrawTime(stack) / 20.0F) + " Draw Time");
        }

        private boolean infinite(EntityPlayer p, ItemStack stack) {
            return p.capabilities.isCreativeMode || arrowCount == 0
                    || EnchantmentHelper.getEnchantmentLevel(Enchantments.INFINITY, stack) > 0;
        }

        @Override
        public void onPlayerStoppedUsing(ItemStack stack, World world, EntityLivingBase living, int timeLeft) {
            if (!(living instanceof EntityPlayer)) return;
            EntityPlayer player = (EntityPlayer) living;
            boolean inf = infinite(player, stack);
            boolean hasAmmo = inf || has(player, ammo);
            int charge = getMaxItemUseDuration(stack) - timeLeft;
            if (ammo == Items.ARROW) {
                charge = ForgeEventFactory.onArrowLoose(stack, world, player, charge, hasAmmo);
                if (charge < 0) return;
            }
            if (!hasAmmo) return;
            float f = (float) charge / Math.max(1, getDrawTime(stack));
            f = (f * f + f * 2.0F) / 3.0F;
            if (f < 0.1F) return;
            if (f > 1.0F) f = 1.0F;
            if (!world.isRemote) {
                int power = EnchantmentHelper.getEnchantmentLevel(Enchantments.POWER, stack);
                int punch = EnchantmentHelper.getEnchantmentLevel(Enchantments.PUNCH, stack);
                int shots = Math.max(1, arrowCount);
                for (int i = 0; i < shots && (inf || has(player, ammo)); i++) {
                    if (ammo == Items.ARROW) {
                        EntityTippedArrow a = new EntityTippedArrow(world, player);
                        a.shoot(player, player.rotationPitch, player.rotationYaw, 0.0F, f * 3.0F, 1.0F);
                        a.posY += 0.25 * (i % 4);
                        if (f == 1.0F) a.setIsCritical(true);
                        double dmg = a.getDamage() + baseDamage;
                        if (power > 0) dmg += power * 0.5 + 0.5;
                        a.setDamage(dmg);
                        if (punch > 0) a.setKnockbackStrength(punch);
                        if (EnchantmentHelper.getEnchantmentLevel(Enchantments.FLAME, stack) > 0) a.setFire(100);
                        if (inf) a.pickupStatus = EntityArrow.PickupStatus.CREATIVE_ONLY;
                        else consume(player, Items.ARROW);
                        world.spawnEntity(a);
                    } else if (ammo instanceof ThrowableItem) {
                        ThrowableItem t = (ThrowableItem) ammo;
                        if (!inf) consume(player, ammo);
                        Thrown e = new Thrown(world, player, t);
                        e.shoot(player, player.rotationPitch, player.rotationYaw, 0.0F, t.velocity * f * 2.0F, 1.0F);
                        if (f == 1.0F) e.damage = (float) (e.damage * 1.5);
                        if (power > 0 && e.damage > 0.0F) e.damage += power * 0.5F + 0.5F;
                        if (punch > 0) e.punch = punch;
                        world.spawnEntity(e);
                    }
                }
                stack.damageItem(1, player);
            }
            world.playSound(null, player.posX, player.posY, player.posZ, shootSound, SoundCategory.NEUTRAL, 1.0F,
                    1.0F / (itemRand.nextFloat() * 0.4F + 1.2F) + f * 0.5F);
        }

        @Override
        public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
            ItemStack stack = player.getHeldItem(hand);
            boolean has = infinite(player, stack) || has(player, ammo);
            if (ammo == Items.ARROW) {
                ActionResult<ItemStack> r = ForgeEventFactory.onArrowNock(stack, world, player, hand, has);
                if (r != null) return r;
            }
            if (!has) return new ActionResult<>(EnumActionResult.FAIL, stack);
            player.setActiveHand(hand);
            return new ActionResult<>(EnumActionResult.SUCCESS, stack);
        }
    }

    public static class Gun extends Basic {
        public final float baseDamage;
        public final int waitTime, bullets, enchantability;
        @Nullable public final ItemStack repair;
        public final Item ammo;
        public final ThrowableItem shot;
        public final SoundEvent shootSound;

        public Gun(String display, float damage, int time, int durability, int bullets, int enchant, @Nullable ItemStack repair,
                   Item ammo, ThrowableItem shot, String sound) {
            super(false, display);
            setMaxDamage(durability);
            setMaxStackSize(1);
            this.baseDamage = damage;
            this.waitTime = time;
            this.bullets = bullets;
            this.enchantability = enchant;
            this.repair = repair;
            this.ammo = ammo;
            this.shot = shot;
            this.shootSound = AddedItems.sound(sound, SoundEvents.ENTITY_ARROW_SHOOT);
        }

        @Override public int getItemEnchantability() { return enchantability; }
        @Override public boolean getIsRepairable(ItemStack tool, ItemStack mat) { return repairable(repair, mat); }

        @Override
        @SideOnly(Side.CLIENT)
        public void addInformation(ItemStack stack, @Nullable World w, List<String> tip, ITooltipFlag f) {
            tip.add("");
            if (baseDamage > 0.0F) {
                tip.add(TextFormatting.BLUE + "+" + FMT.format(baseDamage + shot.damage) + " Ranged Damage");
            }
            tip.add(TextFormatting.BLUE + "+" + FMT.format(waitTime / 20.0F) + " Reload Time");
        }

        @Override
        public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
            ItemStack stack = player.getHeldItem(hand);
            if (world.isRemote) return new ActionResult<>(EnumActionResult.PASS, stack);
            NBTTagCompound tag = stack.getTagCompound();
            if (tag == null) {
                tag = new NBTTagCompound();
                stack.setTagCompound(tag);
            }
            int stamp = tag.getInteger("TickStamp");
            if (stamp <= player.ticksExisted && player.ticksExisted - stamp < waitTime) {
                return new ActionResult<>(EnumActionResult.FAIL, stack);
            }
            boolean inf = player.capabilities.isCreativeMode || bullets == 0
                    || EnchantmentHelper.getEnchantmentLevel(Enchantments.INFINITY, stack) > 0;
            if (inf || has(player, ammo)) {
                int power = EnchantmentHelper.getEnchantmentLevel(Enchantments.POWER, stack);
                int punch = EnchantmentHelper.getEnchantmentLevel(Enchantments.PUNCH, stack);
                for (int i = 0; i < Math.max(1, bullets) && (inf || has(player, ammo)); i++) {
                    if (!inf) consume(player, ammo);
                    Thrown e = new Thrown(world, player, shot);
                    e.shoot(player, player.rotationPitch, player.rotationYaw, 0.0F, shot.velocity, 1.0F);
                    if (e.damage > 0.0F) e.damage = e.damage + power * 1.25F + baseDamage;
                    if (punch > 0) e.punch = punch;
                    world.spawnEntity(e);
                }
                stack.damageItem(1, player);
                world.playSound(null, player.posX, player.posY, player.posZ, shootSound, SoundCategory.NEUTRAL, 1.0F,
                        1.0F / (0.8F + itemRand.nextFloat() * 0.4F));
                tag.setInteger("TickStamp", player.ticksExisted);
                return new ActionResult<>(EnumActionResult.SUCCESS, stack);
            }
            return new ActionResult<>(EnumActionResult.FAIL, stack);
        }
    }


    public static class Sword extends ItemSword implements Added {
        public final String display;
        public final float bonus;

        public Sword(ToolMaterial m, float bonus, String display) {
            super(m);
            this.display = display;
            this.bonus = bonus + m.getAttackDamage();
        }

        @Override public String displayName() { return display; }
        @Override public String getItemStackDisplayName(ItemStack s) { return disp(display); }

        @Override
        public Multimap<String, AttributeModifier> getItemAttributeModifiers(EntityEquipmentSlot slot) {
            Multimap<String, AttributeModifier> map = HashMultimap.create();
            if (slot == EntityEquipmentSlot.MAINHAND) {
                map.put(SharedMonsterAttributes.ATTACK_DAMAGE.getName(), new AttributeModifier(ATTACK_DAMAGE_MODIFIER, "Weapon modifier", bonus, 0));
                map.put(SharedMonsterAttributes.ATTACK_SPEED.getName(), new AttributeModifier(ATTACK_SPEED_MODIFIER, "Weapon modifier", -2.4000000953674316D, 0));
            }
            return map;
        }
    }

    public static class Pickaxe extends ItemPickaxe implements Added {
        public final String display;
        public Pickaxe(ToolMaterial m, String d) { super(m); display = d; }
        @Override public String displayName() { return display; }
        @Override public String getItemStackDisplayName(ItemStack s) { return disp(display); }
    }

    public static class Axe extends ItemAxe implements Added {
        public final String display;
        public Axe(ToolMaterial m, String d) { super(m, m.getAttackDamage() + 6.0F, -3.1F); display = d; }
        @Override public String displayName() { return display; }
        @Override public String getItemStackDisplayName(ItemStack s) { return disp(display); }
    }

    public static class Shovel extends ItemSpade implements Added {
        public final String display;
        public Shovel(ToolMaterial m, String d) { super(m); display = d; }
        @Override public String displayName() { return display; }
        @Override public String getItemStackDisplayName(ItemStack s) { return disp(display); }
    }

    public static class Hoe extends ItemHoe implements Added {
        public final String display;
        public Hoe(ToolMaterial m, String d) { super(m); display = d; }
        @Override public String displayName() { return display; }
        @Override public String getItemStackDisplayName(ItemStack s) { return disp(display); }
    }

    public static class Armor extends ItemArmor implements Added {
        public final String display;
        public final String texture;

        public Armor(ArmorMaterial m, EntityEquipmentSlot slot, String display, String texture) {
            super(m, 0, slot);
            this.display = display;
            this.texture = texture;
        }

        @Override public String displayName() { return display; }
        @Override public String getItemStackDisplayName(ItemStack s) { return disp(display); }

        @Override
        public String getArmorTexture(ItemStack stack, Entity entity, EntityEquipmentSlot slot, String type) {
            String tex = texture;
            String domain = "minecraft";
            int c = tex.indexOf(':');
            if (c > 0) {
                domain = tex.substring(0, c);
                tex = tex.substring(c + 1);
            }
            // Loot++ 1.8 (like vanilla): <name>_layer_1 for head/chest/feet, _layer_2 for legs, optional _overlay
            if (!tex.matches(".*_layer_[12](_overlay)?$"))
                tex = tex + "_layer_" + (slot == EntityEquipmentSlot.LEGS ? 2 : 1) + (type == null ? "" : "_" + type);
            return domain + ":textures/models/armor/" + tex + ".png";
        }
    }

    public static class MultiTool extends Item implements Added {
        public final String display;
        public final float damage, efficiency;
        public final int harvestLevel, enchantability;
        public final Set<String> toolClasses = new HashSet<>();
        @Nullable public final ItemStack repair;
        private final boolean sword;

        public MultiTool(String[] types, int harvest, int durability, float damage, float eff, int ench, @Nullable ItemStack repair, String display) {
            setMaxStackSize(1);
            setMaxDamage(durability);
            this.damage = damage;
            this.harvestLevel = harvest;
            this.efficiency = eff;
            this.enchantability = ench;
            this.repair = repair;
            this.display = display;
            for (String t : types) toolClasses.add(t.trim().toLowerCase(java.util.Locale.ROOT));
            this.sword = toolClasses.contains("sword");
        }

        @Override public String displayName() { return display; }
        @Override public String getItemStackDisplayName(ItemStack s) { return disp(display); }
        @Override public int getItemEnchantability() { return enchantability; }
        @Override public boolean isFull3D() { return true; }
        @Override public boolean getIsRepairable(ItemStack tool, ItemStack mat) { return repairable(repair, mat); }
        @Override public Set<String> getToolClasses(ItemStack s) { return toolClasses; }
        @Override public EnumAction getItemUseAction(ItemStack s) { return sword ? EnumAction.BLOCK : EnumAction.NONE; }
        @Override public int getMaxItemUseDuration(ItemStack s) { return sword ? 72000 : 0; }

        @Override
        public int getHarvestLevel(ItemStack stack, String toolClass, @Nullable EntityPlayer player, @Nullable IBlockState state) {
            return toolClass != null && toolClasses.contains(toolClass) ? harvestLevel : -1;
        }

        @Override
        public float getDestroySpeed(ItemStack stack, IBlockState state) {
            if (sword && state.getBlock() == Blocks.WEB) return 15.0F;
            for (String t : toolClasses) {
                if (state.getBlock().isToolEffective(t, state)) return efficiency;
            }
            return 1.0F;
        }

        @Override
        public boolean canHarvestBlock(IBlockState state) {
            return sword && state.getBlock() == Blocks.WEB;
        }

        @Override
        public boolean hitEntity(ItemStack stack, EntityLivingBase target, EntityLivingBase attacker) {
            stack.damageItem(sword ? 1 : 2, attacker);
            return true;
        }

        @Override
        public boolean onBlockDestroyed(ItemStack stack, World w, IBlockState state, BlockPos pos, EntityLivingBase e) {
            if (state.getBlockHardness(w, pos) != 0.0) stack.damageItem(1, e);
            return true;
        }

        @Override
        public Multimap<String, AttributeModifier> getItemAttributeModifiers(EntityEquipmentSlot slot) {
            Multimap<String, AttributeModifier> map = HashMultimap.create();
            if (slot == EntityEquipmentSlot.MAINHAND) {
                map.put(SharedMonsterAttributes.ATTACK_DAMAGE.getName(), new AttributeModifier(ATTACK_DAMAGE_MODIFIER, "Tool modifier", damage, 0));
                map.put(SharedMonsterAttributes.ATTACK_SPEED.getName(), new AttributeModifier(ATTACK_SPEED_MODIFIER, "Tool modifier", -2.4000000953674316D, 0));
            }
            return map;
        }

        @Override
        public ActionResult<ItemStack> onItemRightClick(World w, EntityPlayer p, EnumHand hand) {
            ItemStack s = p.getHeldItem(hand);
            if (sword) {
                p.setActiveHand(hand);
                return new ActionResult<>(EnumActionResult.SUCCESS, s);
            }
            return new ActionResult<>(EnumActionResult.PASS, s);
        }

        @Override
        public EnumActionResult onItemUse(EntityPlayer p, World w, BlockPos pos, EnumHand hand, EnumFacing side, float x, float y, float z) {
            if (toolClasses.contains("hoe")) return Items.DIAMOND_HOE.onItemUse(p, w, pos, hand, side, x, y, z);
            return EnumActionResult.PASS;
        }
    }
}
