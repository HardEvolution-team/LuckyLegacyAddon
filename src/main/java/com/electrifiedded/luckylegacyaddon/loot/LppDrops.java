package com.electrifiedded.luckylegacyaddon.loot;

import com.electrifiedded.archaiclib.legacy.entity.LegacyEntityList;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.registry.EntityEntry;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class LppDrops {
    private LppDrops() {}

    public abstract static class Part {
        public int weight = 1;
    }

    public static final class ItemPart extends Part {
        public final ItemStack stack;
        public final int min, max;

        ItemPart(ItemStack s, int min, int max, int weight) {
            this.stack = s;
            this.min = min;
            this.max = max;
            this.weight = weight;
        }
    }

    public static final class EntityPart extends Part {
        public final NBTTagCompound tag;

        EntityPart(NBTTagCompound tag, int weight) {
            this.tag = tag;
            this.weight = weight;
        }
    }

    public static final class CommandPart extends Part {
        public final String command;

        CommandPart(String c, int weight) {
            this.command = c;
            this.weight = weight;
        }
    }

    public static final class DropList {
        public final List<List<Part>> groups = new ArrayList<>();

        public boolean isEmpty() {
            return groups.isEmpty();
        }

        public boolean addField(LppConfig.Entry e, String field) {
            if (field == null || field.isEmpty()) return false;
            List<Part> group = new ArrayList<>();
            for (String sub : field.split("%%%%%")) {
                int dash = sub.indexOf('-');
                if (dash < 1) continue;
                try {
                    Part p = parse(sub.charAt(0), sub.substring(dash + 1));
                    if (p != null) group.add(p);
                } catch (Throwable t) {
                    LppUtil.warn(e, "bad drop '" + shorten(sub) + "': " + t.getMessage());
                }
            }
            if (group.isEmpty()) return false;
            groups.add(group);
            return true;
        }

        @Nullable
        public List<Part> pick(Random rand) {
            if (groups.isEmpty()) return null;
            int total = 0;
            for (List<Part> g : groups) total += g.get(0).weight;
            if (total <= 0) total = 1;
            int r = rand.nextInt(total) + 1;
            int acc = 0;
            for (List<Part> g : groups) {
                acc += g.get(0).weight;
                if (acc >= r) return g;
            }
            return groups.get(0);
        }

        public void run(World world, double x, double y, double z, String senderName, int extra, Random rand) {
            List<Part> g = pick(rand);
            if (g == null || world == null || world.isRemote) return;
            for (Part p : g) {
                try {
                    perform(p, world, x, y, z, senderName, extra, rand);
                } catch (Throwable t) {
                    LootPlusPlus.LOG.warn("[Loot++] Drop failed: {}", t.toString());
                }
            }
        }
    }

    private static String shorten(String s) {
        return s.length() > 60 ? s.substring(0, 57) + "..." : s;
    }

    @Nullable
    public static Part parse(char type, String s) throws Exception {
        switch (type) {
            case 'c': {
                String[] p = s.split("-", 2);
                if (p.length < 2) return null;
                int w = Math.max(1, LppUtil.i(p[0]));
                return new CommandPart(p[1], w);
            }
            case 'e': {
                String[] p = s.split("-", 3);
                String name = p[0];
                int w = p.length > 1 ? Math.max(1, LppUtil.i(p[1])) : 1;
                NBTTagCompound tag = p.length > 2 ? LppUtil.nbt(p[2]) : null;
                if (tag == null) tag = new NBTTagCompound();
                EntityEntry en = name.indexOf(':') > 0 ? null : LegacyEntityList.getEntryByName(name);
                tag.setString("id", en != null && en.getRegistryName() != null ? en.getRegistryName().toString()
                        : (name.indexOf(':') > 0 ? name : "minecraft:" + name.toLowerCase(java.util.Locale.ROOT)));
                return new EntityPart(tag, w);
            }
            case 'i': {
                String[] p = s.split("-", 6);
                if (p.length < 2) return null;
                Item item = LppUtil.item(p[0]);
                if (item == null) throw LppUtil.skip("unknown item '" + p[0] + "'");
                int min = Math.max(0, LppUtil.i(p[1]));
                int max = p.length > 2 ? LppUtil.i(p[2]) : min;
                if (max < min) max = min;
                int w = p.length > 3 ? Math.max(1, LppUtil.i(p[3])) : 1;
                int meta = p.length > 4 ? Math.max(0, LppUtil.i(p[4])) : 0;
                NBTTagCompound tag = p.length > 5 ? LppUtil.nbt(p[5]) : null;
                return new ItemPart(LppUtil.stack(item, meta, tag), min, max, w);
            }
            default:
                return null;
        }
    }

    private static void perform(Part p, World world, double x, double y, double z, String sender, int extra, Random rand) {
        if (p instanceof ItemPart) {
            ItemPart ip = (ItemPart) p;
            int n = ip.min + (ip.max > ip.min ? rand.nextInt(ip.max - ip.min + 1) : 0);
            if (extra > 0) n += rand.nextInt(extra + 1);
            if (n <= 0) return;
            ItemStack st = ip.stack.copy();
            st.setCount(Math.min(n, 64 * 4));
            EntityItem ei = new EntityItem(world, x, y, z, st);
            ei.setDefaultPickupDelay();
            world.spawnEntity(ei);
        } else if (p instanceof EntityPart) {
            EntityPart ep = (EntityPart) p;
            int count = 1 + (extra > 0 ? rand.nextInt(extra + 1) : 0);
            for (int k = 0; k < count; k++) spawnEntity(ep.tag, world, x, y, z, rand);
        } else if (p instanceof CommandPart) {
            int count = 1 + (extra > 0 ? rand.nextInt(extra + 1) : 0);
            for (int k = 0; k < count; k++) LppCommands.run(((CommandPart) p).command, sender, world, x, y, z);
        }
    }

    private static void spawnEntity(NBTTagCompound tag, World world, double x, double y, double z, Random rand) {
        NBTTagCompound t = tag.copy();
        boolean bare = t.getKeySet().size() == 1;
        Entity e = EntityList.createEntityFromNBT(t, world);
        if (e == null) {
            Entity viaName = EntityList.createEntityByIDFromName(new net.minecraft.util.ResourceLocation(t.getString("id")), world);
            if (viaName == null) {
                LootPlusPlus.LOG.warn("[Loot++] Cannot spawn entity '{}'", t.getString("id"));
                return;
            }
            e = viaName;
            bare = true;
        }
        if (e instanceof EntityLiving && bare) {
            ((EntityLiving) e).onInitialSpawn(world.getDifficultyForLocation(new BlockPos(x, y, z)), null);
        }
        e.setLocationAndAngles(x + rand.nextDouble() * 0.1, y + rand.nextDouble() * 0.1, z + rand.nextDouble() * 0.1, e.rotationYaw, e.rotationPitch);
        world.spawnEntity(e);
        NBTTagCompound cur = tag;
        Entity below = e;
        while (below != null && cur.hasKey("Riding", 10)) {
            cur = cur.getCompoundTag("Riding");
            NBTTagCompound rt = cur.copy();
            Entity r = EntityList.createEntityFromNBT(rt, world);
            if (r == null) break;
            r.setLocationAndAngles(e.posX, e.posY, e.posZ, r.rotationYaw, r.rotationPitch);
            world.spawnEntity(r);
            below.startRiding(r, true);
            below = r;
        }
    }
}
