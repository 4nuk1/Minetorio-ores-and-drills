package com.example.minetorio;

import com.simibubi.create.content.kinetics.base.KineticBlock;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/** Основание головки: вращение подаётся сверху. Само выдвигает вниз стержень. */
public class DrillHeadBaseBlock extends KineticBlock implements IBE<DrillHeadBaseBlockEntity> {
    public DrillHeadBaseBlock(Properties props) { super(props); }

    @Override
    public boolean hasShaftTowards(LevelReader world, BlockPos pos, BlockState state, Direction face) {
        return face == Direction.UP;
    }

    @Override public Direction.Axis getRotationAxis(BlockState state) { return Direction.Axis.Y; }

    @Override public Class<DrillHeadBaseBlockEntity> getBlockEntityClass() { return DrillHeadBaseBlockEntity.class; }
    @Override public BlockEntityType<? extends DrillHeadBaseBlockEntity> getBlockEntityType() { return ModRegistries.BASE_BE.get(); }
}
