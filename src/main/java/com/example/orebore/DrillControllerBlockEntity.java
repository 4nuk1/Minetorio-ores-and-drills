package com.example.orebore;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.ItemStackHandler;

import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import net.minecraft.world.phys.AABB;

import java.util.List;

public class DrillControllerBlockEntity extends KineticBlockEntity {
    public static final int BASE_RPM = 32;       // 1 ступень = 32 RPM
    public static final int MAX_STEP = 8;        // до 256 RPM
    public static final int CYCLE_TICKS = 6000;  // 5 минут при 1 ступени

    private final ItemStackHandler buffer = new ItemStackHandler(2); // 0 - блок руды, 1 - ед. руды
    private int progress;            // целое число "тиков прогресса"
    private boolean structureOk;
    private Block ore;               // руда под буром (null - нет)

    public DrillControllerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    private DrillTier tier() {
        return ((DrillControllerBlock) getBlockState().getBlock()).tier;
    }

    /** Потребление: impact * RPM = 1024/1536 su на 32 RPM, дальше строго кратно. */
    @Override
    public float calculateStressApplied() {
        float impact = tier().impactPerRpm;
        this.lastStressApplied = impact;
        return impact;
    }

    /** Целая ступень скорости: 0..8. */
    public int speedStep() {
        return Math.min((int) (Math.abs(getSpeed()) / BASE_RPM), MAX_STEP);
    }

    @Override
    public void tick() {
        super.tick();
        if (level == null || level.isClientSide) return;

        if (level.getGameTime() % 10 == 0) revalidate();
        if (level.getGameTime() % 8 == 0) pushOutput();

        int step = speedStep();
        if (!structureOk || ore == null || step == 0) return;

        DrillTier t = tier();
        ItemStack blockOut = new ItemStack(ore.asItem(), t.oreBlocksPerCycle);
        ItemStack rawOut = rawItemFor(ore, t.rawPerCycle);

        if (progress + step >= CYCLE_TICKS) {
            // не забираем прогресс, если буфер полон
            if (!canFit(blockOut, rawOut)) return;
            progress = progress + step - CYCLE_TICKS;
            buffer.insertItem(0, blockOut, false);
            buffer.insertItem(1, rawOut, false);
        } else {
            progress += step;
        }
        setChanged();
    }

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

    /** Проверка каркаса: 4 угловые колонны support_beam. */
    private boolean checkFrame() {
        DrillTier t = tier();
        BlockPos top = worldPosition;
        int baseY = top.getY() - (t.height - 1);
        int r = t.platformHalf;
        for (int sx = -1; sx <= 1; sx += 2) {
            for (int sz = -1; sz <= 1; sz += 2) {
                for (int y = baseY; y <= top.getY(); y++) {
                    BlockPos p = new BlockPos(top.getX() + sx * r, y, top.getZ() + sz * r);
                    if (!level.getBlockState(p).is(ModRegistries.SUPPORT_BEAM.get())) return false;
                }
            }
        }
        return true;
    }

    /**
     * Ищет внутри каркаса контрапцию Create (вращающаяся головка на подшипнике), в составе которой есть
     * блоки drill_head, и возвращает руду прямо под её нижней границей (до 2 блоков зазора).
     * Головка не ломает блоки: поршень сам остановит платформу у поверхности пласта.
     */
    private Block detectOreUnderHead() {
        DrillTier t = tier();
        int r = t.platformHalf;
        int baseY = worldPosition.getY() - (t.height - 1);
        AABB zone = new AABB(worldPosition.getX() - r, baseY - 2, worldPosition.getZ() - r,
                worldPosition.getX() + r + 1, worldPosition.getY() + 1, worldPosition.getZ() + r + 1);
        for (AbstractContraptionEntity e : level.getEntitiesOfClass(AbstractContraptionEntity.class, zone)) {
            if (e.getContraption() == null) continue;
            List<BlockPos> heads = e.getContraption().getBlocks().entrySet().stream()
                    .filter(en -> en.getValue().state().is(ModRegistries.DRILL_HEAD.get()))
                    .map(java.util.Map.Entry::getKey).toList();
            // добыча идёт только если головка собрана строго по форме нужного бура
            if (!DrillShapes.matches(heads, t)) continue;
            int y0 = (int) Math.floor(e.getBoundingBox().minY + 0.01);
            for (int d = 1; d <= 2; d++) {
                BlockState s = level.getBlockState(new BlockPos(worldPosition.getX(), y0 - d, worldPosition.getZ()));
                if (s.isAir()) continue;
                return s.is(Tags.Blocks.ORES) ? s.getBlock() : null;
            }
        }
        return null;
    }

    private void revalidate() {
        structureOk = checkFrame();
        Block found = structureOk ? detectOreUnderHead() : null;
        // Если головка временно не нашлась (поднимается/спускается) - просто пауза, прогресс не теряется.
        if (found != null && ore != null && found != ore) progress = 0;
        ore = found;
        setChanged();
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
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        progress = tag.getInt("Progress");
        buffer.deserializeNBT(registries, tag.getCompound("Buffer"));
    }
}
