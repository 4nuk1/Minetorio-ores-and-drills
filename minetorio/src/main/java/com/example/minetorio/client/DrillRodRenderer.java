package com.example.minetorio.client;

import com.example.minetorio.DrillRodEntity;
import com.example.minetorio.ModRegistries;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.block.state.BlockState;

/** Рисует стержень: столбик моделей (модель №3), уходящий вниз от основания и вращающийся вокруг своей оси. */
public class DrillRodRenderer extends EntityRenderer<DrillRodEntity> {
    public DrillRodRenderer(EntityRendererProvider.Context ctx) { super(ctx); }

    @Override
    public ResourceLocation getTextureLocation(DrillRodEntity entity) { return InventoryMenu.BLOCK_ATLAS; }

    @Override
    public void render(DrillRodEntity entity, float yaw, float partialTicks, PoseStack ps, MultiBufferSource buffer, int ignored) {
        float length = entity.getLength();
        if (length <= 0.001f) return;
        BlockState rod = ModRegistries.DRILL_ROD_BLOCK.get().defaultBlockState();
        int light = LevelRenderer.getLightColor(entity.level(), BlockPos.containing(entity.getX(), entity.getY() - 0.5, entity.getZ()));
        float angle = ((entity.level().getGameTime() + partialTicks) * entity.getRpm() * 0.3f) % 360f; // 1 RPM = 0.3 градуса/тик

        ps.pushPose();
        ps.mulPose(Axis.YP.rotationDegrees(angle));
        int full = (int) Math.floor(length);
        for (int k = 0; k < full; k++) {
            ps.pushPose();
            ps.translate(-0.5, -(k + 1), -0.5);
            Minecraft.getInstance().getBlockRenderer().renderSingleBlock(rod, ps, buffer, light, OverlayTexture.NO_OVERLAY);
            ps.popPose();
        }
        float frac = length - full;
        if (frac > 0.02f) {
            ps.pushPose();
            ps.translate(-0.5, -(full + frac), -0.5);
            ps.scale(1f, frac, 1f);
            Minecraft.getInstance().getBlockRenderer().renderSingleBlock(rod, ps, buffer, light, OverlayTexture.NO_OVERLAY);
            ps.popPose();
        }
        ps.popPose();
    }
}
