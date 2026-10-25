package dev.rasm.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.BooleanSupplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.controls.KeyBindsScreen;
import net.minecraft.network.chat.Component;

/** Compact dashboard; categories and pagination keep every control reachable at high GUI scale. */
final class RasmScreen extends Screen {
    private static final int TEXT = 0xFFE8EEF8, MUTED = 0xFF8C9CB4, ACCENT = 0xFF63E3C2;
    private final RasmClient controls;
    private final List<Feature> features = new ArrayList<>();
    private final List<Button> cards = new ArrayList<>();
    private int left, top, panelWidth, panelHeight, contentX, contentWidth, rows, columns, page, pages;
    private String category = "Combat", query = "";
    private EditBox search;
    private Button previous, next;
    private record Feature(String category, String name, String detail, String binding,
                           Runnable action, BooleanSupplier enabled, boolean closes) {}

    RasmScreen(RasmClient controls) {
        super(Component.literal("Rasm dashboard"));
        this.controls = controls;
        features.add(new Feature("Combat", "KB Flick", "180 flick + Stun Slam", "flip", () -> controls.quickTurn(minecraft), null, true));
        features.add(new Feature("Combat", "Stun Slam", "Slot 1 > axe 8 > mace 2", "stun_slam", () -> controls.swaps.stunSlam(minecraft), null, true));
        features.add(new Feature("Combat", "Shield Stun", "Slot 1 > axe 8 > slot 1", "shield_stun", () -> controls.swaps.shieldStun(minecraft), null, true));
        features.add(new Feature("Combat", "Lunge Swap", "Slot 1 > spear 3; one attack", "lunge", () -> controls.swaps.lunge(minecraft), null, true));
        features.add(new Feature("Combat", "Mace Swap", "Slot 1 > mace 2 > slot 1", "mace", () -> controls.swaps.mace(minecraft), null, true));
        features.add(new Feature("Movement", "Flight", "Camera-relative flight", "flight", () -> controls.toggleFlight(minecraft), controls::clientFlightActive, false));
        features.add(new Feature("Movement", "NoFall", "Toggle fall packet handling", "nofall", () -> NoFall.toggle(minecraft), NoFall::active, false));
        features.add(new Feature("Movement", "Server Flight", "Requires server permission", "server_flight", () -> controls.requestServerFlight(minecraft), null, false));
        features.add(new Feature("Visuals", "X-ray", "Reveal full ore blocks", "xray", () -> Xray.toggle(minecraft), Xray::active, false));
        features.add(new Feature("Visuals", "Ghost Block", "Place glass beneath your feet", "ghost", () -> controls.ghost.place(minecraft), null, false));
        features.add(new Feature("Visuals", "Clear Ghosts", "Remove every ghost block", "", () -> controls.ghost.clear(), null, false));
        features.add(new Feature("Visuals", "Glow Players", "Highlight all loaded players", "", () -> EntityGlow.toggleGroup("players"), () -> EntityGlow.groupActive("players"), false));
        features.add(new Feature("Visuals", "Glow Hostile", "Highlight hostile mobs", "", () -> EntityGlow.toggleGroup("hostile"), () -> EntityGlow.groupActive("hostile"), false));
        features.add(new Feature("Visuals", "Glow Passive", "Highlight passive / neutral mobs", "", () -> EntityGlow.toggleGroup("passive"), () -> EntityGlow.groupActive("passive"), false));
        features.add(new Feature("Visuals", "Custom Glow", "Choose a player or entity name", "", () -> minecraft.setScreen(new net.minecraft.client.gui.screens.ChatScreen("/glow ", false)), null, false));
        features.add(new Feature("Visuals", "Clear Glow", "Remove all custom glow filters", "", EntityGlow::reset, null, false));
    }

    @Override protected void init() {
        panelWidth = Math.min(570, width - 12);
        panelHeight = Math.min(326, height - 12);
        left = (width - panelWidth) / 2;
        top = (height - panelHeight) / 2;
        int sidebar = panelWidth < 400 ? 88 : 108;
        contentX = left + sidebar + 14;
        contentWidth = panelWidth - sidebar - 28;
        columns = contentWidth >= 240 ? 2 : 1;
        rows = Math.max(1, (panelHeight - 99) / 62);
        search = new EditBox(font, contentX, top + 17, contentWidth, 22, Component.literal("Search features"));
        search.setMaxLength(64);
        search.setHint(Component.literal("Search features..."));
        search.setValue(query);
        search.setResponder(value -> { query = value; page = 0; refreshCards(); });
        addRenderableWidget(search);
        String[] categories = {"Combat", "Movement", "Visuals"};
        for (int i = 0; i < categories.length; i++) {
            String name = categories[i];
            addRenderableWidget(new FlatButton(left + 8, top + 60 + i * 30, sidebar - 12, 28, name,
                () -> { category = name; page = 0; search.setValue(""); refreshCards(); }, () -> category.equals(name)));
        }
        addRenderableWidget(new FlatButton(left + 8, top + panelHeight - 70, sidebar - 12, 26, "Keybinds",
            () -> minecraft.setScreen(new KeyBindsScreen(this, minecraft.options)), () -> false));
        addRenderableWidget(new FlatButton(left + 8, top + panelHeight - 38, sidebar - 12, 26, "PANIC", () -> controls.panic(minecraft), () -> false));
        previous = addRenderableWidget(new FlatButton(contentX + contentWidth - 60, top + panelHeight - 36, 26, 22, "<",
            () -> { page--; refreshCards(); }, () -> false));
        next = addRenderableWidget(new FlatButton(contentX + contentWidth - 28, top + panelHeight - 36, 26, 22, ">",
            () -> { page++; refreshCards(); }, () -> false));
        cards.clear();
        refreshCards();
    }

