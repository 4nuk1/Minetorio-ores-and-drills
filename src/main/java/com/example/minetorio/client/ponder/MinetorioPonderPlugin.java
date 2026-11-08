package com.example.minetorio.client.ponder;

import com.example.minetorio.Minetorio;
import net.createmod.ponder.api.registration.PonderPlugin;
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.minecraft.resources.ResourceLocation;

/** Привязывает размышления к блокам мода: зажми W (по умолчанию), наведя на блок в инвентаре или в JEI. */
public class MinetorioPonderPlugin implements PonderPlugin {
    @Override
    public String getModId() {
        return Minetorio.MOD_ID;
    }

    @Override
    public void registerScenes(PonderSceneRegistrationHelper<ResourceLocation> helper) {
        ResourceLocation head = id("drill_head");
        ResourceLocation platform = id("drill_platform");
        ResourceLocation ctrl3 = id("drill_controller_3x3");
        ResourceLocation ctrl5 = id("drill_controller_5x5");

        // головка показывает форму обеих версий и сборку
        for (ResourceLocation block : new ResourceLocation[]{head, platform}) {
            helper.addStoryBoard(block, "head_3x3", MinetorioScenes::head3x3);
            helper.addStoryBoard(block, "head_5x5", MinetorioScenes::head5x5);
            helper.addStoryBoard(block, "drill_3x3", MinetorioScenes::drill3x3);
            helper.addStoryBoard(block, "drill_5x5", MinetorioScenes::drill5x5);
        }
        helper.addStoryBoard(ctrl3, "drill_3x3", MinetorioScenes::drill3x3);
        helper.addStoryBoard(ctrl5, "drill_5x5", MinetorioScenes::drill5x5);
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(Minetorio.MOD_ID, path);
    }
}
