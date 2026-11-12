package com.electrifiedded.luckylegacyaddon.loot;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.init.MobEffects;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class LppEffects {
    private LppEffects() {}

    private static final Random RAND = new Random();

    public static final class PotionSpec {
        public final Potion potion;
        public final int duration;
        public final int amplifier;
        public final float probability;
        public final String particles;

        PotionSpec(Potion p, int d, int a, float prob, String particles) {
            this.potion = p;
            this.duration = d;
            this.amplifier = a;
            this.probability = prob;
            this.particles = particles;
        }

        public void applyIfLucky(EntityLivingBase e, boolean force) {
            if (probability >= RAND.nextFloat()) apply(e, force);
        }

        public void apply(EntityLivingBase e, boolean force) {
            PotionEffect existing = e.getActivePotionEffect(potion);
            PotionEffect eff = new PotionEffect(potion, duration, amplifier,
                    particles.equalsIgnoreCase("faded"), !particles.equalsIgnoreCase("none"));
            if (existing != null && existing.getAmplifier() >= eff.getAmplifier() && potion != MobEffects.NIGHT_VISION && !force) {
                if (existing.getDuration() <= 1) existing.combine(eff);
            } else {
                e.addPotionEffect(eff);
            }
        }
    }

    public static PotionSpec parse(String s) {
        String[] p = s.split("-");
        if (p.length != 5) throw LppUtil.skip("effect '" + s + "' needs 5 parts");
        Potion potion = LppUtil.potion(p[0]);
        if (potion == null) throw LppUtil.skip("unknown potion effect '" + p[0] + "'");
        return new PotionSpec(potion, LppUtil.i(p[1]), LppUtil.i(p[2]), LppUtil.f(p[3]), p[4]);
    }

    public static List<PotionSpec> parseAll(LppConfig.Entry e, String[] parts, int from) {
        List<PotionSpec> out = new ArrayList<>();
        for (int k = from; k < parts.length; k++) {
            if (parts[k].isEmpty()) continue;
            try {
                out.add(parse(parts[k]));
            } catch (Throwable t) {
                LppUtil.warn(e, t.getMessage());
            }
        }
        return out;
    }
}
