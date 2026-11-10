package com.example.minetorio;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.UUID;

/**
 * Основание головки. Игрок ставит под ним головку из блоков "Головка бура" точно по форме из OBJ.
 * Когда на основание подано вращение, блоки головки убираются из мира, из основания выдвигается стержень
 * (DrillRodEntity), который толкает вниз и вращает головку (DrillBitEntity), пока она не упрётся в блок.
 * Без вращения стержень втягивается, а головка снова становится блоками на прежнем месте.
 */
public class DrillHeadBaseBlockEntity extends KineticBlockEntity {
    public static final float IMPACT = 8f;          // 8 su на RPM: 256 su при 32 RPM
    private static final float MAX_EXTENSION = 48f; // предельная глубина спуска, блоков

    private BlockPos pairedDrive;
    private DrillTier shapeTier;   // головка, собранная под основанием (пока она в виде блоков)
    private boolean deployed;      // блоки головки убраны, головка - сущность
    private DrillTier tier;        // размер опущенной головки
    private float extension;       // на сколько блоков опущена головка
    private UUID rodId;
    private UUID bitId;

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
    public void pair(BlockPos drive) { this.pairedDrive = drive.immutable(); setChanged(); }
    public boolean isDeployed() { return deployed; }
    public DrillTier currentTier() { return deployed ? tier : shapeTier; }

    private int tipHomeY() { return worldPosition.getY() - tier.shapeHeight(); }

    /** Головка опущена, остриё стоит ровно на границе блоков (упёрлось или не двигалось). */
    private boolean isTouching() {
        double tipBottom = tipHomeY() - extension;
        return Math.abs(tipBottom - Math.round(tipBottom)) < 0.01;
    }

    public boolean isReadyFor(DrillTier t) {
        return deployed && tier == t && getSpeed() != 0 && isTouching();
    }

    /** Блок прямо под остриём головки: именно из него бур берёт руду. */
    public BlockPos blockBelowTip() {
        return new BlockPos(worldPosition.getX(), (int) Math.round(tipHomeY() - extension) - 1, worldPosition.getZ());
    }

    // ---------- логика ----------
    @Override
    public void tick() {
        super.tick();
        if (level == null || level.isClientSide) return;
        long t = level.getGameTime();
        float speed = Math.abs(getSpeed());

        if (t % 20 == 0) validatePair();

        if (!deployed) {
            if (t % 5 == 0) shapeTier = detectTier();
            if (speed > 0 && shapeTier != null) deploy(shapeTier);
            return;
        }

        if (speed > 0) {
            extension = pushDown(speed * 0.0015f);          // 32 RPM ~ 0.05 блока в тик
        } else {
            extension = Math.max(0f, extension - 0.04f);    // без вращения стержень втягивается
            if (extension <= 0f) {
                undeploy();
                return;
            }
        }
        syncEntities(speed);
    }

    private void validatePair() {
        if (pairedDrive == null || !level.isLoaded(pairedDrive)) return;
        if (!(level.getBlockEntity(pairedDrive) instanceof DrillControllerBlockEntity d) || !worldPosition.equals(d.pairedBase())) {
            pairedDrive = null;
            setChanged();
        }
    }

    // ---------- сборка головки ----------

    private DrillTier detectTier() {
        if (shapeMatches(DrillTier.LARGE_5X5)) return DrillTier.LARGE_5X5;
        if (shapeMatches(DrillTier.SMALL_3X3)) return DrillTier.SMALL_3X3;
        return null;
    }

    /** Под основанием стоят блоки головки точно по форме: все клетки заняты. */
    private boolean shapeMatches(DrillTier t) {
        int tipY = worldPosition.getY() - t.shapeHeight();
        for (BlockPos rel : DrillShapes.cells(t)) {
            BlockPos p = new BlockPos(worldPosition.getX() + rel.getX(), tipY + rel.getY(), worldPosition.getZ() + rel.getZ());
            if (!level.getBlockState(p).is(ModRegistries.DRILL_HEAD.get())) return false;
        }
        return true;
    }

    private void deploy(DrillTier t) {
        int tipY = worldPosition.getY() - t.shapeHeight();
        for (BlockPos rel : DrillShapes.cells(t)) {
            level.removeBlock(new BlockPos(worldPosition.getX() + rel.getX(), tipY + rel.getY(), worldPosition.getZ() + rel.getZ()), false);
        }
        tier = t;
        deployed = true;
        extension = 0f;
        setChanged();
    }

