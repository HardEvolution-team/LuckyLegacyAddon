package com.electrifiedded.luckylegacyaddon.mixin;

import com.electrifiedded.luckylegacyaddon.convert.NativeAddons;
import mod.lucky.resources.BaseResource;
import mod.lucky.resources.loader.PluginLoader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;

/** Serves legacy add-on files from memory (fixed on the fly); originals are never rewritten. */
@Mixin(value = PluginLoader.class, remap = false)
public abstract class PluginLoaderMixin {
    @Shadow private File pluginFile;

    @Inject(method = "getResourceStream", at = @At("HEAD"), cancellable = true)
    private void lla$stream(BaseResource r, CallbackInfoReturnable<InputStream> cir) {
        NativeAddons.Addon a = NativeAddons.byKey(pluginFile);
        if (a == null) return;
        byte[] b = a.files.get(r.getDirectory());
        // directory resource ("structures") or a standard config file the 1.8 add-on simply doesn't have
        if (b == null && (a.has(r.getDirectory()) || r.getDirectory().endsWith(".txt") || r.getDirectory().equals("structures"))) b = new byte[0];
        if (b == null && !r.isOptional())
            System.out.println("Lucky Block: Error getting resource '" + r.getDirectory() + "' from plugin '" + a.name + "'");
        cir.setReturnValue(b == null ? null : new ByteArrayInputStream(b));
    }

    @Inject(method = "hasResource", at = @At("HEAD"), cancellable = true)
    private void lla$has(BaseResource r, CallbackInfoReturnable<Boolean> cir) {
        NativeAddons.Addon a = NativeAddons.byKey(pluginFile);
        if (a != null) cir.setReturnValue(a.has(r.getDirectory()));
    }

    @Inject(method = "getResourcePack", at = @At("HEAD"), cancellable = true)
    private void lla$pack(CallbackInfoReturnable<Object> cir) {
        NativeAddons.Addon a = NativeAddons.byKey(pluginFile);
        if (a != null) cir.setReturnValue(com.electrifiedded.luckylegacyaddon.compat.MemoryResourcePack.of(a));
    }
}
