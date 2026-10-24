package dev.rasm.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.controls.KeyBindsScreen;
import net.minecraft.network.chat.Component;

final class RasmScreen extends Screen {
    private final RasmClient controls;
    private int left, top, panelWidth;
    private Button flight, nofall, ghost;
    RasmScreen(RasmClient controls) { super(Component.literal("RASM")); this.controls = controls; }
    @Override protected void init() {
        panelWidth = Math.min(350, width - 20);
        left = (width - panelWidth) / 2;
        top = Math.max(2, (height - 232) / 2);
        int x = left + 12, y = top + 46, half = (panelWidth - 30) / 2;
        button("180 turn + attack", x, y, half, () -> { closeForAction(); controls.quickTurn(Minecraft.getInstance()); });
        button("Lunge swap 1 / 3", x + half + 6, y, half, () -> {
            closeForAction(); controls.swaps.lunge(Minecraft.getInstance());
        }).setTooltip(Tooltip.create(Component.literal("Slot 1: low-cooldown item. Slot 3: Lunge spear. One sequence per press.")));
        button("Mace swap 1 / 2", x, y + 28, half, () -> {
            closeForAction(); controls.swaps.mace(Minecraft.getInstance());
        }).setTooltip(Tooltip.create(Component.literal("Attacks with slot 1, switches to a mace in slot 2, attacks, then returns to slot 1.")));
        ghost = button("Ghost block", x + half + 6, y + 28, half, () -> controls.ghost.place(Minecraft.getInstance()));
        ghost.setTooltip(Tooltip.create(Component.literal("Places one client-only full glass block under your feet, in air. Repeat to place more; /ghost clear removes all.")));
        flight = button("Flight", x, y + 56, half, () -> controls.toggleFlight(Minecraft.getInstance()));
        nofall = button("Nofall", x + half + 6, y + 56, half, () -> NoFall.toggle(Minecraft.getInstance()));
        button("Server flight (optional)", x, y + 88, panelWidth - 24,
            () -> controls.requestServerFlight(Minecraft.getInstance()));
        button("Rebind keys", x, y + 116, half,
            () -> Minecraft.getInstance().setScreen(new KeyBindsScreen(this, Minecraft.getInstance().options)));
        button("Done", x + half + 6, y + 116, half, this::onClose);
    }
    private void closeForAction() { Minecraft.getInstance().setScreen(null); }
    private Button button(String label, int x, int y, int w, Runnable action) {
        return addRenderableWidget(Button.builder(Component.literal(label), b -> action.run()).bounds(x,y,w,22).build());
    }
    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        graphics.fill(left, top, left + panelWidth, top + 222, 0xF019202C);
        graphics.fill(left, top, left + panelWidth, top + 3, 0xFF55D9CF);
        graphics.drawString(font, title, left + 12, top + 13, 0xFFF1F5FA);
        graphics.drawString(font, "Movement / combat / visuals", left + 12, top + 29, 0xFF94A3B8);
        graphics.drawCenteredString(font, "/glow creeper   /glow off", width / 2, top + 196, 0xFF94A3B8);
        flight.setMessage(label("Flight",controls.clientFlightActive()));
        nofall.setMessage(label("Nofall",NoFall.active()));
        ghost.setMessage(Component.literal("Place ghost (" + controls.ghost.count() + ")"));
        super.render(graphics,mouseX,mouseY,delta);
    }
    private Component label(String text, boolean active) {
        return Component.literal(text + ": " + (active ? "ON" : "OFF")).withStyle(active ? ChatFormatting.AQUA : ChatFormatting.GRAY);
    }
    @Override public boolean isPauseScreen() { return false; }
}
