package dev.ua.ikeepcalm.harmoniyabridge.client.detector;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.regex.Pattern;

public class MaliciousModDetector {
    private static final Logger LOGGER = LoggerFactory.getLogger("HarmoniyaBridge-Detector");
    
    private static final Set<String> KNOWN_MALICIOUS_MODS = Set.of(
        "xray", "x-ray", "xrayfabric", "advanced-xray", "cave-mapper",
        "baritone", "baritoneapi", "meteor-client",
        "impact", "wurst", "future", "liquidbounce", "aristois", "wolfram",
        "inertia", "lambda", "rusherhack", "konas", "phobos", "kami"
    );
    
    private static final Set<Pattern> SUSPICIOUS_PATTERNS = Set.of(
        Pattern.compile(".*hack.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*cheat.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*exploit.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*grief.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*auto.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*bot.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*xray.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*wallhack.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*killaura.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*freecam.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*fly.*hack.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*speed.*hack.*", Pattern.CASE_INSENSITIVE)
    );
    
    private static final Set<String> UTILITY_MODS = Set.of(
        "minihud", "tweakeroo", "worldedit", "voxelmap", 
        "journeymap", "clientcommands"
    );
    
    private static final Set<String> ALLOWED_MODS = Set.of(
        // Core modpack mods
        "litematica", "malilib", "rei", "roughlyenoughitems", "jei", 
        "waila", "hwyla", "modmenu", "cloth-config",
        
        // Performance & optimization mods
        "appleskin", "badoptimizations", "dynamic-fps", "entityculling", 
        "fabric-api", "fabricapi", "ferritecore", "immediatelyfast", 
        "iris", "lithium", "modernfix", "moreculling", "sodium", 
        "scalablelux", "threadtweak", "vmp",
        
        // Visual & audio mods  
        "distanthorizons", "skinlayers3d", "sound-physics-remastered", "sounds",
        
        // Utility mods
        "nochatreports", "resourcepackchecker", "yet_another_config_lib", "yacl",
        "voicechat", "simple-voice-chat", "mru",
        
        // Minimap mods (Xaero's are generally accepted)
        "xaeros-minimap", "xaeros_minimap", "xaerosminimap", 
        "xaeros-world-map", "xaeros_world_map", "xaerosworldmap"
    );
    
    public static DetectionResult scanForMaliciousMods() {
        LOGGER.info("Starting malicious mod detection scan...");
        
        List<String> detectedMalicious = new ArrayList<>();
        List<String> detectedUtility = new ArrayList<>();
        List<String> suspiciousMods = new ArrayList<>();
        List<String> allMods = new ArrayList<>();
        
        try {
            Collection<ModContainer> mods = FabricLoader.getInstance().getAllMods();
            
            for (ModContainer mod : mods) {
                String modId = mod.getMetadata().getId().toLowerCase();
                String modName = mod.getMetadata().getName().toLowerCase();
                String description = mod.getMetadata().getDescription().toLowerCase();
                
                allMods.add(mod.getMetadata().getId() + ":" + mod.getMetadata().getVersion().getFriendlyString());
                
                if (ALLOWED_MODS.contains(modId) || ALLOWED_MODS.contains(modName)) {
                    continue;
                }
                
                if (KNOWN_MALICIOUS_MODS.contains(modId) || KNOWN_MALICIOUS_MODS.contains(modName)) {
                    detectedMalicious.add(modId);
                    LOGGER.warn("Detected malicious mod: {}", modId);
                    continue;
                }
                
                if (UTILITY_MODS.contains(modId) || UTILITY_MODS.contains(modName)) {
                    detectedUtility.add(modId);
                    LOGGER.warn("Detected utility mod (may be restricted on server): {}", modId);
                    continue;
                }
                
                boolean isSuspicious = SUSPICIOUS_PATTERNS.stream()
                    .anyMatch(pattern -> pattern.matcher(modId).matches() || 
                                       pattern.matcher(modName).matches() || 
                                       pattern.matcher(description).matches());
                
                if (isSuspicious && !isSystemMod(modId)) {
                    suspiciousMods.add(modId);
                    LOGGER.warn("Detected suspicious mod: {}", modId);
                }
            }
            
            LOGGER.info("Scan complete. Found {} malicious, {} utility, {} suspicious mods out of {} total", 
                       detectedMalicious.size(), detectedUtility.size(), suspiciousMods.size(), allMods.size());
            
        } catch (Exception e) {
            LOGGER.error("Error during mod detection scan", e);
        }
        
        return new DetectionResult(detectedMalicious, detectedUtility, suspiciousMods, allMods);
    }
    
    private static boolean isSystemMod(String modId) {
        return modId.equals("harmoniyabridge") || 
               modId.equals("minecraft") || 
               modId.equals("java") ||
               modId.equals("fabricloader") ||
               modId.equals("fabric-api") ||
               modId.equals("fabricapi") ||
               modId.startsWith("fabric-") ||
               modId.startsWith("minecraft");
    }
    
    public static class DetectionResult {
        private final List<String> maliciousMods;
        private final List<String> utilityMods;
        private final List<String> suspiciousMods;
        private final List<String> allMods;
        
        public DetectionResult(List<String> maliciousMods, List<String> utilityMods, 
                              List<String> suspiciousMods, List<String> allMods) {
            this.maliciousMods = new ArrayList<>(maliciousMods);
            this.utilityMods = new ArrayList<>(utilityMods);
            this.suspiciousMods = new ArrayList<>(suspiciousMods);
            this.allMods = new ArrayList<>(allMods);
        }
        
        public List<String> getMaliciousMods() { return maliciousMods; }
        public List<String> getUtilityMods() { return utilityMods; }
        public List<String> getSuspiciousMods() { return suspiciousMods; }
        public List<String> getAllMods() { return allMods; }
        
        public boolean hasIssues() {
            return !maliciousMods.isEmpty() || !utilityMods.isEmpty() || !suspiciousMods.isEmpty();
        }
        
        public int getTotalIssueCount() {
            return maliciousMods.size() + utilityMods.size() + suspiciousMods.size();
        }
    }
}