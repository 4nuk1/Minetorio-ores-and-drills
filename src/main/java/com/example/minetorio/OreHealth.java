package com.example.minetorio;

import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.vehicle.MinecartTNT;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Прочность руд из пластов. В центре пласта ~2000, к краю падает до ~100.
 * Каждая добыча киркой или цикл бура снимает 1. Когда прочность кончается, блок исчезает по-настоящему.
 * Блоки, поставленные игроком, в данных чанка отсутствуют и ведут себя как обычные.
 * Динамит (TNT) разрушает такие блоки сразу, остальные взрывы (крипер и т.п.) - нет.
 */
@EventBusSubscriber(modid = Minetorio.MOD_ID)
public final class OreHealth {
    public static final int MAX_HEALTH = 2000;
    public static final int MIN_HEALTH = 100;

    private record Restore(ServerLevel level, BlockPos pos, BlockState state) {}
    private static final List<Restore> PENDING = new ArrayList<>();

    public static boolean isTracked(ServerLevel level, BlockPos pos) {
        LevelChunk c = level.getChunkAt(pos);
        return c.hasData(ModRegistries.ORE_HEALTH.get())
                && c.getData(ModRegistries.ORE_HEALTH.get()).containsKey(pos.asLong());
    }

    /** Текущая прочность блока руды (0, если блок не из пласта). */
    public static int get(ServerLevel level, BlockPos pos) {
        LevelChunk c = level.getChunkAt(pos);
        if (!c.hasData(ModRegistries.ORE_HEALTH.get())) return 0;
        return c.getData(ModRegistries.ORE_HEALTH.get()).get(pos.asLong());
    }

    /** Снимает 1 прочность. Возвращает остаток (0 - запись удалена, блок пора убирать); -1 - блок не отслеживается. */
    public static int hit(ServerLevel level, BlockPos pos) {
        LevelChunk c = level.getChunkAt(pos);
        if (!c.hasData(ModRegistries.ORE_HEALTH.get())) return -1;
        Long2IntOpenHashMap map = c.getData(ModRegistries.ORE_HEALTH.get());
        long key = pos.asLong();
        if (!map.containsKey(key)) return -1;
        int left = map.get(key) - 1;
        if (left <= 0) { map.remove(key); left = 0; } else map.put(key, left);
        c.setUnsaved(true);
        return left;
    }

    private static void forget(ServerLevel level, BlockPos pos) {
        LevelChunk c = level.getChunkAt(pos);
        if (!c.hasData(ModRegistries.ORE_HEALTH.get())) return;
        if (c.getData(ModRegistries.ORE_HEALTH.get()).remove(pos.asLong()) != 0) c.setUnsaved(true);
    }

    @SubscribeEvent
    public static void onBreak(BlockEvent.BreakEvent e) {
        if (!(e.getLevel() instanceof ServerLevel level)) return;
        int left = hit(level, e.getPos());
        // дроп и опыт игрок получает как обычно; если прочность осталась, блок вернётся на место в конце тика
        if (left > 0) PENDING.add(new Restore(level, e.getPos().immutable(), e.getState()));
    }

    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post e) {
        if (PENDING.isEmpty()) return;
        List<Restore> copy = new ArrayList<>(PENDING);
        PENDING.clear();
        for (Restore r : copy) {
            if (r.level().getBlockState(r.pos()).isAir()) r.level().setBlock(r.pos(), r.state(), 3);
        }
    }

    @SubscribeEvent
    public static void onExplosion(ExplosionEvent.Detonate e) {
        if (!(e.getLevel() instanceof ServerLevel level)) return;
        var source = e.getExplosion().getDirectSourceEntity();
        boolean tnt = source instanceof PrimedTnt || source instanceof MinecartTNT;
        if (tnt) {
            for (BlockPos p : e.getAffectedBlocks()) forget(level, p); // блок разрушится - запись прочности не нужна
        } else {
            e.getAffectedBlocks().removeIf(p -> isTracked(level, p)); // крипер и прочее руду не ломают
        }
    }

    @SubscribeEvent
    public static void onStarting(ServerAboutToStartEvent e) { OreCatalog.clear(); }

    @SubscribeEvent
    public static void onStopped(ServerStoppedEvent e) { OreCatalog.clear(); PENDING.clear(); }

    private OreHealth() {}
}
