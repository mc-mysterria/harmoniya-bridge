package dev.ua.ikeepcalm.harmoniyabridge.client;

import dev.ua.ikeepcalm.harmoniyabridge.client.config.HarmoniyaConfig;
import dev.ua.ikeepcalm.harmoniyabridge.client.detector.MaliciousModDetector;
import dev.ua.ikeepcalm.harmoniyabridge.client.packets.HandshakePayload;
import dev.ua.ikeepcalm.harmoniyabridge.client.webhook.DiscordWebhook;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.session.Session;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class HarmoniyaClient implements ClientModInitializer {

    private static final String MOD_ID = "harmoniyabridge";
    public static final Identifier HANDSHAKE = Identifier.of(MOD_ID, "handshake");
    private static final Logger LOGGER = LoggerFactory.getLogger("HarmoniyaBridge");

    private static HarmoniyaConfig config;
    private static DiscordWebhook discordWebhook;
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

    @Override
    public void onInitializeClient() {
        try {
            LOGGER.info("Starting HarmoniyaBridge client initialization");

            config = HarmoniyaConfig.getInstance();

            if (config.enableDiscordWebhook && !config.discordWebhookUrl.trim().isEmpty() 
                && !config.discordWebhookUrl.contains("your-webhook-token-here")) {
                discordWebhook = new DiscordWebhook(config.discordWebhookUrl);
                LOGGER.info("Discord webhook integration enabled");
            } else {
                LOGGER.info("Discord webhook integration disabled - configure webhook URL in harmoniya-bridge.json");
            }

            PayloadTypeRegistry.playC2S().register(HandshakePayload.ID, HandshakePayload.CODEC);

            ClientPlayConnectionEvents.JOIN.register(this::handleServerJoin);

            LOGGER.info("HarmoniyaBridge client initialization completed");

        } catch (Exception e) {
            LOGGER.error("Failed to initialize HarmoniyaBridge client", e);
        }
    }

    private void handleServerJoin(ClientPlayNetworkHandler handler, PacketSender sender, MinecraftClient client) {
        try {
            LOGGER.info("Player joined server, initiating handshake process");

            if (!ClientPlayNetworking.canSend(HANDSHAKE)) {
                LOGGER.warn("Cannot send handshake packet - server doesn't support the channel");
                return;
            }

            Session session = client.getSession();
            if (session == null) {
                LOGGER.error("Session is null, cannot perform handshake");
                return;
            }

            String accessToken = session.getAccessToken();
            if (accessToken == null || accessToken.trim().isEmpty()) {
                LOGGER.error("Access token is null or empty, cannot perform handshake");
                return;
            }

            LOGGER.info("Access token validated, scanning for mods...");

            CompletableFuture.supplyAsync(() -> {
                if (config.enableModDetection) {
                    return MaliciousModDetector.scanForMaliciousMods();
                } else {
                    return new MaliciousModDetector.DetectionResult(
                            java.util.Collections.emptyList(),
                            java.util.Collections.emptyList(),
                            java.util.Collections.emptyList(),
                            java.util.Collections.emptyList()
                    );
                }
            }).thenAccept(detectionResult -> {
                sendHandshakeWithRetry(accessToken, detectionResult, client, 1);

                if (discordWebhook != null && config.enableDiscordWebhook) {
                    String serverAddress = getServerAddress(client);
                    String playerName = session.getUsername();

                    discordWebhook.reportDetection(playerName, serverAddress, detectionResult)
                            .thenAccept(success -> {
                                if (success) {
                                    LOGGER.info("Discord webhook report sent successfully");
                                } else {
                                    LOGGER.warn("Failed to send Discord webhook report");
                                }
                            });
                }
            }).exceptionally(throwable -> {
                LOGGER.error("Error during mod detection or handshake process", throwable);
                return null;
            });

        } catch (Exception e) {
            LOGGER.error("Unexpected error during server join handling", e);
        }
    }

    private void sendHandshakeWithRetry(String accessToken, MaliciousModDetector.DetectionResult detectionResult,
                                        MinecraftClient client, int attempt) {
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
                        () -> sendHandshakeWithRetry(accessToken, detectionResult, client, nextAttempt),
                        delay,
                        TimeUnit.MILLISECONDS
                );
            } else {
                LOGGER.error("All handshake attempts failed or retries disabled");
            }
        }
    }

    private String getServerAddress(MinecraftClient client) {
        try {
            ServerInfo serverInfo = client.getCurrentServerEntry();
            if (serverInfo != null) {
                return serverInfo.address;
            }

            if (client.getNetworkHandler() != null) {
                return client.getNetworkHandler().getConnection().getAddress().toString();
            }

            return "Unknown";
        } catch (Exception e) {
            LOGGER.warn("Could not determine server address", e);
            return "Unknown";
        }
    }
}
