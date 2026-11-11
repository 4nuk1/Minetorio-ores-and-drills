package com.example.minetorio.client;

import com.example.minetorio.DrillControllerBlockEntity;
import com.example.minetorio.DrillHeadBaseBlockEntity;
import com.example.minetorio.Minetorio;
import com.example.minetorio.ModRegistries;
import com.example.minetorio.client.ponder.MinetorioPonderPlugin;
import net.createmod.ponder.foundation.PonderIndex;
import net.minecraft.core.Direction;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

import java.util.List;

/** Только клиент: сюда попадают классы Ponder и рендера. */
@EventBusSubscriber(modid = Minetorio.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientSetup {
    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        PonderIndex.addPlugin(new MinetorioPonderPlugin());
    }

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModRegistries.DRILL_ROD.get(), DrillRodRenderer::new);
        event.registerEntityRenderer(ModRegistries.DRILL_BIT.get(), DrillBitRenderer::new);
        // привод: валы со всех сторон, кроме выхода руды
        event.registerBlockEntityRenderer(ModRegistries.CONTROLLER_BE.get(),
                ctx -> new ShaftStubRenderer<DrillControllerBlockEntity>(DrillControllerBlockEntity::shaftFaces));
        // основание: вал только сверху
        event.registerBlockEntityRenderer(ModRegistries.BASE_BE.get(),
                ctx -> new ShaftStubRenderer<DrillHeadBaseBlockEntity>(be -> List.of(Direction.UP)));
    }

    private ClientSetup() {}
}
