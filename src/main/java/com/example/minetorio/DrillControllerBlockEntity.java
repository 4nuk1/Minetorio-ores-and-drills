package com.example.minetorio;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
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
 * стержень выдвинут до конца, а блок руды под его концом взят из пласта.
 */
public class DrillControllerBlockEntity extends KineticBlockEntity {
    public static final int BASE_RPM = 32;       // 1 ступень = 32 RPM
    public static final int MAX_STEP = 8;        // до 256 RPM
    public static final int CYCLE_TICKS = 6000;  // 5 минут при 1 ступени
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

    /** Потребление: impact * RPM = 1024/1536 su на 32 RPM, дальше строго кратно. */
    @Override
    public float calculateStressApplied() {
        float impact = tier().impactPerRpm;
        this.lastStressApplied = impact;
        return impact;
    }

    public int speedStep() {
        return Math.min((int) (Math.abs(getSpeed()) / BASE_RPM), MAX_STEP);
    }

    @Override
    public void tick() {
        super.tick();
        if (level == null || level.isClientSide) return;
        long t = level.getGameTime();

        if (t % 20 == 0) validatePair();
        if (basePos == null && t % 10 == 0 && speedStep() > 0) tryPair();
        if (t % 2 == 0) updateTarget();
        if (t % 8 == 0) pushOutput();

        int step = speedStep();
        if (ore == null || orePos == null || step == 0) return;

        DrillTier tr = tier();
        if (progress + step >= CYCLE_TICKS) {
            ItemStack blockOut = new ItemStack(ore.asItem(), tr.oreBlocksPerCycle);
            ItemStack rawOut = rawItemFor(ore, tr.rawPerCycle);
            if (!canFit(blockOut, rawOut)) return;
            progress = progress + step - CYCLE_TICKS;
            buffer.insertItem(0, blockOut, false);
            buffer.insertItem(1, rawOut, false);
            // цикл стоит блоку руды 1 прочности; когда она кончается, блок исчезает
            if (OreHealth.hit((ServerLevel) level, orePos) == 0) {
                level.removeBlock(orePos, false);
                ore = null;
                orePos = null;
            }
        } else {
            progress += step;
        }
        setChanged();
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
                            && b.pairedDrive() == null && b.shapeTier() == t) {
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

    /** ПКМ по приводу: сколько руды бур ещё может добыть из блока под головкой. */
    public void sendInfo(Player player) {
        Component msg;
        if (basePos == null) {
            msg = Component.translatable("message.minetorio.not_paired");
        } else if (orePos == null) {
            msg = Component.translatable("message.minetorio.no_ore");
        } else {
            int cycles = OreHealth.get((ServerLevel) level, orePos);
            DrillTier t = tier();
            msg = Component.translatable("message.minetorio.ore_left", cycles * t.oreBlocksPerCycle, cycles * t.rawPerCycle, cycles);
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
