package com.example.minetorio.client;

import com.example.minetorio.DrillBitEntity;
import com.example.minetorio.DrillShapes;
import com.example.minetorio.DrillTier;
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

/** Рисует опущенную головку: все её блоки (по форме из OBJ), вращающиеся вокруг центральной колонны. */
public class DrillBitRenderer extends EntityRenderer<DrillBitEntity> {
    public DrillBitRenderer(EntityRendererProvider.Context ctx) { super(ctx); }

    @Override
    public ResourceLocation getTextureLocation(DrillBitEntity entity) { return InventoryMenu.BLOCK_ATLAS; }

    @Override
    public void render(DrillBitEntity entity, float yaw, float partialTicks, PoseStack ps, MultiBufferSource buffer, int ignored) {
        DrillTier tier = entity.getTier();
        BlockState state = ModRegistries.DRILL_HEAD.get().defaultBlockState();
        int light = LevelRenderer.getLightColor(entity.level(),
                BlockPos.containing(entity.getX(), entity.getY() + tier.shapeHeight() + 0.5, entity.getZ()));
        float angle = ((entity.level().getGameTime() + partialTicks) * entity.getRpm() * 0.3f) % 360f;

        ps.pushPose();
        ps.mulPose(Axis.YP.rotationDegrees(angle));
        for (BlockPos c : DrillShapes.cells(tier)) {
            ps.pushPose();
            ps.translate(c.getX() - 0.5, c.getY(), c.getZ() - 0.5);
            Minecraft.getInstance().getBlockRenderer().renderSingleBlock(state, ps, buffer, light, OverlayTexture.NO_OVERLAY);
            ps.popPose();
        }
        ps.popPose();
    }
}
