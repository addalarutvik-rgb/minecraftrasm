package dev.rasm.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

final class RasmScreen extends Screen {
    private final RasmClient controls;
    private int panelLeft;
    private int panelTop;
    private int panelWidth;
    private Button flightButton;
    private Button noFallButton;

    RasmScreen(RasmClient controls) {
        super(Component.literal("RASM"));
        this.controls = controls;
    }

    @Override
    protected void init() {
        panelWidth = Math.min(300, width - 24);
        panelLeft = (width - panelWidth) / 2;
        panelTop = Math.max(6, (height - 238) / 2);
        int x = panelLeft + 16;
        int w = panelWidth - 32;
        int y = panelTop + 55;
        addRenderableWidget(Button.builder(Component.literal("180 turn + attack"), button -> {
            Minecraft client = Minecraft.getInstance();
            client.setScreen(null);
            controls.quickTurn(client);
        }).bounds(x, y, w, 24).build());
        flightButton = addRenderableWidget(Button.builder(toggleLabel("Flight", controls.clientFlightActive()),
            button -> controls.toggleFlight(Minecraft.getInstance())).bounds(x, y + 30, w, 24).build());
        noFallButton = addRenderableWidget(Button.builder(toggleLabel("Nofall", NoFall.active()),
            button -> NoFall.toggle(Minecraft.getInstance())).bounds(x, y + 60, w, 24).build());
        addRenderableWidget(Button.builder(Component.literal("Server flight (optional)"), button ->
            controls.requestServerFlight(Minecraft.getInstance())).bounds(x, y + 96, w, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Done"), button -> onClose())
            .bounds(x, y + 142, w, 22).build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        int right = panelLeft + panelWidth;
        int bottom = panelTop + 232;
        graphics.fill(panelLeft, panelTop, right, bottom, 0xF019202C);
        graphics.fill(panelLeft, panelTop, right, panelTop + 3, 0xFF55D9CF);
        graphics.drawString(font, title, panelLeft + 16, panelTop + 15, 0xFFF1F5FA);
        graphics.drawString(font, "Movement & combat", panelLeft + 16, panelTop + 31, 0xFF94A3B8);
        graphics.fill(panelLeft + 16, panelTop + 46, right - 16, panelTop + 47, 0xFF364152);
        graphics.drawCenteredString(font, "Change keys in Options > Controls", width / 2, panelTop + 180, 0xFF94A3B8);
        flightButton.setMessage(toggleLabel("Flight", controls.clientFlightActive()));
        noFallButton.setMessage(toggleLabel("Nofall", NoFall.active()));
        super.render(graphics, mouseX, mouseY, delta);
    }

    private Component toggleLabel(String name, boolean active) {
        return Component.literal(name + "   " + (active ? "ON" : "OFF"))
            .withStyle(active ? ChatFormatting.AQUA : ChatFormatting.GRAY);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
