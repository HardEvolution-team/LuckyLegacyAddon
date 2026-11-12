package com.electrifiedded.luckylegacyaddon.compat;

import com.electrifiedded.luckylegacyaddon.LuckyLegacyAddon;
import mod.lucky.Lucky;
import mod.lucky.world.LuckyTickHandler;

import java.lang.reflect.Field;

public final class LuckyUpdateCheckFix {
    private LuckyUpdateCheckFix() {}

    public static void apply() {
        try {
            Lucky lucky = Lucky.getInstance();
            LuckyTickHandler handler = lucky == null ? null : lucky.getTickHandler();
            if (handler == null) {
                LuckyLegacyAddon.LOGGER.warn("Lucky tick handler not available, update-check fix skipped");
                return;
            }
            Field f = LuckyTickHandler.class.getDeclaredField("shownUpdateVersion");
            f.setAccessible(true);
            f.setBoolean(handler, false);
            LuckyLegacyAddon.LOGGER.info("Disabled Lucky Block's dead-link update check (prevents client freeze)");
        } catch (Throwable t) {
            LuckyLegacyAddon.LOGGER.warn("Could not disable Lucky Block update check", t);
        }
    }
}
