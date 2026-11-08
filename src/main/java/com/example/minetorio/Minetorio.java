package com.example.minetorio;

import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;

@Mod(Minetorio.MOD_ID)
public class Minetorio {
    public static final String MOD_ID = "minetorio";

    public Minetorio(IEventBus modBus) {
        ModRegistries.register(modBus);
        modBus.addListener(this::addToCreativeTab);
    }

    private void addToCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            event.accept(ModRegistries.DRILL_HEAD);
            event.accept(ModRegistries.DRILL_PLATFORM);
            event.accept(ModRegistries.CONTROLLER_3X3);
            event.accept(ModRegistries.CONTROLLER_5X5);
        }
    }
}
