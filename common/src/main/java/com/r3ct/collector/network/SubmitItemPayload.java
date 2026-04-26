package com.r3ct.collector.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

// W 1.21 używamy "record", co automatycznie generuje nam konstruktor i gettery!
public record SubmitItemPayload(String itemId) implements CustomPacketPayload {

    // Unikalne ID naszego pakietu
    public static final Type<SubmitItemPayload> TYPE = new Type<>(Identifier.parse("r3ct_collector:submit_item"));

    // Kodek, który mówi grze, jak zamienić nasz "String itemId" na zera i jedynki przesyłane przez neta
    public static final StreamCodec<RegistryFriendlyByteBuf, SubmitItemPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, SubmitItemPayload::itemId,
            SubmitItemPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}