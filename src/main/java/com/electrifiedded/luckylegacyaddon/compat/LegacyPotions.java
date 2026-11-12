package com.electrifiedded.luckylegacyaddon.compat;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.effect.EntityLightningBolt;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.MobEffects;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemPotion;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.*;

/**
 * Re-implementation of the Potion Core (1.8.9) effects used by legacy Lucky addons, registered under the
 * original "potioncore" namespace so commands (/effect @p potioncore:flight) and NBT (Id=potioncore:flight)
 * keep working unchanged. Skipped entirely when the real Potion Core mod is installed.
 */
@Mod.EventBusSubscriber(modid = com.electrifiedded.luckylegacyaddon.Reference.MOD_ID)
public final class LegacyPotions {
    public static final Map<String, Potion> ALL = new LinkedHashMap<>();
    public static Item customPotion;
    private static final UUID REACH = UUID.fromString("9a1f3b0e-4b8e-4d1a-a3f4-6c0b1d2e7a01");
    private static final UUID STEP = UUID.fromString("9a1f3b0e-4b8e-4d1a-a3f4-6c0b1d2e7a02");
    private static final UUID LUCK = UUID.fromString("9a1f3b0e-4b8e-4d1a-a3f4-6c0b1d2e7a03");
    private static final UUID WEIGHT = UUID.fromString("9a1f3b0e-4b8e-4d1a-a3f4-6c0b1d2e7a04");
    private static final UUID SOLID = UUID.fromString("9a1f3b0e-4b8e-4d1a-a3f4-6c0b1d2e7a05");
    private static final Set<UUID> FLYING = new HashSet<>();
    private static final Random RNG = new Random();

    private LegacyPotions() {}

    static boolean active() { return !Loader.isModLoaded("potioncore"); }

    /** Effect with a tick action; instant=true runs once (vanilla instant semantics). */
    public static class P extends Potion {
        final boolean instant;
        P(String name, boolean bad, int color, boolean instant) {
            super(bad, color);
            this.instant = instant;
            setPotionName("effect.potioncore." + name);
            setRegistryName(new ResourceLocation("potioncore", name));
        }
        @Override public boolean isInstant() { return instant; }
        @Override public boolean isReady(int duration, int amp) { return true; }
        @Override public void performEffect(EntityLivingBase e, int amp) { tick(this, e, amp); if (instant && !e.world.isRemote) e.removePotionEffect(this); }
        @Override public void affectEntity(net.minecraft.entity.Entity s, net.minecraft.entity.Entity i, EntityLivingBase e, int amp, double h) { tick(this, e, amp); }
    }

    private static void add(RegistryEvent.Register<Potion> ev, String name, boolean bad, int color, boolean instant) {
        P p = new P(name, bad, color, instant);
        ALL.put(name, p);
        ev.getRegistry().register(p);
    }

    @SubscribeEvent
    public static void potions(RegistryEvent.Register<Potion> ev) {
        if (!active()) return;
        String[] good = {"flight", "levitate", "slowfall", "climb", "stepup", "reach", "purity", "antidote", "archery",
                "bless", "repair", "revival", "recoil", "solidcore", "soildcore", "extension", "health_boost", "love"};
        String[] bad = {"curse", "drown", "fire", "klutz", "perplexity", "rust", "spin", "vulnerable", "weight",
                "disorganization", "invert"};
        String[] instant = {"cure", "dispel", "explode", "burst", "launch", "lightning", "teleport", "teleportsurface", "chance"};
        for (String s : good) add(ev, s, false, 0x55CCFF ^ s.hashCode() & 0x3F3F3F, false);
        for (String s : bad) add(ev, s, true, 0x663344 ^ s.hashCode() & 0x3F3F3F, false);
        for (String s : instant) add(ev, s, false, 0xFFAA33 ^ s.hashCode() & 0x3F3F3F, true);
    }

    @SubscribeEvent
    public static void items(RegistryEvent.Register<Item> ev) {
        if (!active()) return;
        customPotion = new ItemPotion().setRegistryName(new ResourceLocation("potioncore", "custom_potion"))
                .setTranslationKey("potioncore.custom_potion");
        ev.getRegistry().register(customPotion);
    }

    private static boolean has(EntityLivingBase e, String n) {
        Potion p = ALL.get(n);
        return p != null && e.isPotionActive(p);
    }

