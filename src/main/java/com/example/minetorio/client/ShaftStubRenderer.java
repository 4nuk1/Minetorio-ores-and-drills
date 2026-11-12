package com.example.minetorio.client;

import com.example.minetorio.ModRegistries;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import java.util.function.Function;

/**
 * Рисует короткие вращающиеся валы на заданных гранях блока. Модель вала своя (assets/minetorio/models/block/shaft_stub.json,
 * текстуры берутся у Create): половинка вала, смотрящая вверх. Её поворачивают на нужную грань и вращают вокруг оси.
 */
public class ShaftStubRenderer<T extends KineticBlockEntity> implements BlockEntityRenderer<T> {
    private final Function<T, List<Direction>> faces;

    public ShaftStubRenderer(Function<T, List<Direction>> faces) { this.faces = faces; }

    @Override
    public void render(T be, float partialTick, PoseStack ps, MultiBufferSource buffer, int light, int overlay) {
        Level level = be.getLevel();
        if (level == null) return;
        BlockState stub = ModRegistries.SHAFT_STUB.get().defaultBlockState();

        // 1 RPM = 6 градусов в секунду = 0.3 градуса за тик (как у Create)
        float angle = ((level.getGameTime() + partialTick) * be.getSpeed() * 0.3f) % 360f;

        for (Direction face : faces.apply(be)) {
            ps.pushPose();
            ps.translate(0.5, 0.5, 0.5);
            // поворачиваем половинку вала (она смотрит вверх, +Y) в сторону грани
            switch (face) {
                case DOWN -> ps.mulPose(Axis.XP.rotationDegrees(180));
                case NORTH -> ps.mulPose(Axis.XP.rotationDegrees(-90));
                case SOUTH -> ps.mulPose(Axis.XP.rotationDegrees(90));
                case EAST -> ps.mulPose(Axis.ZP.rotationDegrees(-90));
                case WEST -> ps.mulPose(Axis.ZP.rotationDegrees(90));
                default -> {}
            }
            // вращение вокруг оси вала: знак учитывает, на какой стороне блока вал (положительной или отрицательной)
            float sign = face.getAxisDirection() == Direction.AxisDirection.POSITIVE ? 1f : -1f;
            ps.mulPose(Axis.YP.rotationDegrees(angle * sign));
            ps.translate(-0.5, -0.5, -0.5);
            Minecraft.getInstance().getBlockRenderer().renderSingleBlock(stub, ps, buffer, light, overlay);
            ps.popPose();
        }
    }
}
