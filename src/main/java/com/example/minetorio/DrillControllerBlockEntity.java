package com.example.minetorio;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.ItemStackHandler;

import java.util.ArrayList;
import java.util.List;

/**
 * Привод бура. Когда на него подано вращение, он находит свободное основание головки нужного размера
 * и закрепляется за ним (и наоборот) - пока один из блоков не разрушен. Добыча идёт, когда основание вращается,
 * головка стоит на блоке руды из пласта. Цикл всегда 1 минута; от скорости зависит выход за цикл.
 */
public class DrillControllerBlockEntity extends KineticBlockEntity {
    public static final int MIN_RPM = 16;        // ниже - бур не работает
    public static final int CYCLE_TICKS = 1200;  // ровно 1 минута на любой скорости
    private static final int SEARCH_RADIUS = 6;

    private final ItemStackHandler buffer = new ItemStackHandler(2); // 0 - блок руды, 1 - ед. руды
    private int progress;
    private BlockPos basePos;   // закреплённое основание
    private Block ore;
    private BlockPos orePos;

    public DrillControllerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    private DrillTier tier() {
        return ((DrillControllerBlock) getBlockState().getBlock()).tier;
    }

    public BlockPos pairedBase() { return basePos; }

    /** Грани, на которых рисуются валы: все, кроме выхода руды. */
    public List<Direction> shaftFaces() {
        List<Direction> out = new ArrayList<>(5);
        Direction front = getBlockState().getValue(DrillControllerBlock.FACING);
        for (Direction d : Direction.values()) if (d != front) out.add(d);
        return out;
    }

    /** Потребление: impact * RPM = 1024/1536 su на 16 RPM, дальше строго пропорционально. */
    @Override
    public float calculateStressApplied() {
        float impact = tier().impactPerRpm;
        this.lastStressApplied = impact;
        return impact;
    }

    /** Уровень скорости: 16 -> 1, 32 -> 2, 64 -> 3, 128 -> 4, 256 и выше -> 5; ниже 16 RPM - 0 (бур стоит). */
    public int speedLevel() {
        float rpm = Math.abs(getSpeed());
        if (rpm >= 256) return 5;
        if (rpm >= 128) return 4;
        if (rpm >= 64) return 3;
        if (rpm >= 32) return 2;
        if (rpm >= MIN_RPM) return 1;
        return 0;
    }

    @Override
    public void tick() {
        super.tick();
        if (level == null || level.isClientSide) return;
        long t = level.getGameTime();

        if (t % 20 == 0) validatePair();
        if (basePos == null && t % 10 == 0 && speedLevel() > 0) tryPair();
        if (t % 2 == 0) updateTarget();
        if (t % 8 == 0) pushOutput();

        int speed = speedLevel();
        if (ore == null || orePos == null || speed == 0) return;

        DrillTier tr = tier();
        if (progress + 1 >= CYCLE_TICKS) {
            ItemStack blockOut = new ItemStack(ore.asItem(), tr.oreBlocks(speed));
            ItemStack rawOut = rawItemFor(ore, tr.raw(speed));
            if (!canFit(blockOut, rawOut)) return;   // выход забит: бур стоит, без звука и частиц
            mineEffects(speed);
            progress = 0;
            buffer.insertItem(0, blockOut, false);
            buffer.insertItem(1, rawOut, false);
            // цикл стоит блоку руды 1 прочности; когда она кончается, блок исчезает
            if (OreHealth.hit((ServerLevel) level, orePos) == 0) {
                level.removeBlock(orePos, false);
                ore = null;
                orePos = null;
            }
        } else {
            progress++;
            mineEffects(speed);
        }
        setChanged();
    }