    private static void mod(EntityLivingBase e, net.minecraft.entity.ai.attributes.IAttribute a, UUID id, double v, int op, boolean on) {
        IAttributeInstance inst = e.getAttributeMap().getAttributeInstance(a);
        if (inst == null) return;
        AttributeModifier cur = inst.getModifier(id);
        if (on && cur == null) inst.applyModifier(new AttributeModifier(id, "potioncore", v, op).setSaved(false));
        else if (!on && cur != null) inst.removeModifier(id);
    }

    static void tick(Potion p, EntityLivingBase e, int amp) {
        String n = p.getRegistryName().getPath();
        if (e.world.isRemote && !n.equals("levitate") && !n.equals("slowfall") && !n.equals("climb") && !n.equals("spin")) return;
        switch (n) {
            case "levitate": e.motionY = 0.05 * (amp + 1) + 0.02; e.fallDistance = 0; break;
            case "slowfall": if (e.motionY < -0.08) e.motionY = -0.08; e.fallDistance = 0; break;
            case "climb": if (e.collidedHorizontally) { e.motionY = 0.2; e.fallDistance = 0; } break;
            case "spin": e.rotationYaw += 15; break;
            case "purity": case "cure": case "antidote":
                for (PotionEffect pe : new ArrayList<>(e.getActivePotionEffects()))
                    if (pe.getPotion().isBadEffect() && (!n.equals("antidote") || pe.getPotion() == MobEffects.POISON)) e.removePotionEffect(pe.getPotion());
                break;
            case "dispel": for (PotionEffect pe : new ArrayList<>(e.getActivePotionEffects())) if (pe.getPotion() != p) e.removePotionEffect(pe.getPotion()); break;
            case "drown": if (e.ticksExisted % 20 == 0) e.setAir(Math.max(-19, e.getAir() - 30 * (amp + 1))); break;
            case "fire": e.setFire(2); break;
            case "klutz": if (e.ticksExisted % 100 == 0 && e instanceof EntityPlayer) ((EntityPlayer) e).dropItem(true); break;
            case "rust": if (e.ticksExisted % 40 == 0) for (ItemStack s : e.getArmorInventoryList()) if (s.isItemStackDamageable()) s.damageItem(amp + 1, e); break;
            case "repair": if (e.ticksExisted % 40 == 0) for (ItemStack s : e.getEquipmentAndArmor()) if (s.isItemDamaged()) s.setItemDamage(Math.max(0, s.getItemDamage() - (amp + 1))); break;
            case "love": if (e.ticksExisted % 40 == 0) for (EntityAnimal a : e.world.getEntitiesWithinAABB(EntityAnimal.class, e.getEntityBoundingBox().grow(8)))
                    if (e instanceof EntityPlayer) a.setInLove((EntityPlayer) e); break;
            case "disorganization": if (e instanceof EntityPlayer && e.ticksExisted % 100 == 0) {
                    List<ItemStack> inv = ((EntityPlayer) e).inventory.mainInventory; Collections.shuffle(inv.subList(0, inv.size()), RNG); } break;
            case "invert": case "perplexity": e.addPotionEffect(new PotionEffect(MobEffects.NAUSEA, 40, 0, false, false)); break;
            case "curse": if (e.ticksExisted % 200 == 0) e.addPotionEffect(new PotionEffect(MobEffects.UNLUCK, 220, amp, false, false)); break;
            case "bless": if (e.ticksExisted % 200 == 0) e.addPotionEffect(new PotionEffect(MobEffects.LUCK, 220, amp, false, false)); break;
            case "health_boost": if (e.ticksExisted % 200 == 0) e.addPotionEffect(new PotionEffect(MobEffects.HEALTH_BOOST, 220, amp, false, false)); break;
            case "extension": if (e.ticksExisted % 20 == 0) for (PotionEffect pe : e.getActivePotionEffects())
                    if (pe.getPotion() != p && !pe.getPotion().isBadEffect()) pe.combine(new PotionEffect(pe.getPotion(), pe.getDuration() + 20, pe.getAmplifier(), pe.getIsAmbient(), pe.doesShowParticles())); break;
            case "explode": e.world.createExplosion(null, e.posX, e.posY, e.posZ, 2f + amp, true); break;
            case "burst": e.world.createExplosion(e, e.posX, e.posY, e.posZ, 1.5f + amp, false); break;
            case "launch": e.motionY += 1.0 + 0.5 * amp; e.velocityChanged = true; break;
            case "lightning": e.world.addWeatherEffect(new EntityLightningBolt(e.world, e.posX, e.posY, e.posZ, false)); break;
            case "teleport": for (int i = 0; i < 16; i++) {
                    double x = e.posX + (RNG.nextDouble() - 0.5) * 32, z = e.posZ + (RNG.nextDouble() - 0.5) * 32;
                    if (e.attemptTeleport(x, e.posY + RNG.nextInt(16) - 8, z)) break; } break;
            case "teleportsurface": { BlockPos top = e.world.getTopSolidOrLiquidBlock(e.getPosition()); e.setPositionAndUpdate(e.posX, top.getY(), e.posZ); } break;
            case "chance": { List<Potion> l = new ArrayList<>(); for (Potion q : Potion.REGISTRY) if (!q.isInstant()) l.add(q);
                e.addPotionEffect(new PotionEffect(l.get(RNG.nextInt(l.size())), 600, amp)); } break;
            default: break;
        }
    }

