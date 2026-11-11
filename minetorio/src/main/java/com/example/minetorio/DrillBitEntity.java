package com.example.minetorio;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

/**
 * Головка бура в движении: пока она опущена, её блоки убраны из мира, а саму головку рисует эта сущность.
 * Позиция сущности - центр низа остриЯ. Не сохраняется; основание пересоздаёт её при загрузке.
 */
public class DrillBitEntity extends Entity {
    private static final EntityDataAccessor<Integer> TIER = SynchedEntityData.defineId(DrillBitEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> RPM = SynchedEntityData.defineId(DrillBitEntity.class, EntityDataSerializers.FLOAT);

    private BlockPos owner;

    public DrillBitEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(TIER, 0);
        builder.define(RPM, 0f);
    }

    public void setTier(DrillTier tier) { entityData.set(TIER, tier.ordinal()); }
    public DrillTier getTier() { return DrillTier.values()[Math.min(entityData.get(TIER), DrillTier.values().length - 1)]; }
    public void setRpm(float rpm) { entityData.set(RPM, rpm); }
    public float getRpm() { return entityData.get(RPM); }
    public void setOwner(BlockPos pos) { this.owner = pos.immutable(); }

    /**
     * Раз в секунду проверяем основание. Если его больше нет (сломали, команда и т.п.), блоки головки
     * возвращаются в мир там, где головка сейчас находится, а сущность исчезает.
     */
    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide || tickCount % 20 != 0) return;
        if (owner == null) { discard(); return; }
        if (!level().isLoaded(owner)) return;
        if (!(level().getBlockEntity(owner) instanceof DrillHeadBaseBlockEntity base)) {
            float extension = (owner.getY() - getTier().shapeHeight()) - (float) getY();
            DrillHeadBaseBlockEntity.restoreBlocks((ServerLevel) level(), owner, getTier(), extension);
            discard();
        } else if (!base.isDeployed()) {
            discard();
        }
    }

    @Override public boolean isPickable() { return false; }
    @Override public boolean isPushedByFluid() { return false; }
    @Override protected void readAdditionalSaveData(CompoundTag tag) {}
    @Override protected void addAdditionalSaveData(CompoundTag tag) {}

    @Override
    public AABB getBoundingBoxForCulling() {
        return new AABB(getX() - 4, getY() - 1, getZ() - 4, getX() + 4, getY() + getTier().shapeHeight() + 1, getZ() + 4);
    }
}
