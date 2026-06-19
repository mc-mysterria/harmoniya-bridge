package dev.ua.ikeepcalm.harmoniyabridge.client.screen;

import dev.ua.ikeepcalm.harmoniyabridge.client.config.HarmoniyaConfig;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;
import java.util.function.Consumer;

public class WarningScreen extends Screen {

    private final Consumer<Boolean> callback;
    private Checkbox checkbox;

    public WarningScreen(Consumer<Boolean> callback) {
        super(Component.literal("Увага!"));
        this.callback = callback;
    }

    @Override
    protected void init() {
        // Add "Join" button
        this.addRenderableWidget(Button.builder(Component.literal("Увійти"), button -> {
            if (this.checkbox != null && this.checkbox.selected()) {
                HarmoniyaConfig.getInstance().bypassWarningPopup = true;
                HarmoniyaConfig.getInstance().save();
            }
            this.callback.accept(true);
        }).pos(this.width / 2 - 105, this.height / 2 + 50).size(100, 20).build());

        // Add "Cancel" button
        this.addRenderableWidget(Button.builder(Component.literal("Скасувати"), button -> {
            this.callback.accept(false);
        }).pos(this.width / 2 + 5, this.height / 2 + 50).size(100, 20).build());

        // Add Checkbox
        this.checkbox = Checkbox.builder(Component.literal("Більше не показувати це повідомлення"), this.font)
                .pos(this.width / 2 - 105, this.height / 2 + 20)
                .selected(false)
                .build();
        this.addRenderableWidget(this.checkbox);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractRenderState(graphics, mouseX, mouseY, a);

        // Draw Title "§e§lУвага!" centered
        graphics.centeredText(this.font, Component.literal("§e§lУвага!"), this.width / 2, this.height / 2 - 60, -1);

        // Draw description text (wrapped to width 220)
        String messageStr = "§7При першому вході §c§lБУДЬ ЛАСКА §7оберіть §a\"Я не використовую офіційний Minecraft\" §7та §6§lобов'язково збережіть §7ваш згенерований пароль, який буде показано в чаті!";
        List<FormattedCharSequence> lines = this.font.split(Component.literal(messageStr), 220);
        int yOffset = this.height / 2 - 40;
        for (FormattedCharSequence line : lines) {
            graphics.centeredText(this.font, line, this.width / 2, yOffset, -1);
            yOffset += 9;
        }
    }
}