    private void refreshCards() {
        cards.forEach(this::removeWidget);
        cards.clear();
        String filter = query.toLowerCase(Locale.ROOT).trim();
        List<Feature> shown = features.stream().filter(f -> filter.isEmpty() ? f.category.equals(category)
            : (f.name + " " + f.detail + " " + f.category).toLowerCase(Locale.ROOT).contains(filter)).toList();
        int capacity = rows * columns;
        pages = Math.max(1, (shown.size() + capacity - 1) / capacity);
        page = Math.max(0, Math.min(page, pages - 1));
        int cardWidth = (contentWidth - (columns - 1) * 8) / columns;
        for (int i = page * capacity; i < Math.min(shown.size(), (page + 1) * capacity); i++) {
            int cell = i - page * capacity;
            Feature feature = shown.get(i);
            Button card = new FeatureCard(contentX + (cell % columns) * (cardWidth + 8), top + 64 + (cell / columns) * 62, cardWidth, feature);
            cards.add(addRenderableWidget(card));
        }
        previous.active = page > 0;
        next.active = page + 1 < pages;
    }

    @Override public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        g.fill(0, 0, width, height, 0xA5090E17);
        g.fill(left - 3, top + 4, left + panelWidth + 3, top + panelHeight + 4, 0x70000000);
        g.fill(left, top, left + panelWidth, top + panelHeight, 0xFF101925);
        g.fill(left, top, contentX - 14, top + panelHeight, 0xFF0C131D);
        g.fill(left, top, left + panelWidth, top + 2, ACCENT);
        g.drawString(font, "R A S M", left + 13, top + 18, TEXT, false);
        g.drawString(font, "1.21.11", left + 13, top + 33, MUTED, false);
        g.drawString(font, query.isBlank() ? category.toUpperCase(Locale.ROOT) : "SEARCH RESULTS", contentX, top + 49, MUTED, false);
        g.fill(contentX, top + panelHeight - 43, left + panelWidth - 14, top + panelHeight - 42, 0xFF243246);
        String footer = category.equals("Visuals") && query.isBlank() ? "Ghosts: " + controls.ghost.count() + "  /glow <name>" : "Select a feature  /  Esc to close";
        g.drawString(font, font.plainSubstrByWidth(footer, contentWidth - 110), contentX, top + panelHeight - 29, MUTED, false);
        g.drawString(font, (page + 1) + "/" + pages, contentX + contentWidth - 96, top + panelHeight - 29, MUTED, false);
        if (cards.isEmpty()) g.drawString(font, "No matching features", contentX + 10, top + 85, MUTED, false);
        super.render(g, mouseX, mouseY, delta);
    }

    private final class FeatureCard extends Button {
        private final Feature feature;
        FeatureCard(int x, int y, int w, Feature feature) {
            super(x, y, w, 54, Component.literal(feature.name), b -> {
                if (feature.closes) Minecraft.getInstance().setScreen(null);
                feature.action.run();
            }, DEFAULT_NARRATION);
            this.feature = feature;
            setTooltip(Tooltip.create(Component.literal(feature.detail + (feature.binding.isEmpty() ? "" : " | Key: " + controls.bindingLabel(feature.binding)))));
        }
        @Override protected void renderContents(GuiGraphics g, int mx, int my, float dt) {
            boolean on = feature.enabled != null && feature.enabled.getAsBoolean();
            boolean hover = isHoveredOrFocused();
            int x = getX(), y = getY(), w = getWidth();
            g.fill(x, y, x + w, y + 54, hover ? 0xFF293A4E : 0xFF1A2839);
            g.fill(x, y, x + 2, y + 54, on || hover ? ACCENT : 0xFF34465C);
            String status = feature.enabled == null ? "RUN" : on ? "ON" : "OFF";
            int statusWidth = font.width(status);
            g.drawString(font, status, x + w - statusWidth - 9, y + 10, on ? ACCENT : MUTED, false);
            g.drawString(font, font.plainSubstrByWidth(feature.name, w - statusWidth - 30), x + 10, y + 10, TEXT, false);
            g.drawString(font, font.plainSubstrByWidth(feature.detail, w - 20), x + 10, y + 26, MUTED, false);
            String key = feature.binding.isEmpty() ? "UTILITY" : controls.bindingLabel(feature.binding);
            g.drawString(font, font.plainSubstrByWidth(key, w - 20), x + 10, y + 40, 0xFF65B9D8, false);
        }
    }

    private final class FlatButton extends Button {
        private final BooleanSupplier selected;
        FlatButton(int x, int y, int w, int h, String name, Runnable action, BooleanSupplier selected) {
            super(x, y, w, h, Component.literal(name), b -> action.run(), DEFAULT_NARRATION);
            this.selected = selected;
        }
        @Override protected void renderContents(GuiGraphics g, int mx, int my, float dt) {
            boolean current = selected.getAsBoolean();
            int x = getX(), y = getY();
            g.fill(x, y, x + getWidth(), y + getHeight(), current ? 0xFF213F43 : isHoveredOrFocused() && active ? 0xFF243449 : 0xFF162130);
            if (current) g.fill(x, y + 5, x + 2, y + getHeight() - 5, ACCENT);
            g.drawCenteredString(font, getMessage(), x + getWidth() / 2, y + (getHeight() - 8) / 2, !active ? 0xFF48576B : getMessage().getString().equals("PANIC") ? 0xFFFF818B : current ? ACCENT : TEXT);
        }
    }
    @Override public boolean isPauseScreen() { return false; }
}
