package com.r3ct.collection.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.r3ct.collection.block.TrophyBlock;
import com.r3ct.collection.block.TrophyBlockEntity;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class TrophyBlockEntityRenderer implements BlockEntityRenderer<TrophyBlockEntity, TrophyBlockEntityRenderer.TrophyRenderState> {

    private final ItemModelResolver itemModelResolver;
    private final Font font;

    public TrophyBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        this.itemModelResolver = context.itemModelResolver();
        this.font = context.font();
    }

    @Override
    public TrophyRenderState createRenderState() {
        return new TrophyRenderState();
    }

    @Override
    public void extractRenderState(TrophyBlockEntity blockEntity, TrophyRenderState state, float partialTicks, Vec3 cameraPosition, ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(blockEntity, state, partialTicks, cameraPosition, breakProgress);

        state.facing = blockEntity.getBlockState().getValue(TrophyBlock.FACING);
        state.isWall = blockEntity.getBlockState().getValue(TrophyBlock.FACE) == AttachFace.WALL;

        String ownerName = blockEntity.getOwnerName();
        if (ownerName != null && !ownerName.isEmpty()) {
            state.customName = Component.literal(ownerName);
        } else {
            state.customName = null;
        }

        state.time = (blockEntity.getLevel() != null ? blockEntity.getLevel().getGameTime() : 0) + partialTicks;

        String itemIdStr = blockEntity.getDisplayItemId();
        if (itemIdStr != null && !itemIdStr.isEmpty()) {
            Item item = BuiltInRegistries.ITEM.get(Identifier.parse(itemIdStr)).map(Holder::value).orElse(Items.AIR);
            if (item != Items.AIR) {
                this.itemModelResolver.updateForTopItem(state.itemState, new ItemStack(item), ItemDisplayContext.GROUND, blockEntity.getLevel(), null, 0);
            } else {
                state.itemState.clear();
            }
        } else {
            state.itemState.clear();
        }
    }

    @Override
    public void submit(TrophyRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
        poseStack.pushPose();

        poseStack.translate(0.5D, 0.0D, 0.5D);
        float rotation = -state.facing.toYRot();
        poseStack.mulPose(Axis.YP.rotationDegrees(rotation));

        if (state.isWall) {
            poseStack.translate(0.0D, 0.0D, -0.3125D);
        }

        if (!state.itemState.isEmpty()) {
            poseStack.pushPose();

            float offset = (float) Math.sin(state.time / 10.0F) * 0.05F;
            float spin = state.time * 3.0F;

            poseStack.translate(0.0D, 0.45D + offset, 0.0D);
            poseStack.mulPose(Axis.YP.rotationDegrees(spin));
            poseStack.scale(1.0F, 1.0F, 1.0F);

            state.itemState.submit(poseStack, submitNodeCollector, 15728880, OverlayTexture.NO_OVERLAY, 0);

            poseStack.popPose();
        }

        if (state.customName != null) {
            FormattedCharSequence formattedText = state.customName.getVisualOrderText();
            float textWidth = this.font.width(formattedText);
            float textX = -textWidth / 2.0F;

            poseStack.pushPose();
            poseStack.translate(0.0D, 0.18D, 0.25D);
            poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
            poseStack.scale(-0.015F, -0.015F, 0.015F);

            submitNodeCollector.submitText(
                    poseStack, textX, 0.0F, formattedText, false,
                    Font.DisplayMode.NORMAL, 15728880, 0xFFFFFFFF, 0, 0x00000000
            );
            poseStack.popPose();

            poseStack.pushPose();
            poseStack.translate(0.0D, 0.18D, -0.25D);
            poseStack.mulPose(Axis.YP.rotationDegrees(0.0F));
            poseStack.scale(-0.015F, -0.015F, 0.015F);

            submitNodeCollector.submitText(
                    poseStack, textX, 0.0F, formattedText, false,
                    Font.DisplayMode.NORMAL, 15728880, 0xFFFFFFFF, 0, 0x00000000
            );
            poseStack.popPose();
        }

        poseStack.popPose();
    }

    public static class TrophyRenderState extends BlockEntityRenderState {
        public Direction facing = Direction.NORTH;
        public boolean isWall = false;
        public final ItemStackRenderState itemState = new ItemStackRenderState();
        public Component customName = null;
        public float time = 0.0f;
    }
}