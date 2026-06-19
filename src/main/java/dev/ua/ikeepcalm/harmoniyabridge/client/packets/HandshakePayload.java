package dev.ua.ikeepcalm.harmoniyabridge.client.packets;

import dev.ua.ikeepcalm.harmoniyabridge.client.HarmoniyaClient;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record HandshakePayload(String handshake) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<HandshakePayload> ID = new CustomPacketPayload.Type<>(HarmoniyaClient.HANDSHAKE);

    public static final StreamCodec<RegistryFriendlyByteBuf, HandshakePayload> CODEC =
            StreamCodec.composite(ByteBufCodecs.stringUtf8(32767), HandshakePayload::handshake, HandshakePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