    /**
     * Звук и частицы под головкой, пока бур добывает: осколки ломаемого блока руды, дым и скрежет.
     * Чем выше уровень скорости, тем чаще звук и больше частиц.
     */
    private void mineEffects(int speed) {
        ServerLevel sl = (ServerLevel) level;
        long t = level.getGameTime();
        BlockState oreState = level.getBlockState(orePos);
        double spread = tier() == DrillTier.SMALL_3X3 ? 0.7 : 1.3;   // частицы вылетают из-под всей головки
        double x = orePos.getX() + 0.5, y = orePos.getY() + 1.05, z = orePos.getZ() + 0.5;
        if (t % 2 == 0) {
            sl.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, oreState), x, y, z, 2 + speed, spread, 0.05, spread, 0.15);
        }
        if (t % 4 == 0) {
            sl.sendParticles(ParticleTypes.SMOKE, x, y + 0.05, z, 1, spread * 0.8, 0.05, spread * 0.8, 0.01);
        }
        if (t % Math.max(3, 9 - speed) == 0) {
            sl.playSound(null, orePos, oreState.getSoundType().getHitSound(), SoundSource.BLOCKS, 0.6f, 0.7f + 0.06f * speed);
        }
        if (t % 20 == 0) {
            sl.playSound(null, orePos, SoundEvents.GRINDSTONE_USE, SoundSource.BLOCKS, 0.35f, 0.8f + 0.06f * speed);
        }
    }

    // ---------- связь с основанием ----------

    private void validatePair() {
        if (basePos == null || !level.isLoaded(basePos)) return;
        if (!(level.getBlockEntity(basePos) instanceof DrillHeadBaseBlockEntity b) || !worldPosition.equals(b.pairedDrive())) {
            basePos = null;
            setChanged();
        }
    }

    private void tryPair() {
        DrillTier t = tier();
        int vr = t.shapeHeight() + 4;
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int x = worldPosition.getX() - SEARCH_RADIUS; x <= worldPosition.getX() + SEARCH_RADIUS; x++) {
            for (int z = worldPosition.getZ() - SEARCH_RADIUS; z <= worldPosition.getZ() + SEARCH_RADIUS; z++) {
                for (int y = worldPosition.getY() - vr; y <= worldPosition.getY() + vr; y++) {
                    p.set(x, y, z);
                    if (!level.getBlockState(p).is(ModRegistries.DRILL_HEAD_BASE.get())) continue;
                    if (level.getBlockEntity(p) instanceof DrillHeadBaseBlockEntity b
                            && b.pairedDrive() == null && b.currentTier() == t) {
                        b.pair(worldPosition);
                        basePos = p.immutable();
                        setChanged();
                        return;
                    }
                }
            }
        }
    }

    private void updateTarget() {
        Block found = null;
        BlockPos foundPos = null;
        if (basePos != null && level.getBlockEntity(basePos) instanceof DrillHeadBaseBlockEntity b && b.isReadyFor(tier())) {
            BlockPos below = b.blockBelowTip();
            BlockState s = level.getBlockState(below);
            if (s.is(Tags.Blocks.ORES) && OreHealth.isTracked((ServerLevel) level, below)) {
                found = s.getBlock();
                foundPos = below;
            }
        }
        if (found != null && ore != null && found != ore) progress = 0;
        ore = found;
        orePos = foundPos;
    }

    // ---------- информация для игрока ----------

    /** ПКМ по приводу: сколько руды бур ещё может добыть из блока под головкой на текущей скорости. */
    public void sendInfo(Player player) {
        Component msg;
        if (basePos == null) {
            msg = Component.translatable("message.minetorio.not_paired");
        } else if (orePos == null) {
            msg = Component.translatable("message.minetorio.no_ore");
        } else if (speedLevel() == 0) {
            msg = Component.translatable("message.minetorio.too_slow", MIN_RPM);
        } else {
            int cycles = OreHealth.get((ServerLevel) level, orePos);
            DrillTier t = tier();
            int speed = speedLevel();
            msg = Component.translatable("message.minetorio.ore_left", cycles * t.oreBlocks(speed), cycles * t.raw(speed),
                    cycles, speed);
        }
        player.displayClientMessage(msg, true);
    }

    // ---------- добыча ----------

    private boolean canFit(ItemStack a, ItemStack b) {
        return buffer.insertItem(0, a, true).isEmpty() && buffer.insertItem(1, b, true).isEmpty();
    }

    private ItemStack rawItemFor(Block oreBlock, int amount) {
        if (level instanceof ServerLevel sl) {
            List<ItemStack> drops = Block.getDrops(oreBlock.defaultBlockState(), sl, worldPosition, null);
            for (ItemStack s : drops) {
                if (!s.isEmpty()) return new ItemStack(s.getItem(), amount);
            }
        }
        return new ItemStack(oreBlock.asItem(), amount);
    }

    private void pushOutput() {
        Direction out = getBlockState().getValue(DrillControllerBlock.FACING);
        IItemHandler target = level.getCapability(Capabilities.ItemHandler.BLOCK,
                worldPosition.relative(out), out.getOpposite());
        if (target == null) return;
        for (int i = 0; i < buffer.getSlots(); i++) {
            ItemStack stack = buffer.extractItem(i, 64, true);
            if (stack.isEmpty()) continue;
            ItemStack left = ItemHandlerHelper.insertItem(target, stack, false);
            buffer.extractItem(i, stack.getCount() - left.getCount(), false);
        }
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.putInt("Progress", progress);
        tag.put("Buffer", buffer.serializeNBT(registries));
        if (basePos != null) tag.putLong("Base", basePos.asLong());
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        progress = tag.getInt("Progress");
        buffer.deserializeNBT(registries, tag.getCompound("Buffer"));
        basePos = tag.contains("Base") ? BlockPos.of(tag.getLong("Base")) : null;
    }
}
