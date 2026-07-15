package com.r3ct.collection.mixin;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import org.joml.Matrix3x2fStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;

@Mixin(GuiGraphics.class)
public abstract class GuiGraphicsMixin {

    @Shadow public abstract Matrix3x2fStack pose();
    @Shadow public abstract void renderFakeItem(ItemStack itemStack, int x, int y);

    @Unique
    private static Item CACHED_TROPHY_ITEM = null;

    @Inject(method = "renderItemDecorations(Lnet/minecraft/client/gui/Font;Lnet/minecraft/world/item/ItemStack;IILjava/lang/String;)V", at = @At("TAIL"))
    private void r3ct$renderTrophyMiniature(Font font, ItemStack stack, int x, int y, String text, CallbackInfo ci) {

        if (CACHED_TROPHY_ITEM == null) {
            CACHED_TROPHY_ITEM = BuiltInRegistries.ITEM.get(Identifier.parse("r3ct_collection:trophy")).map(Holder::value).orElse(Items.AIR);
        }

        if (stack.getItem() == CACHED_TROPHY_ITEM && CACHED_TROPHY_ITEM != Items.AIR) {

            CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
            if (customData != null && !customData.isEmpty()) {

                CompoundTag tag = customData.copyTag();

                Optional<String> displayItemOpt = tag.getString("DisplayItem");

                displayItemOpt.ifPresent(itemIdStr -> {
                    Item item = BuiltInRegistries.ITEM.get(Identifier.parse(itemIdStr)).map(Holder::value).orElse(Items.AIR);

                    if (item != Items.AIR) {
                        ItemStack innerStack = new ItemStack(item);
                        Matrix3x2fStack pose = this.pose();

                        pose.pushMatrix();

                        pose.translate(x + 4.8F, y + 2.0F);
                        pose.scale(0.4F, 0.4F);

                        this.renderFakeItem(innerStack, 0, 0);

                        pose.popMatrix();
                    }
                });
            }
        }
    }
}