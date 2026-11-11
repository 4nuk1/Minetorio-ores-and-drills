package com.example.minetorio;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

/** Стержень бура: выдвигается вниз из основания головки и вращается. Только визуал, не сохраняется. */
public class DrillRodEntity extends Entity {
    private static final EntityDataAccessor<Float> LENGTH = SynchedEntityData.defineId(DrillRodEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> RPM = SynchedEntityData.defineId(DrillRodEntity.class, EntityDataSerializers.FLOAT);

    /** Основание, которому принадлежит стержень (только на сервере). */
    private BlockPos owner;

    public DrillRodEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(LENGTH, 0f);
        builder.define(RPM, 0f);
    }

    public void setLength(float length) { entityData.set(LENGTH, length); }
    public float getLength() { return entityData.get(LENGTH); }
    public void setRpm(float rpm) { entityData.set(RPM, rpm); }
    public float getRpm() { return entityData.get(RPM); }
    public void setOwner(BlockPos pos) { this.owner = pos.immutable(); }

    /** Раз в секунду проверяем, что основание на месте; иначе стержень исчезает. */
    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide && tickCount % 20 == 0) {
            if (owner == null) { discard(); return; }
            if (level().isLoaded(owner) && !(level().getBlockEntity(owner) instanceof DrillHeadBaseBlockEntity)) discard();
        }
    }

    @Override public boolean isPickable() { return false; }
    @Override public boolean isPushedByFluid() { return false; }
    @Override protected void readAdditionalSaveData(CompoundTag tag) {}
    @Override protected void addAdditionalSaveData(CompoundTag tag) {}

    /** Стержень уходит вниз от позиции сущности: расширяем область для отсечения невидимого. */
    @Override
    public AABB getBoundingBoxForCulling() {
        return new AABB(getX() - 1, getY() - getLength() - 1, getZ() - 1, getX() + 1, getY() + 1, getZ() + 1);
    }
}
