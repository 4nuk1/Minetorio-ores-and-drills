package com.example.orebore;

import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;

@Mod(OreBore.MOD_ID)
public class OreBore {
    public static final String MOD_ID = "orebore";

    public OreBore(IEventBus modBus) {
        ModRegistries.register(modBus);
        modBus.addListener(this::addToCreativeTab);
    }

    private void addToCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            event.accept(ModRegistries.SUPPORT_BEAM);
            event.accept(ModRegistries.DRILL_HEAD);
            event.accept(ModRegistries.DRILL_PLATFORM);
            event.accept(ModRegistries.CONTROLLER_3X3);
            event.accept(ModRegistries.CONTROLLER_5X5);
        }
    }
}
