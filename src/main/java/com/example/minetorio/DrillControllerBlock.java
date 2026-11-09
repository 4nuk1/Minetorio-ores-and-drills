package com.example.minetorio;

import com.simibubi.create.content.kinetics.base.KineticBlock;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;

/** Верхний блок бура: принимает вращение сверху/снизу (ось Y) и выдаёт руду в выбранную сторону. */
public class DrillControllerBlock extends KineticBlock implements IBE<DrillControllerBlockEntity> {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING; // сторона выхода руды
    public final DrillTier tier;

    public DrillControllerBlock(Properties props, DrillTier tier) {
        super(props);
        this.tier = tier;
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> b) {
        super.createBlockStateDefinition(b);
        b.add(FACING);
    }

    /**
     * Сторона выхода руды задаётся только при установке и дальше не меняется:
     * обычная установка - выход смотрит НА игрока, установка с зажатым Shift - ОТ игрока.
     * Выход отмечен красной точкой на текстуре.
     */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        Direction lookDir = ctx.getHorizontalDirection(); // куда смотрит игрок
        Direction out = ctx.isSecondaryUseActive() ? lookDir : lookDir.getOpposite();
        return defaultBlockState().setValue(FACING, out);
    }

    @Override public boolean hasShaftTowards(LevelReader world, BlockPos pos, BlockState state, Direction face) {
        return face.getAxis() == Direction.Axis.Y;
    }
    @Override public Direction.Axis getRotationAxis(BlockState state) { return Direction.Axis.Y; }

    @Override public Class<DrillControllerBlockEntity> getBlockEntityClass() { return DrillControllerBlockEntity.class; }
    @Override public BlockEntityType<? extends DrillControllerBlockEntity> getBlockEntityType() { return ModRegistries.CONTROLLER_BE.get(); }
}
