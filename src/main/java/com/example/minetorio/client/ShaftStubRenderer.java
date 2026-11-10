package com.example.minetorio.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import java.util.List;
import java.util.function.Function;

/**
 * Рисует короткие вращающиеся валы (половинки вала Create) на заданных гранях блока.
 * Используется модель самого блока create:shaft, сжатая вдвое вдоль оси: от центра блока до грани.
 */
public class ShaftStubRenderer<T extends KineticBlockEntity> implements BlockEntityRenderer<T> {
    private final Function<T, List<Direction>> faces;

    public ShaftStubRenderer(Function<T, List<Direction>> faces) { this.faces = faces; }

    @Override
    public void render(T be, float partialTick, PoseStack ps, MultiBufferSource buffer, int light, int overlay) {
        Level level = be.getLevel();
        if (level == null) return;
        Block shaft = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("create", "shaft"));
        if (shaft == Blocks.AIR) return;

        // 1 RPM = 6 градусов в секунду = 0.3 градуса за тик (как у Create)
        float angle = ((level.getGameTime() + partialTick) * be.getSpeed() * 0.3f) % 360f;

        for (Direction face : faces.apply(be)) {
            Direction.Axis axis = face.getAxis();
            BlockState state = shaft.defaultBlockState();
            if (state.hasProperty(BlockStateProperties.AXIS)) state = state.setValue(BlockStateProperties.AXIS, axis);

            ps.pushPose();
            // центр отрезка "от центра блока до грани" лежит на 0.25 от центра
            ps.translate(0.5 + face.getStepX() * 0.25, 0.5 + face.getStepY() * 0.25, 0.5 + face.getStepZ() * 0.25);
            ps.mulPose((axis == Direction.Axis.X ? Axis.XP : axis == Direction.Axis.Y ? Axis.YP : Axis.ZP).rotationDegrees(angle));
            ps.scale(axis == Direction.Axis.X ? 0.5f : 1f, axis == Direction.Axis.Y ? 0.5f : 1f, axis == Direction.Axis.Z ? 0.5f : 1f);
            ps.translate(-0.5, -0.5, -0.5);
            Minecraft.getInstance().getBlockRenderer().renderSingleBlock(state, ps, buffer, light, overlay);
            ps.popPose();
        }
    }
}
