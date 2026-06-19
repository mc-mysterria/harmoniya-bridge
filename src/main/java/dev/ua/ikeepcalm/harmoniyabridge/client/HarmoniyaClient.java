package dev.ua.ikeepcalm.harmoniyabridge.client;

import dev.ua.ikeepcalm.harmoniyabridge.client.config.HarmoniyaConfig;
import dev.ua.ikeepcalm.harmoniyabridge.client.packets.HandshakePayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.User;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class HarmoniyaClient implements ClientModInitializer {

    private static final String MOD_ID = "harmoniyabridge";

    public static final Identifier HANDSHAKE = Identifier.fromNamespaceAndPath(MOD_ID, "handshake");

    private static final Logger LOGGER = LoggerFactory.getLogger("HarmoniyaBridge");

    private static HarmoniyaConfig config;
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

    @Override
    public void onInitializeClient() {
        try {
            LOGGER.info("Starting HarmoniyaBridge client initialization");

            config = HarmoniyaConfig.getInstance();

            PayloadTypeRegistry.serverboundPlay().register(HandshakePayload.ID, HandshakePayload.CODEC);

            ClientPlayConnectionEvents.JOIN.register(this::handleServerJoin);

            LOGGER.info("HarmoniyaBridge client initialization completed");

        } catch (Exception e) {
            LOGGER.error("Failed to initialize HarmoniyaBridge client", e);
        }
    }

    private void handleServerJoin(ClientPacketListener handler, PacketSender sender, Minecraft client) {
        try {
            LOGGER.info("Player joined server, initiating handshake process");

            if (!ClientPlayNetworking.canSend(HandshakePayload.ID)) {
                LOGGER.warn("Cannot send handshake packet - server doesn't support the channel");
                return;
            }

            User user = client.getUser();

            String accessToken = user.getAccessToken();
            if (accessToken.trim().isEmpty()) {
                LOGGER.error("Access token is null or empty, cannot perform handshake");
                return;
            }

            LOGGER.info("Access token validated, sending handshake...");
            sendHandshakeWithRetry(accessToken, 1);

        } catch (Exception e) {
            LOGGER.error("Unexpected error during server join handling", e);
        }
    }

    private void sendHandshakeWithRetry(String accessToken, int attempt) {
        try {
            HandshakePayload payload = new HandshakePayload(accessToken);

            ClientPlayNetworking.send(payload);
            LOGGER.info("Handshake packet sent successfully (attempt {})", attempt);

        } catch (Exception e) {
            LOGGER.error("Failed to send handshake packet (attempt {})", attempt, e);

            if (config.enableRetryMechanism && attempt < config.maxRetryAttempts) {
                int nextAttempt = attempt + 1;
                int delay = config.retryDelayMs * attempt;

                LOGGER.info("Scheduling retry attempt {} in {}ms", nextAttempt, delay);
                scheduler.schedule(
                        () -> sendHandshakeWithRetry(accessToken, nextAttempt),
                        delay,
                        TimeUnit.MILLISECONDS
                );
            } else {
                LOGGER.error("All handshake attempts failed or retries disabled");
            }
        }
    }
}
