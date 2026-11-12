package com.electrifiedded.luckylegacyaddon.mixin;

import com.electrifiedded.luckylegacyaddon.convert.NativeAddons;
import mod.lucky.resources.loader.PluginLoader;
import mod.lucky.resources.loader.ResourceLoader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;

/** Replaces Lucky's on-disk plugin loaders for legacy add-ons with loaders backed by NativeAddons. */
@Mixin(value = ResourceLoader.class, remap = false)
public abstract class ResourceLoaderMixin {
    @Shadow private ArrayList<PluginLoader> pluginLoaders;

    @Inject(method = "resetLoaders", at = @At("TAIL"))
    private void lla$nativeAddons(CallbackInfo ci) {
        pluginLoaders.removeIf(l -> NativeAddons.isOrigin(l.getPluginFile()));
        for (NativeAddons.Addon a : NativeAddons.all()) pluginLoaders.add(new PluginLoader(a.key));
    }
}
