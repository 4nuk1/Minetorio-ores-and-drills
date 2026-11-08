package com.example.minetorio.client;

import com.example.minetorio.Minetorio;
import com.example.minetorio.client.ponder.MinetorioPonderPlugin;
import net.createmod.ponder.foundation.PonderIndex;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

/** Только клиент: сюда попадают классы Ponder, на выделенном сервере этот класс не загружается. */
@EventBusSubscriber(modid = Minetorio.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientSetup {
    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        PonderIndex.addPlugin(new MinetorioPonderPlugin());
    }

    private ClientSetup() {}
}
