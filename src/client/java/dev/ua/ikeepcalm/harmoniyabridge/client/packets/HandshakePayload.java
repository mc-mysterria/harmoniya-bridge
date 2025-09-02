package dev.ua.ikeepcalm.harmoniyabridge.client.packets;

import dev.ua.ikeepcalm.harmoniyabridge.client.HarmoniyaClient;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

import java.util.ArrayList;
import java.util.List;

public record HandshakePayload(String handshake) implements CustomPayload {

    public static final CustomPayload.Id<HandshakePayload> ID = new CustomPayload.Id<>(HarmoniyaClient.HANDSHAKE);

    public static final PacketCodec<RegistryByteBuf, HandshakePayload> CODEC =
            PacketCodec.tuple(PacketCodecs.STRING, HandshakePayload::handshake, HandshakePayload::new);

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}
