package dev.ua.ikeepcalm.harmoniyabridge.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class HarmoniyaConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger("HarmoniyaBridge-Config");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String CONFIG_FILE = "harmoniya-bridge.json";
    
    private static HarmoniyaConfig instance;
    
    public boolean enableModDetection = true;
    public boolean enableDiscordWebhook = true;
    public String discordWebhookUrl = "https://discord.com/api/webhooks/1412324308713340938/WnaESVOsEufsOnqf90E-btKxtE8v0hc0knE3NoDOxxnIghsv947kwJpQJl5llyk7X4Xt";
    public boolean enableRetryMechanism = true;
    public int maxRetryAttempts = 3;
    public int retryDelayMs = 5000;
    
    public static HarmoniyaConfig getInstance() {
        if (instance == null) {
            instance = load();
        }
        return instance;
    }
    
    private static HarmoniyaConfig load() {
        Path configPath = FabricLoader.getInstance().getConfigDir().resolve(CONFIG_FILE);
        
        if (Files.exists(configPath)) {
            try {
                String json = Files.readString(configPath);
                HarmoniyaConfig config = GSON.fromJson(json, HarmoniyaConfig.class);
                LOGGER.info("Loaded configuration from {}", configPath);
                return config;
            } catch (Exception e) {
                LOGGER.error("Failed to load config, using defaults", e);
            }
        } else {
            LOGGER.info("Config file not found, creating default at {}", configPath);
        }
        
        HarmoniyaConfig defaultConfig = new HarmoniyaConfig();
        defaultConfig.save();
        return defaultConfig;
    }
    
    public void save() {
        Path configPath = FabricLoader.getInstance().getConfigDir().resolve(CONFIG_FILE);
        
        try {
            Files.createDirectories(configPath.getParent());
            String json = GSON.toJson(this);
            Files.writeString(configPath, json);
            LOGGER.info("Saved configuration to {}", configPath);
        } catch (IOException e) {
            LOGGER.error("Failed to save configuration", e);
        }
    }
}