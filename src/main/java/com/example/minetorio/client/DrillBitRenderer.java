package com.example.minetorio.client;

import com.example.minetorio.DrillBitEntity;
import com.example.minetorio.DrillShapes;
import com.example.minetorio.DrillTier;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** Рисует головку как набор кубов (по форме из OBJ), вращая вокруг центральной колонны. Заглушки: железо и красная шерсть. */
public class DrillBitRenderer extends EntityRenderer<DrillBitEntity> {
    public DrillBitRenderer(EntityRendererProvider.Context ctx) { super(ctx); }

    @Override
    public ResourceLocation getTextureLocation(DrillBitEntity entity) { return InventoryMenu.BLOCK_ATLAS; }

    @Override
    public void render(DrillBitEntity entity, float yaw, float partialTicks, PoseStack ps, MultiBufferSource buffer, int light) {
        DrillTier tier = entity.getTier();
        BlockState state = tier == DrillTier.SMALL_3X3 ? Blocks.IRON_BLOCK.defaultBlockState() : Blocks.RED_WOOL.defaultBlockState();
        // 1 RPM = 6 градусов в секунду = 0.3 градуса за тик
        float angle = ((entity.level().getGameTime() + partialTicks) * entity.getRpm() * 0.3f) % 360f;
        ps.pushPose();
        ps.mulPose(Axis.YP.rotationDegrees(angle));
        for (BlockPos c : DrillShapes.visualCells(tier)) {
            ps.pushPose();
            ps.translate(c.getX() - 0.5, c.getY() - 0.5, c.getZ() - 0.5);
            Minecraft.getInstance().getBlockRenderer().renderSingleBlock(state, ps, buffer, light, OverlayTexture.NO_OVERLAY);
            ps.popPose();
        }
        ps.popPose();
    }
}
