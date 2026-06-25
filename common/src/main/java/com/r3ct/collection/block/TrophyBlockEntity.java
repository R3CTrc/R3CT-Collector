package com.r3ct.collection.block;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class TrophyBlockEntity extends BlockEntity {

    private Component customName;
    private String displayItemId = "";
    private String ownerName = "";

    public TrophyBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    public void setCustomName(Component customName) {
        this.customName = customName;
        this.setChanged();
        syncToClient();
    }

    public Component getCustomName() {
        return this.customName;
    }

    public void setDisplayItemId(String itemId) {
        this.displayItemId = itemId;
        this.setChanged();
        syncToClient();
    }

    public String getDisplayItemId() {
        return this.displayItemId;
    }

    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
        this.setChanged();
        syncToClient();
    }

    public String getOwnerName() {
        return this.ownerName;
    }

    private void syncToClient() {
        if (this.level != null && !this.level.isClientSide()) {
            this.level.sendBlockUpdated(this.getBlockPos(), this.getBlockState(), this.getBlockState(), 3);
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (this.customName != null) {
            var ops = this.level != null ? this.level.registryAccess().createSerializationContext(JsonOps.INSTANCE) : JsonOps.INSTANCE;
            ComponentSerialization.CODEC.encodeStart(ops, this.customName)
                    .result()
                    .ifPresent(jsonElement -> output.putString("CustomName", jsonElement.toString()));
        }

        if (this.displayItemId != null && !this.displayItemId.isEmpty()) {
            output.putString("DisplayItem", this.displayItemId);
        }

        if (this.ownerName != null && !this.ownerName.isEmpty()) {
            output.putString("OwnerName", this.ownerName);
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        input.getString("CustomName").ifPresent(jsonStr -> {
            try {
                var ops = this.level != null ? this.level.registryAccess().createSerializationContext(JsonOps.INSTANCE) : JsonOps.INSTANCE;
                var jsonElement = JsonParser.parseString(jsonStr);
                ComponentSerialization.CODEC.parse(ops, jsonElement)
                        .result()
                        .ifPresent(name -> this.customName = name);
            } catch (Exception e) {}
        });

        input.getString("DisplayItem").ifPresent(id -> this.displayItemId = id);
        input.getString("OwnerName").ifPresent(name -> this.ownerName = name);
    }

    @Override
    protected void applyImplicitComponents(DataComponentGetter input) {
        super.applyImplicitComponents(input);
        this.customName = input.get(DataComponents.CUSTOM_NAME);

        CustomData customData = input.get(DataComponents.CUSTOM_DATA);
        if (customData != null) {
            CompoundTag tag = customData.copyTag();

            String item = tag.getString("DisplayItem").orElse("");
            if (!item.isEmpty()) {
                this.displayItemId = item;
            }

            String owner = tag.getString("OwnerName").orElse("");
            if (!owner.isEmpty()) {
                this.ownerName = owner;
            }
        }
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        if (this.customName != null) {
            components.set(DataComponents.CUSTOM_NAME, this.customName);
        }

        if ((this.displayItemId != null && !this.displayItemId.isEmpty()) || (this.ownerName != null && !this.ownerName.isEmpty())) {
            CompoundTag tag = new CompoundTag();
            if (this.displayItemId != null && !this.displayItemId.isEmpty()) {
                tag.putString("DisplayItem", this.displayItemId);
            }
            if (this.ownerName != null && !this.ownerName.isEmpty()) {
                tag.putString("OwnerName", this.ownerName);
            }
            components.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return this.saveWithoutMetadata(registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}