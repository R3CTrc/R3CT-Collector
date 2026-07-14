package com.r3ct.collection.network;

import com.r3ct.collection.Constants;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;

public record BulkSubmitPayload(String tabId, List<String> itemIds, List<Integer> slotIds) implements CustomPacketPayload {

    public static final Type<BulkSubmitPayload> TYPE = new Type<>(Identifier.parse(Constants.MOD_ID + ":bulk_submit"));

    public static final StreamCodec<RegistryFriendlyByteBuf, BulkSubmitPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, BulkSubmitPayload::tabId,
            ByteBufCodecs.collection(ArrayList::new, ByteBufCodecs.STRING_UTF8), BulkSubmitPayload::itemIds,
            ByteBufCodecs.collection(ArrayList::new, ByteBufCodecs.VAR_INT), BulkSubmitPayload::slotIds,
            BulkSubmitPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}