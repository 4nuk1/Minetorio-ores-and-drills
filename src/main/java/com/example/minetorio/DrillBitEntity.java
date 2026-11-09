package com.example.minetorio;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

/** Вращающаяся головка бура: только визуал, без коллизий. Создаётся и двигается приводом, не сохраняется. */
public class DrillBitEntity extends Entity {
    private static final EntityDataAccessor<Integer> TIER = SynchedEntityData.defineId(DrillBitEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> RPM = SynchedEntityData.defineId(DrillBitEntity.class, EntityDataSerializers.FLOAT);

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

    @Override public boolean isPickable() { return false; }
    @Override public boolean isPushedByFluid() { return false; }
    @Override protected void readAdditionalSaveData(CompoundTag tag) {}
    @Override protected void addAdditionalSaveData(CompoundTag tag) {}

    /** Головка крупнее хитбокса: расширяем область для отсечения невидимого. */
    @Override
    public AABB getBoundingBoxForCulling() { return getBoundingBox().inflate(4.0, 8.0, 4.0); }
}
