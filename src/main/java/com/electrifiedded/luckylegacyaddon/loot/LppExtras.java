package com.electrifiedded.luckylegacyaddon.loot;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class LppExtras {
    LppExtras() {}

    private static final class Effect {
        final LppConfig.Entry entry;
        final Item item;
        final int meta;
        @Nullable final NBTTagCompound tag;
        final List<LppEffects.PotionSpec> potions = new ArrayList<>();
        String command;
        float probability = 1.0F;

        Effect(LppConfig.Entry entry, Item item, int meta, @Nullable NBTTagCompound tag) {
            this.entry = entry;
            this.item = item;
            this.meta = meta;
            this.tag = tag;
        }
    }

    private static final Map<Item, List<Effect>> HELD = new HashMap<>();
    private static final Map<Item, List<Effect>> INVENTORY = new HashMap<>();
    private static final Map<Item, List<Effect>> ARMOUR = new HashMap<>();
    private static final Map<Item, List<Effect>> DIGGING = new HashMap<>();
    private static final Map<Item, List<Effect>> HIT_ENTITY = new HashMap<>();
    private static final Map<Item, List<Effect>> HIT_SELF = new HashMap<>();

    private static final Map<Item, List<Effect>> CMD_HELD = new HashMap<>();
    private static final Map<Item, List<Effect>> CMD_INVENTORY = new HashMap<>();
    private static final Map<Item, List<Effect>> CMD_ARMOUR = new HashMap<>();
    private static final Map<Item, List<Effect>> CMD_DIGGING = new HashMap<>();
    private static final Map<Item, List<Effect>> CMD_HIT_ENTITY = new HashMap<>();
    private static final Map<Item, List<Effect>> CMD_HIT_SELF = new HashMap<>();

    static void init() {
        add("item_effects/held", HELD, false);
        add("item_effects/in_inventory", INVENTORY, false);
        add("item_effects/wearing_armour", ARMOUR, false);
        add("item_effects/digging_block", DIGGING, false);
        add("item_effects/hitting_entity_to_entity", HIT_ENTITY, false);
        add("item_effects/hitting_entity_to_yourself", HIT_SELF, false);
        add("item_effects/command_held", CMD_HELD, true);
        add("item_effects/command_in_inventory", CMD_INVENTORY, true);
        add("item_effects/command_wearing_armour", CMD_ARMOUR, true);
        add("item_effects/command_digging_block", CMD_DIGGING, true);
        add("item_effects/command_hitting_entity_to_entity", CMD_HIT_ENTITY, true);
        add("item_effects/command_hitting_entity_to_yourself", CMD_HIT_SELF, true);
    }

    private static void add(String key, Map<Item, List<Effect>> target, boolean commands) {
        LppUtil.each(key, (e, p) -> {
            LppUtil.needMin(e, p, 8);
            Item item = LppUtil.item(p[0]);
            if (item == null) throw LppUtil.skip("unknown item '" + p[0] + "'");
            int meta = LppUtil.i(p[1]);
            NBTTagCompound tag = LppUtil.nbt(p[2]);
            Effect eff = new Effect(e, item, meta, tag);
            boolean command = commands && p.length <= 6;
            if (command) {
                eff.probability = LppUtil.f(p[3]);
                eff.command = p[4].trim();
                if (eff.command.startsWith("/")) eff.command = eff.command.substring(1);
                if (eff.command.contains("potioncore:") && !net.minecraftforge.fml.common.Loader.isModLoaded("potioncore") && com.electrifiedded.luckylegacyaddon.compat.LegacyPotions.ALL.isEmpty()) {
                    LppUtil.warn(e, "Potion Core effect is not available, command skipped: " + eff.command);
                    return;
                }
            } else {
                int k = 3;
                while (k + 4 < p.length) {
                    if (p[k].trim().isEmpty()) break;
                    var potion = LppUtil.potion(p[k]);
                    if (potion == null) {
                        LppUtil.warn(e, "unknown potion effect '" + p[k] + "'");
                    } else {
                        eff.potions.add(new LppEffects.PotionSpec(potion, LppUtil.i(p[k + 1]), LppUtil.i(p[k + 2]),
                                LppUtil.f(p[k + 3]), p[k + 4]));
                    }
                    k += 5;
                }
                if (eff.potions.isEmpty()) return;
            }
            List<Effect> list = target.computeIfAbsent(item, i -> new ArrayList<>());
            if (!command) for (Effect old : list) {
                if (old.meta == eff.meta && old.entry.text.equals(e.text)) return;
            }
            list.add(eff);
        });
    }

    static void postInit() {
        int n = 0;
        for (Map<Item, List<Effect>> m : List.of(HELD, INVENTORY, ARMOUR, DIGGING, HIT_ENTITY, HIT_SELF,
                CMD_HELD, CMD_INVENTORY, CMD_ARMOUR, CMD_DIGGING, CMD_HIT_ENTITY, CMD_HIT_SELF)) {
            for (List<Effect> l : m.values()) n += l.size();
        }
        if (n > 0) LootPlusPlus.LOG.info("[Loot++] Loaded {} item effect definition(s)", n);
    }

    static void runCondition(String args, String senderName, World world, double x, double y, double z) {
        String rest = args.trim();
        if (rest.startsWith("lppeffect")) {
            rest = rest.substring("lppeffect".length()).trim();
        }
        if (!rest.isEmpty()) LppCommands.run(rest, senderName, world, x, y, z);
    }

    private static boolean matches(ItemStack stack, Effect e) {
        if (stack.isEmpty() || stack.getItem() != e.item) return false;
        if (e.meta >= 0 && stack.getMetadata() != e.meta) return false;
        if (e.tag == null) return true;
        NBTTagCompound have = stack.getTagCompound();
        if (have == null) return false;
        for (String k : e.tag.getKeySet()) {
            NBTBase a = have.getTag(k);
            NBTBase b = e.tag.getTag(k);
            if (a == null || !a.equals(b)) return false;
        }
        return true;
    }

    private static void potions(Effect e, EntityLivingBase target) {
        for (LppEffects.PotionSpec spec : e.potions) {
            try {
                spec.applyIfLucky(target, false);
            } catch (Throwable t) {
                LootPlusPlus.LOG.warn("[Loot++] Effect {} failed: {}", e.entry.where(), t.toString());
            }
        }
    }

    private static void command(Effect e, EntityPlayer player) {
        if (player.ticksExisted % 20 != 0) return;
        if (e.probability < player.getRNG().nextFloat()) return;
        LppCommands.runAs(player, e.command);
    }

    private static void apply(Map<Item, List<Effect>> map, ItemStack stack, EntityPlayer player, EntityLivingBase target) {
        if (map.isEmpty()) return;
        List<Effect> list = map.get(stack.getItem());
        if (list == null) return;
        for (Effect e : list) {
            if (matches(stack, e)) potions(e, target == null ? player : target);
        }
    }

    private static void applyCommands(Map<Item, List<Effect>> map, ItemStack stack, EntityPlayer player) {
        if (map.isEmpty()) return;
        List<Effect> list = map.get(stack.getItem());
        if (list == null) return;
        for (Effect e : list) {
            if (matches(stack, e)) command(e, player);
        }
    }

    private static void runCommands(Map<Item, List<Effect>> map, ItemStack stack, EntityPlayer player) {
        List<Effect> list = map.get(stack.getItem());
        if (list == null) return;
        for (Effect e : list) {
            if (!matches(stack, e)) continue;
            if (e.probability < player.getRNG().nextFloat()) continue;
            LppCommands.runAs(player, e.command);
        }
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent ev) {
        if (ev.phase != TickEvent.Phase.END) return;
        EntityPlayer p = ev.player;
        if (p == null || p.world == null || p.world.isRemote) return;
        try {
            ItemStack main = p.getHeldItemMainhand();
            ItemStack off = p.getHeldItemOffhand();
            apply(HELD, main, p, null);
            apply(HELD, off, p, null);
            applyCommands(CMD_HELD, main, p);
            applyCommands(CMD_HELD, off, p);

            for (EntityEquipmentSlot slot : EntityEquipmentSlot.values()) {
                if (slot.getSlotType() != EntityEquipmentSlot.Type.ARMOR) continue;
                ItemStack st = p.getItemStackFromSlot(slot);
                apply(ARMOUR, st, p, null);
                applyCommands(CMD_ARMOUR, st, p);
            }

            ItemStack[] inv = p.inventory.mainInventory.toArray(new ItemStack[0]);
            for (ItemStack st : inv) {
                if (st.isEmpty()) continue;
                apply(INVENTORY, st, p, null);
                applyCommands(CMD_INVENTORY, st, p);
            }
            for (ItemStack st : p.inventory.armorInventory) {
                apply(INVENTORY, st, p, null);
                applyCommands(CMD_INVENTORY, st, p);
            }
            apply(INVENTORY, off, p, null);
            applyCommands(CMD_INVENTORY, off, p);
        } catch (Throwable t) {
            LootPlusPlus.LOG.warn("[Loot++] Player item effects failed: {}", t.toString());
        }
    }

    @SubscribeEvent
    public void onHurt(LivingHurtEvent ev) {
        try {
            EntityLivingBase target = ev.getEntityLiving();
            if (target == null || target.world == null || target.world.isRemote) return;
            if (!(ev.getSource().getTrueSource() instanceof EntityPlayer)) return;
            EntityPlayer p = (EntityPlayer) ev.getSource().getTrueSource();
            for (net.minecraft.util.EnumHand hand : net.minecraft.util.EnumHand.values()) {
                ItemStack st = p.getHeldItem(hand);
                if (st.isEmpty()) continue;
                List<Effect> list = HIT_ENTITY.get(st.getItem());
                if (list != null) for (Effect e : list) if (matches(st, e)) potions(e, target);
                List<Effect> self = HIT_SELF.get(st.getItem());
                if (self != null) for (Effect e : self) if (matches(st, e)) potions(e, p);
                runCommands(CMD_HIT_ENTITY, st, p);
                runCommands(CMD_HIT_SELF, st, p);
            }
        } catch (Throwable t) {
            LootPlusPlus.LOG.warn("[Loot++] Hit item effects failed: {}", t.toString());
        }
    }

    @SubscribeEvent
    public void onBreak(BlockEvent.BreakEvent ev) {
        try {
            EntityPlayer p = ev.getPlayer();
            if (p == null || p.world == null || p.world.isRemote) return;
            ItemStack st = p.getHeldItemMainhand();
            if (st.isEmpty()) return;
            List<Effect> list = DIGGING.get(st.getItem());
            if (list != null) for (Effect e : list) if (matches(st, e)) potions(e, p);
            runCommands(CMD_DIGGING, st, p);
        } catch (Throwable t) {
            LootPlusPlus.LOG.warn("[Loot++] Digging item effects failed: {}", t.toString());
        }
    }

    @SuppressWarnings("unused")
    private static BlockPos pos(EntityPlayer p) {
        return p.getPosition();
    }
}