    /** Attribute-based / state effects that need to switch off when the effect ends. */
    @SubscribeEvent
    public static void living(LivingEvent.LivingUpdateEvent ev) {
        if (ALL.isEmpty()) return;
        EntityLivingBase e = ev.getEntityLiving();
        if (e.world.isRemote) return;
        mod(e, EntityPlayer.REACH_DISTANCE, REACH, 2.0, 0, has(e, "reach"));
        mod(e, SharedMonsterAttributes.MOVEMENT_SPEED, WEIGHT, -0.3, 2, has(e, "weight"));
        mod(e, SharedMonsterAttributes.KNOCKBACK_RESISTANCE, SOLID, 1.0, 0, has(e, "solidcore") || has(e, "soildcore"));
        mod(e, SharedMonsterAttributes.LUCK, LUCK, 2.0, 0, has(e, "bless"));
        boolean step = has(e, "stepup");
        if (step) e.stepHeight = 1.0f; else if (e.stepHeight == 1.0f && e.getEntityData().getBoolean("llaStep")) e.stepHeight = 0.6f;
        e.getEntityData().setBoolean("llaStep", step);
        if (e instanceof EntityPlayer) {
            EntityPlayer pl = (EntityPlayer) e;
            boolean fly = has(e, "flight");
            if (fly && !pl.capabilities.allowFlying) { pl.capabilities.allowFlying = true; FLYING.add(pl.getUniqueID()); pl.sendPlayerAbilities(); }
            else if (!fly && FLYING.remove(pl.getUniqueID()) && !pl.isCreative() && !pl.isSpectator()) {
                pl.capabilities.allowFlying = false; pl.capabilities.isFlying = false; pl.sendPlayerAbilities();
            }
        }
    }

    @SubscribeEvent
    public static void fall(LivingFallEvent ev) {
        if (has(ev.getEntityLiving(), "slowfall") || has(ev.getEntityLiving(), "levitate") || has(ev.getEntityLiving(), "flight")) ev.setCanceled(true);
    }

    @SubscribeEvent
    public static void hurt(LivingHurtEvent ev) {
        EntityLivingBase e = ev.getEntityLiving();
        if (has(e, "vulnerable")) ev.setAmount(ev.getAmount() * 1.5f);
        if (has(e, "recoil") && ev.getSource().getTrueSource() instanceof EntityLivingBase)
            ((EntityLivingBase) ev.getSource().getTrueSource()).attackEntityFrom(net.minecraft.util.DamageSource.MAGIC, ev.getAmount() * 0.5f);
        if (ev.getSource().isProjectile() && ev.getSource().getTrueSource() instanceof EntityLivingBase
                && has((EntityLivingBase) ev.getSource().getTrueSource(), "archery")) ev.setAmount(ev.getAmount() * 1.5f);
    }

    @SubscribeEvent
    public static void death(LivingDeathEvent ev) {
        EntityLivingBase e = ev.getEntityLiving();
        Potion rev = ALL.get("revival");
        if (rev != null && e.isPotionActive(rev)) {
            e.removePotionEffect(rev);
            e.setHealth(e.getMaxHealth() / 2);
            ev.setCanceled(true);
        }
    }

}
