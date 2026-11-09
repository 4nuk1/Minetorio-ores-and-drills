package com.example.minetorio;

import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.ItemStackHandler;

import java.util.List;
import java.util.UUID;

/**
 * Привод бура. Ищет рядом под собой сборку: "Основание головки" с колонной "Блоков головки" под ним
 * (в мире или внутри контрапции Create, которую двигает поршень). Вокруг нижних блоков колонны
 * приводом создаётся вращающаяся головка (DrillBitEntity). Добыча идёт, когда сборка стоит на месте,
 * а блок руды прямо под нижним блоком колонны взят из пласта (имеет прочность).
 */
public class DrillControllerBlockEntity extends KineticBlockEntity {
    public static final int BASE_RPM = 32;       // 1 ступень = 32 RPM
    public static final int MAX_STEP = 8;        // до 256 RPM
    public static final int CYCLE_TICKS = 6000;  // 5 минут при 1 ступени

    /** tipPos == null - сборка движется внутри контрапции. */
    private record Assembly(Vec3 tipCenter, BlockPos tipPos) {}

    private final ItemStackHandler buffer = new ItemStackHandler(2); // 0 - блок руды, 1 - ед. руды
    private int progress;
    private Block ore;
    private BlockPos orePos;
    private BlockPos staticTip;
    private UUID bitId;

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

    public int speedStep() {
        return Math.min((int) (Math.abs(getSpeed()) / BASE_RPM), MAX_STEP);
    }

    @Override
    public void tick() {
        super.tick();
        if (level == null || level.isClientSide) return;

        if (level.getGameTime() % 2 == 0) updateAssembly();
        if (level.getGameTime() % 8 == 0) pushOutput();

        int step = speedStep();
        if (ore == null || orePos == null || step == 0) return;

        DrillTier t = tier();
        if (progress + step >= CYCLE_TICKS) {
            ItemStack blockOut = new ItemStack(ore.asItem(), t.oreBlocksPerCycle);
            ItemStack rawOut = rawItemFor(ore, t.rawPerCycle);
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

    // ---------- поиск сборки ----------

    private AABB zone() {
        DrillTier t = tier();
        int r = t.platformHalf + 2;
        return new AABB(worldPosition.getX() - r, worldPosition.getY() - t.height, worldPosition.getZ() - r,
                worldPosition.getX() + r + 1, worldPosition.getY() + 1, worldPosition.getZ() + r + 1);
    }

    private void updateAssembly() {
        Assembly a = findAssembly();
        syncBit(a);
        Block found = null;
        BlockPos foundPos = null;
        if (a != null && a.tipPos() != null) {
            BlockPos below = a.tipPos().below();
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

    private Assembly findAssembly() {
        Assembly moving = findMoving();
        if (moving != null) { staticTip = null; return moving; }
        if (staticTip != null && validStatic(staticTip)) return new Assembly(Vec3.atCenterOf(staticTip), staticTip);
        staticTip = null;
        if (level.getGameTime() % 10 == 0) {
            staticTip = scanStatic();
            if (staticTip != null) return new Assembly(Vec3.atCenterOf(staticTip), staticTip);
        }
        return null;
    }

    /** Сборка внутри движущейся контрапции (поршень Create). */
    private Assembly findMoving() {
        int n = tier().rods();
        for (AbstractContraptionEntity e : level.getEntitiesOfClass(AbstractContraptionEntity.class, zone())) {
            if (e.getContraption() == null) continue;
            var blocks = e.getContraption().getBlocks();
            for (var entry : blocks.entrySet()) {
                if (!entry.getValue().state().is(ModRegistries.DRILL_HEAD_BASE.get())) continue;
                BlockPos tipLocal = entry.getKey().below(n);
                boolean ok = true;
                for (int i = 0; i < n && ok; i++) {
                    var info = blocks.get(tipLocal.above(i));
                    ok = info != null && info.state().is(ModRegistries.DRILL_HEAD.get());
                }
                if (ok) return new Assembly(e.toGlobalVector(Vec3.atCenterOf(tipLocal), 1.0f), null);
            }
        }
        return null;
    }

    /** Нижний блок колонны, если сборка (основание + ровно n блоков головки) стоит в мире. */
    private boolean validStatic(BlockPos tip) {
        int n = tier().rods();
        for (int i = 0; i < n; i++) {
            if (!level.getBlockState(tip.above(i)).is(ModRegistries.DRILL_HEAD.get())) return false;
        }
        return level.getBlockState(tip.above(n)).is(ModRegistries.DRILL_HEAD_BASE.get())
                && !level.getBlockState(tip.below()).is(ModRegistries.DRILL_HEAD.get());
    }

    private BlockPos scanStatic() {
        DrillTier t = tier();
        int n = t.rods();
        int r = t.platformHalf + 2;
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int x = worldPosition.getX() - r; x <= worldPosition.getX() + r; x++) {
            for (int z = worldPosition.getZ() - r; z <= worldPosition.getZ() + r; z++) {
                for (int y = worldPosition.getY(); y >= worldPosition.getY() - t.height; y--) {
                    p.set(x, y, z);
                    if (!level.getBlockState(p).is(ModRegistries.DRILL_HEAD_BASE.get())) continue;
                    BlockPos tip = p.below(n);
                    if (validStatic(tip)) return tip;
                }
            }
        }
        return null;
    }

    // ---------- вращающаяся головка ----------

    private void syncBit(Assembly a) {
        ServerLevel sl = (ServerLevel) level;
        DrillBitEntity bit = bitId != null && sl.getEntity(bitId) instanceof DrillBitEntity b ? b : null;
        if (a == null) {
            if (bit != null) bit.discard();
            bitId = null;
            return;
        }
        if (bit == null) {
            bit = new DrillBitEntity(ModRegistries.DRILL_BIT.get(), sl);
            bit.setTier(tier());
            bit.setPos(a.tipCenter());
            sl.addFreshEntity(bit);
            bitId = bit.getUUID();
        }
        bit.setPos(a.tipCenter());
        bit.setRpm(Math.abs(getSpeed()));
    }

    @Override
    public void setRemoved() {
        if (level instanceof ServerLevel sl && bitId != null && sl.getEntity(bitId) != null) {
            sl.getEntity(bitId).discard();
        }
        super.setRemoved();
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
