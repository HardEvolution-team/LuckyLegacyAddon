package com.electrifiedded.luckylegacyaddon.loot;

import com.electrifiedded.archaiclib.legacy.entity.LegacyEntityList;
import com.electrifiedded.archaiclib.legacy.sound.LegacySounds;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import net.minecraft.util.SoundEvent;
import net.minecraft.entity.EntityList;
import net.minecraftforge.fml.common.registry.EntityEntry;

import javax.annotation.Nullable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class LppCommands {
    private LppCommands() {}

    public static final class Sender implements ICommandSender {
        private final String name;
        private final World world;
        private final Vec3d vec;
        private final BlockPos pos;

        public Sender(String name, World world, double x, double y, double z) {
            this.name = name == null ? "Loot++" : name;
            this.world = world;
            this.vec = new Vec3d(x, y, z);
            this.pos = new BlockPos(MathHelper.floor(x), MathHelper.floor(y), MathHelper.floor(z));
        }

        @Override public String getName() { return name; }
        @Override public ITextComponent getDisplayName() { return new TextComponentString(name); }
        @Override public boolean canUseCommand(int permLevel, String commandName) { return true; }
        @Override public World getEntityWorld() { return world; }
        @Override public MinecraftServer getServer() { return world.getMinecraftServer(); }
        @Override public BlockPos getPosition() { return pos; }
        @Override public Vec3d getPositionVector() { return vec; }
        @Nullable @Override public Entity getCommandSenderEntity() { return null; }
        @Override public boolean sendCommandFeedback() { return false; }
        @Override public void sendMessage(ITextComponent c) { }
    }

    private static final Pattern SUMMON = Pattern.compile("^(summon\\s+)(\\S+)(.*)$", Pattern.DOTALL);
    private static final Pattern PLAYSOUND = Pattern.compile("^(playsound\\s+)(\\S+)\\s+(@\\S+)(.*)$", Pattern.DOTALL);
    private static final Pattern LOWER_EXEC = Pattern.compile("^(execute\\s+\\S+\\s+\\S+\\s+\\S+\\s+\\S+\\s+\\S+\\s+)(.*)$", Pattern.DOTALL);

    public static String fix(String cmd) {
        String c = cmd.trim();
        if (c.startsWith("/")) c = c.substring(1);
        Matcher m = SUMMON.matcher(c);
        if (m.matches()) {
            String id = m.group(2);
            if (id.equalsIgnoreCase("LightningBolt")) {
                id = "lightning_bolt";
            } else if (id.indexOf(':') < 0) {
                EntityEntry e = LegacyEntityList.getEntryByName(id);
                if (e != null && e.getRegistryName() != null) id = e.getRegistryName().toString();
            }
            return m.group(1) + id + m.group(3);
        }
        m = PLAYSOUND.matcher(c);
        if (m.matches()) {
            SoundEvent ev = LegacySounds.get(m.group(2));
            String sound = ev != null && ev.getRegistryName() != null ? ev.getRegistryName().toString() : null;
            if (sound == null) sound = com.electrifiedded.luckylegacyaddon.convert.fixers.value.Tables.sound(m.group(2));
            if (sound == null) sound = m.group(2);
            return m.group(1) + sound + " master " + m.group(3) + m.group(4);
        }
        m = LOWER_EXEC.matcher(c);
        if (m.matches() && !m.group(2).isEmpty()) {
            return m.group(1) + fix(m.group(2));
        }
        return c;
    }

    public static void runAs(EntityPlayer player, String command) {
        if (player == null || player.world == null || player.world.isRemote) return;
        MinecraftServer server = player.world.getMinecraftServer();
        if (server == null) return;
        try {
            server.getCommandManager().executeCommand(player, fix(command));
        } catch (Throwable t) {
            LootPlusPlus.LOG.warn("[Loot++] Command '{}' failed: {}", command, t.toString());
        }
    }

    public static void run(String command, String senderName, World world, double x, double y, double z) {
        if (world == null || world.isRemote) return;
        MinecraftServer server = world.getMinecraftServer();
        if (server == null) return;
        try {
            String c = fix(command);
            if (c.startsWith("lppcondition")) {
                LppExtras.runCondition(c.substring("lppcondition".length()).trim(), senderName, world, x, y, z);
                return;
            }
            server.getCommandManager().executeCommand(new Sender(senderName, world, x, y, z), c);
        } catch (Throwable t) {
            LootPlusPlus.LOG.warn("[Loot++] Command '{}' failed: {}", command, t.toString());
        }
    }

    @SuppressWarnings("unused")
    private static String entityName(Entity e) {
        return EntityList.getEntityString(e);
    }
}
