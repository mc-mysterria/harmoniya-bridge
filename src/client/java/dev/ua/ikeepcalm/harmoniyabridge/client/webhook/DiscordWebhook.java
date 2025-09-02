package dev.ua.ikeepcalm.harmoniyabridge.client.webhook;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.ua.ikeepcalm.harmoniyabridge.client.detector.MaliciousModDetector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class DiscordWebhook {

    private static final Logger LOGGER = LoggerFactory.getLogger("HarmoniyaBridge-Webhook");
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    
    private final String webhookUrl;
    
    public DiscordWebhook(String webhookUrl) {
        this.webhookUrl = webhookUrl;
    }
    
    public CompletableFuture<Boolean> reportDetection(String playerName, String serverAddress, 
                                                     MaliciousModDetector.DetectionResult result) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                JsonObject embed = createDetectionEmbed(playerName, serverAddress, result);
                JsonObject payload = new JsonObject();
                
                if (result.hasIssues()) {
                    payload.addProperty("content", "🚨 **Malicious/Utility Mods Detected!**");
                } else {
                    payload.addProperty("content", "✅ **Clean Client Connection**");
                }
                
                JsonArray embeds = new JsonArray();
                embeds.add(embed);
                payload.add("embeds", embeds);
                
                return sendWebhook(payload);
            } catch (Exception e) {
                LOGGER.error("Failed to create detection report", e);
                return false;
            }
        });
    }
    
    private JsonObject createDetectionEmbed(String playerName, String serverAddress, 
                                           MaliciousModDetector.DetectionResult result) {
        JsonObject embed = new JsonObject();
        
        if (result.hasIssues()) {
            embed.addProperty("title", "🚨 Suspicious Client Detected");
            embed.addProperty("color", 0xFF0000); // Red
        } else {
            embed.addProperty("title", "✅ Clean Client Connection");
            embed.addProperty("color", 0x00FF00); // Green
        }
        
        embed.addProperty("timestamp", Instant.now().toString());
        
        JsonArray fields = new JsonArray();
        
        JsonObject playerField = new JsonObject();
        playerField.addProperty("name", "👤 Player");
        playerField.addProperty("value", playerName);
        playerField.addProperty("inline", true);
        fields.add(playerField);
        
        JsonObject serverField = new JsonObject();
        serverField.addProperty("name", "🌐 Server");
        serverField.addProperty("value", serverAddress != null ? serverAddress : "Unknown");
        serverField.addProperty("inline", true);
        fields.add(serverField);
        
        JsonObject totalField = new JsonObject();
        totalField.addProperty("name", "📊 Total Mods");
        totalField.addProperty("value", String.valueOf(result.getAllMods().size()));
        totalField.addProperty("inline", true);
        fields.add(totalField);
        
        if (!result.getMaliciousMods().isEmpty()) {
            JsonObject maliciousField = new JsonObject();
            maliciousField.addProperty("name", "🔴 Malicious Mods (" + result.getMaliciousMods().size() + ")");
            maliciousField.addProperty("value", formatModList(result.getMaliciousMods()));
            maliciousField.addProperty("inline", false);
            fields.add(maliciousField);
        }
        
        if (!result.getUtilityMods().isEmpty()) {
            JsonObject utilityField = new JsonObject();
            utilityField.addProperty("name", "🟡 Utility Mods (" + result.getUtilityMods().size() + ")");
            utilityField.addProperty("value", formatModList(result.getUtilityMods()));
            utilityField.addProperty("inline", false);
            fields.add(utilityField);
        }
        
        if (!result.getSuspiciousMods().isEmpty()) {
            JsonObject suspiciousField = new JsonObject();
            suspiciousField.addProperty("name", "🟠 Suspicious Mods (" + result.getSuspiciousMods().size() + ")");
            suspiciousField.addProperty("value", formatModList(result.getSuspiciousMods()));
            suspiciousField.addProperty("inline", false);
            fields.add(suspiciousField);
        }
        
        embed.add("fields", fields);
        
        JsonObject footer = new JsonObject();
        footer.addProperty("text", "HarmoniyaBridge Mod Detector");
        embed.add("footer", footer);
        
        return embed;
    }
    
    private String formatModList(List<String> mods) {
        if (mods.isEmpty()) return "None";
        
        StringBuilder sb = new StringBuilder();
        sb.append("```\n");
        for (int i = 0; i < Math.min(mods.size(), 10); i++) {
            sb.append("• ").append(mods.get(i)).append("\n");
        }
        if (mods.size() > 10) {
            sb.append("... and ").append(mods.size() - 10).append(" more\n");
        }
        sb.append("```");
        return sb.toString();
    }
    
    private boolean sendWebhook(JsonObject payload) {
        if (webhookUrl == null || webhookUrl.trim().isEmpty()) {
            LOGGER.warn("Discord webhook URL not configured, skipping report");
            return false;
        }
        
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(webhookUrl))
                    .timeout(Duration.ofSeconds(30))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
                    .build();
            
            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                LOGGER.info("Successfully sent Discord webhook report");
                return true;
            } else {
                LOGGER.error("Failed to send Discord webhook: {} {}", response.statusCode(), response.body());
                return false;
            }
        } catch (IOException | InterruptedException e) {
            LOGGER.error("Error sending Discord webhook", e);
            return false;
        }
    }
}