package dev.rasm.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

final class RasmScreen extends Screen {
    private final RasmClient controls;

    RasmScreen(RasmClient controls) {
        super(Component.literal("Rasm Client"));
        this.controls = controls;
    }

    @Override
    protected void init() {
        int x = width / 2 - 110;
        int y = height / 2 - 45;
        addRenderableWidget(Button.builder(Component.literal("Quick 180 turn + return"), button -> {
            Minecraft client = Minecraft.getInstance();
            client.setScreen(null);
            controls.quickTurn(client);
        }).bounds(x, y, 220, 20).build());
        addRenderableWidget(Button.builder(flightLabel(), button -> {
            controls.toggleFlight(Minecraft.getInstance());
            button.setMessage(flightLabel());
        }).bounds(x, y + 26, 220, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Request server-permitted flight"), button -> {
            controls.requestServerFlight(Minecraft.getInstance());
            rebuildWidgets();
        }).bounds(x, y + 52, 220, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Done"), button -> onClose())
            .bounds(x, y + 80, 220, 20).build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        super.render(graphics, mouseX, mouseY, delta);
        graphics.drawCenteredString(font, title, width / 2, height / 2 - 75, 0xFFFFFFFF);
        graphics.drawCenteredString(font, "Client flight may be rejected by servers", width / 2, height / 2 + 65, 0xFFBBBBBB);
        graphics.drawCenteredString(font, "Rebind keys in Options > Controls", width / 2, height / 2 + 78, 0xFFBBBBBB);
    }

    private Component flightLabel() {
        return Component.literal("Client flight: " + (controls.clientFlightActive() ? "ON" : "OFF"));
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
