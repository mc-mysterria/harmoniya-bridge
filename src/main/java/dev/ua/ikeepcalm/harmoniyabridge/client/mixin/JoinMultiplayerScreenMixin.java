package dev.ua.ikeepcalm.harmoniyabridge.client.mixin;

import dev.ua.ikeepcalm.harmoniyabridge.client.config.HarmoniyaConfig;
import dev.ua.ikeepcalm.harmoniyabridge.client.screen.WarningScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.ServerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(JoinMultiplayerScreen.class)
public class JoinMultiplayerScreenMixin {

    @Unique
    private boolean bypassPopup = false;

    @Inject(method = "init", at = @At("HEAD"))
    private void onInit(CallbackInfo ci) {
        Minecraft client = Minecraft.getInstance();
        ServerList serverList = new ServerList(client);
        serverList.load();
        boolean exists = false;
        for (int i = 0; i < serverList.size(); i++) {
            ServerData server = serverList.get(i);
            if (server.ip.equalsIgnoreCase("mc.mysterria.net") || server.ip.equalsIgnoreCase("play.mysterria.net")) {
                exists = true;
                break;
            }
        }
        if (!exists) {
            ServerData newServer = new ServerData("Містеррія", "mc.mysterria.net", ServerData.Type.OTHER);
            serverList.add(newServer, false);
            serverList.save();
        }
    }

    @Inject(method = "join", at = @At("HEAD"), cancellable = true)
    private void onJoin(ServerData serverData, CallbackInfo ci) {
        if (!bypassPopup && !HarmoniyaConfig.getInstance().bypassWarningPopup && (serverData.ip.equalsIgnoreCase("mc.mysterria.net") || serverData.ip.equalsIgnoreCase("play.mysterria.net"))) {
            ci.cancel();

            Minecraft.getInstance().setScreen(new WarningScreen(
                    confirmed -> {
                        if (confirmed) {
                            bypassPopup = true;
                            JoinMultiplayerScreen screen = (JoinMultiplayerScreen) (Object) this;
                            screen.join(serverData);
                            bypassPopup = false;
                        } else {
                            Minecraft.getInstance().setScreen((JoinMultiplayerScreen) (Object) this);
                        }
                    }
            ));
        }
    }
}
