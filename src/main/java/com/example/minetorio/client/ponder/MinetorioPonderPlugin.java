package com.example.minetorio.client.ponder;

import com.example.minetorio.Minetorio;
import net.createmod.ponder.api.registration.PonderPlugin;
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.minecraft.resources.ResourceLocation;

/** Зажми W (по умолчанию), наведя на блок в инвентаре или в JEI. */
public class MinetorioPonderPlugin implements PonderPlugin {
    @Override
    public String getModId() {
        return Minetorio.MOD_ID;
    }

    @Override
    public void registerScenes(PonderSceneRegistrationHelper<ResourceLocation> helper) {
        for (String block : new String[]{"drill_head", "drill_head_base"}) {
            ResourceLocation id = id(block);
            helper.addStoryBoard(id, "head_3x3", MinetorioScenes::head3x3);
            helper.addStoryBoard(id, "head_5x5", MinetorioScenes::head5x5);
            helper.addStoryBoard(id, "drill_3x3", MinetorioScenes::drill3x3);
            helper.addStoryBoard(id, "drill_5x5", MinetorioScenes::drill5x5);
        }
        helper.addStoryBoard(id("drill_drive_3x3"), "drill_3x3", MinetorioScenes::drill3x3);
        helper.addStoryBoard(id("drill_drive_5x5"), "drill_5x5", MinetorioScenes::drill5x5);
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(Minetorio.MOD_ID, path);
    }
}