    private void undeploy() {
        restoreBlocks((ServerLevel) level, worldPosition, tier, 0f);
        discardEntities();
        deployed = false;
        extension = 0f;
        setChanged();
    }

    /** Ставит блоки головки в мир со сдвигом вниз на extension (округлённым). Занятое место - выпадает предметом. */
    public static void restoreBlocks(ServerLevel level, BlockPos base, DrillTier t, float extension) {
        int tipY = base.getY() - t.shapeHeight() - Math.round(extension);
        for (BlockPos rel : DrillShapes.cells(t)) {
            BlockPos p = new BlockPos(base.getX() + rel.getX(), tipY + rel.getY(), base.getZ() + rel.getZ());
            if (level.getBlockState(p).canBeReplaced()) {
                level.setBlock(p, ModRegistries.DRILL_HEAD.get().defaultBlockState(), 3);
            } else {
                Block.popResource(level, p, new ItemStack(ModRegistries.DRILL_HEAD_ITEM.get()));
            }
        }
    }

    // ---------- движение вниз ----------

    /**
     * Опускает головку на delta, но не дальше, чем позволяют блоки на пути: каждая клетка головки
     * при входе в новый ряд блоков проверяет, свободен ли он. Головка не ломает блоки, а встаёт на них.
     */
    private float pushDown(float delta) {
        int x = worldPosition.getX(), z = worldPosition.getZ();
        int tipHome = tipHomeY();
        float next = Math.min(MAX_EXTENSION, extension + delta);
        for (BlockPos rel : DrillShapes.cells(tier)) {
            double bottom = tipHome + rel.getY() - extension;
            double bottomNew = tipHome + rel.getY() - next;
            int rowOld = (int) Math.floor(bottom + 1e-3);
            int rowNew = (int) Math.floor(bottomNew + 1e-3);
            if (rowNew < rowOld) {
                BlockPos p = new BlockPos(x + rel.getX(), rowNew, z + rel.getZ());
                if (!level.getBlockState(p).getCollisionShape(level, p).isEmpty()) {
                    next = Math.min(next, (float) (tipHome + rel.getY() - rowOld));
                }
            }
        }
        return Math.max(extension, next);
    }

    // ---------- сущности ----------

    private void syncEntities(float rpm) {
        ServerLevel sl = (ServerLevel) level;
        DrillRodEntity rod = rodId != null && sl.getEntity(rodId) instanceof DrillRodEntity r ? r : null;
        DrillBitEntity bit = bitId != null && sl.getEntity(bitId) instanceof DrillBitEntity b ? b : null;
        double cx = worldPosition.getX() + 0.5, cz = worldPosition.getZ() + 0.5;

        if (rod == null) {
            rod = new DrillRodEntity(ModRegistries.DRILL_ROD.get(), sl);
            rod.setOwner(worldPosition);
            rod.setPos(cx, worldPosition.getY(), cz);
            sl.addFreshEntity(rod);
            rodId = rod.getUUID();
        }
        if (bit == null) {
            bit = new DrillBitEntity(ModRegistries.DRILL_BIT.get(), sl);
            bit.setTier(tier);
            bit.setOwner(worldPosition);
            bit.setPos(cx, tipHomeY() - extension, cz);
            sl.addFreshEntity(bit);
            bitId = bit.getUUID();
        }
        rod.setLength(extension);   // стержень заполняет промежуток между основанием и верхом головки
        rod.setRpm(rpm);
        bit.setPos(cx, tipHomeY() - extension, cz);
        bit.setRpm(rpm);
    }

    private void discardEntities() {
        ServerLevel sl = (ServerLevel) level;
        if (rodId != null && sl.getEntity(rodId) != null) sl.getEntity(rodId).discard();
        if (bitId != null && sl.getEntity(bitId) != null) sl.getEntity(bitId).discard();
        rodId = null;
        bitId = null;
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        if (pairedDrive != null) tag.putLong("Drive", pairedDrive.asLong());
        tag.putBoolean("Deployed", deployed);
        if (tier != null) tag.putInt("Tier", tier.ordinal());
        tag.putFloat("Extension", extension);
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        pairedDrive = tag.contains("Drive") ? BlockPos.of(tag.getLong("Drive")) : null;
        deployed = tag.getBoolean("Deployed");
        tier = tag.contains("Tier") ? DrillTier.values()[Math.min(tag.getInt("Tier"), DrillTier.values().length - 1)] : null;
        extension = tag.getFloat("Extension");
        if (tier == null) deployed = false;
    }
}
