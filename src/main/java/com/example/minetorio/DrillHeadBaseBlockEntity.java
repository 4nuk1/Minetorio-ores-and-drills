package com.example.minetorio;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/**
 * Основание головки. Под ним игрок строит головку из блоков (по форме из OBJ) с пустой центральной колонной.
 * Когда на основание подано вращение, из него вниз по колонне выдвигается вращающийся стержень (DrillRodEntity).
 * Связь с приводом: привод находит свободное основание нужного размера и они закрепляются друг за другом.
 */
public class DrillHeadBaseBlockEntity extends KineticBlockEntity {
    public static final float IMPACT = 8f; // 8 su на RPM: 256 su при 32 RPM

    private BlockPos pairedDrive;
    private DrillTier shapeTier;   // какая головка собрана под основанием (null - никакая)
    private float length;          // выдвинутая длина стержня в блоках
    private UUID rodId;

    public DrillHeadBaseBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public float calculateStressApplied() {
        this.lastStressApplied = IMPACT;
        return IMPACT;
    }

    // ---------- данные для привода ----------
    public BlockPos pairedDrive() { return pairedDrive; }
    public DrillTier shapeTier() { return shapeTier; }
    public void pair(BlockPos drive) { this.pairedDrive = drive.immutable(); setChanged(); }

    /** Основание вращается, головка собрана под нужный размер, стержень выдвинут до конца. */
    public boolean isReadyFor(DrillTier tier) {
        return shapeTier == tier && getSpeed() != 0 && length >= tier.shapeHeight() - 0.01f;
    }

    /** Блок прямо под нижним концом стержня (там должна лежать руда). */
    public BlockPos blockBelowTip() {
        return new BlockPos(worldPosition.getX(), worldPosition.getY() - shapeTier.shapeHeight() - 1, worldPosition.getZ());
    }

    // ---------- логика ----------
    @Override
    public void tick() {
        super.tick();
        if (level == null || level.isClientSide) return;
        long t = level.getGameTime();
        if (t % 10 == 0) shapeTier = detectTier();
        if (t % 20 == 0) validatePair();

        float speed = Math.abs(getSpeed());
        float target = (speed > 0 && shapeTier != null) ? shapeTier.shapeHeight() : 0f;
        if (length < target) length = Math.min(target, length + speed * 0.0015f);   // 32 RPM ~ 0.05 блока в тик
        else if (length > target) length = Math.max(target, length - 0.04f);
        syncRod(speed);
    }

    private void validatePair() {
        if (pairedDrive == null || !level.isLoaded(pairedDrive)) return;
        if (!(level.getBlockEntity(pairedDrive) instanceof DrillControllerBlockEntity d) || !worldPosition.equals(d.pairedBase())) {
            pairedDrive = null;
            setChanged();
        }
    }

    private DrillTier detectTier() {
        if (shapeMatches(DrillTier.LARGE_5X5)) return DrillTier.LARGE_5X5;
        if (shapeMatches(DrillTier.SMALL_3X3)) return DrillTier.SMALL_3X3;
        return null;
    }

    /** Под основанием: блоки головки точно по форме, центральная колонна свободна для стержня. */
    private boolean shapeMatches(DrillTier tier) {
        int h = tier.shapeHeight();
        int x = worldPosition.getX(), z = worldPosition.getZ();
        int tipY = worldPosition.getY() - h;
        for (BlockPos rel : DrillShapes.visualCells(tier)) {
            if (!level.getBlockState(new BlockPos(x + rel.getX(), tipY + rel.getY(), z + rel.getZ())).is(ModRegistries.DRILL_HEAD.get()))
                return false;
        }
        for (int y = tipY; y < worldPosition.getY(); y++) {
            if (!level.getBlockState(new BlockPos(x, y, z)).isAir()) return false;
        }
        return true;
    }

    private void syncRod(float rpm) {
        ServerLevel sl = (ServerLevel) level;
        DrillRodEntity rod = rodId != null && sl.getEntity(rodId) instanceof DrillRodEntity r ? r : null;
        if (length <= 0.001f) {
            if (rod != null) rod.discard();
            rodId = null;
            return;
        }
        if (rod == null) {
            rod = new DrillRodEntity(ModRegistries.DRILL_ROD.get(), sl);
            rod.setOwner(worldPosition);
            rod.setPos(rodOrigin());
            sl.addFreshEntity(rod);
            rodId = rod.getUUID();
        }
        rod.setLength(length);
        rod.setRpm(rpm);
    }

    /** Нижняя грань основания, центр по X/Z. */
    private Vec3 rodOrigin() {
        return new Vec3(worldPosition.getX() + 0.5, worldPosition.getY(), worldPosition.getZ() + 0.5);
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        if (pairedDrive != null) tag.putLong("Drive", pairedDrive.asLong());
        tag.putFloat("Length", length);
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        pairedDrive = tag.contains("Drive") ? BlockPos.of(tag.getLong("Drive")) : null;
        length = tag.getFloat("Length");
    }
}
