package com.example.minetorio;

import com.simibubi.create.content.kinetics.base.KineticBlock;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Привод бура. Вращение принимает со всех сторон, кроме стороны выхода руды.
 * Сторона выхода (красная точка) задаётся при установке: обычная установка - выход смотрит НА игрока,
 * с зажатым Shift - ОТ игрока. ПКМ по блоку показывает, сколько руды ещё можно добыть.
 */
public class DrillControllerBlock extends KineticBlock implements IBE<DrillControllerBlockEntity> {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public final DrillTier tier;

    public DrillControllerBlock(Properties props, DrillTier tier) {
        super(props);
        this.tier = tier;
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.EAST));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        super.createBlockStateDefinition(b);
        b.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        Direction lookDir = ctx.getHorizontalDirection();
        Direction out = ctx.isSecondaryUseActive() ? lookDir : lookDir.getOpposite();
        return defaultBlockState().setValue(FACING, out);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof DrillControllerBlockEntity be) be.sendInfo(player);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    /** Все стороны, кроме выхода руды. */
    @Override
    public boolean hasShaftTowards(LevelReader world, BlockPos pos, BlockState state, Direction face) {
        return face != state.getValue(FACING);
    }

    @Override public Direction.Axis getRotationAxis(BlockState state) { return Direction.Axis.Y; }

    @Override public Class<DrillControllerBlockEntity> getBlockEntityClass() { return DrillControllerBlockEntity.class; }
    @Override public BlockEntityType<? extends DrillControllerBlockEntity> getBlockEntityType() { return ModRegistries.CONTROLLER_BE.get(); }
}
