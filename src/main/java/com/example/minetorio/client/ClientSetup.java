package com.example.minetorio.client;

import com.example.minetorio.Minetorio;
import com.example.minetorio.ModRegistries;
import com.example.minetorio.client.ponder.MinetorioPonderPlugin;
import net.createmod.ponder.foundation.PonderIndex;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/** Только клиент: сюда попадают классы Ponder и рендера. */
@EventBusSubscriber(modid = Minetorio.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientSetup {
    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        PonderIndex.addPlugin(new MinetorioPonderPlugin());
    }

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModRegistries.DRILL_BIT.get(), DrillBitRenderer::new);
    }

    private ClientSetup() {}
}
