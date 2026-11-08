package com.example.minetorio;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Бесконечные руды: блок руды, созданный генерацией пластов, не исчезает после добычи.
 * Блоки, поставленные игроком, в списке чанка отсутствуют и ведут себя как обычные.
 */
@EventBusSubscriber(modid = Minetorio.MOD_ID)
public final class InfiniteOres {
    private record Restore(ServerLevel level, BlockPos pos, BlockState state) {}
    private static final List<Restore> PENDING = new ArrayList<>();

    public static boolean isInfinite(ServerLevel level, BlockPos pos) {
        LevelChunk chunk = level.getChunkAt(pos);
        if (!chunk.hasData(ModRegistries.INFINITE_ORES.get())) return false;
        return chunk.getData(ModRegistries.INFINITE_ORES.get()).contains(pos.asLong());
    }

    @SubscribeEvent
    public static void onBreak(BlockEvent.BreakEvent e) {
        if (!(e.getLevel() instanceof ServerLevel level)) return;
        if (isInfinite(level, e.getPos())) {
            PENDING.add(new Restore(level, e.getPos().immutable(), e.getState()));
        }
    }

    /** Дроп и опыт игрок получает как обычно; в конце тика блок возвращается на место. */
    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post e) {
        if (PENDING.isEmpty()) return;
        List<Restore> copy = new ArrayList<>(PENDING);
        PENDING.clear();
        for (Restore r : copy) {
            if (r.level().getBlockState(r.pos()).isAir()) {
                r.level().setBlock(r.pos(), r.state(), 3);
            }
        }
    }

    @SubscribeEvent
    public static void onExplosion(ExplosionEvent.Detonate e) {
        if (e.getLevel() instanceof ServerLevel level) {
            e.getAffectedBlocks().removeIf(p -> isInfinite(level, p));
        }
    }

    @SubscribeEvent
    public static void onStarting(ServerAboutToStartEvent e) { OreCatalog.clear(); }

    @SubscribeEvent
    public static void onStopped(ServerStoppedEvent e) { OreCatalog.clear(); PENDING.clear(); }

    private InfiniteOres() {}
}
