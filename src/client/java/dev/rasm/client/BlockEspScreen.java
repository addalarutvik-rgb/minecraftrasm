package dev.rasm.client;

import java.util.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Block;

final class BlockEspScreen extends Screen {
    private final Screen parent;
    private final List<Button> entries = new ArrayList<>();
    private final List<Block> blocks = BuiltInRegistries.BLOCK.stream()
        .sorted(Comparator.comparing(b -> BuiltInRegistries.BLOCK.getKey(b).toString())).toList();
    private String query = "";
    private int left, top, panelWidth, panelHeight, page, pages, rows;
    private Button previous, next, toggle;
    BlockEspScreen(Screen parent) { super(Component.literal("Block ESP")); this.parent = parent; }
    @Override protected void init() {
        panelWidth = Math.min(510, width - 16); panelHeight = Math.min(330, height - 16);
        left = (width-panelWidth)/2; top = (height-panelHeight)/2;
        rows = Math.max(1, (panelHeight - 120)/25);
        EditBox search = new EditBox(font,left+12,top+34,panelWidth-24,20,Component.literal("Search every registered block"));
        search.setHint(Component.literal("Search name or minecraft:block_id..."));
        search.setValue(query);
        search.setResponder(value -> { query=value; page=0; refresh(); });
        addRenderableWidget(search);
        toggle = addRenderableWidget(Button.builder(Component.literal("ESP"), b -> BlockEsp.toggle()).bounds(left+12,top+panelHeight-52,92,20).build());
        addRenderableWidget(Button.builder(Component.literal("Clear selections"),b -> { BlockEsp.clear(); refresh(); }).bounds(left+110,top+panelHeight-52,110,20).build());
        previous = addRenderableWidget(Button.builder(Component.literal("<"),b -> { page--; refresh(); }).bounds(left+panelWidth-70,top+panelHeight-52,26,20).build());
        next = addRenderableWidget(Button.builder(Component.literal(">"),b -> { page++; refresh(); }).bounds(left+panelWidth-38,top+panelHeight-52,26,20).build());
        addRenderableWidget(Button.builder(Component.literal("Back"),b -> onClose()).bounds(left+panelWidth-70,top+panelHeight-26,58,20).build());
        entries.clear(); refresh();
    }
    private void refresh() {
        entries.forEach(this::removeWidget); entries.clear();
        String filter = query.toLowerCase(Locale.ROOT).trim();
        List<Block> found = blocks.stream().filter(b -> (BuiltInRegistries.BLOCK.getKey(b) + " " + b.getName().getString()).toLowerCase(Locale.ROOT).contains(filter)).toList();
        pages = Math.max(1,(found.size()+rows-1)/rows); page=Math.max(0,Math.min(page,pages-1));
        for (int i=page*rows; i<Math.min(found.size(),(page+1)*rows); i++) {
            Block block=found.get(i);
            String id=BuiltInRegistries.BLOCK.getKey(block).toString();
            String label=(BlockEsp.selected(block)?"[x] ":"[ ] ")+block.getName().getString()+"  /  "+id;
            Button button=Button.builder(Component.literal(font.plainSubstrByWidth(label,panelWidth-42)), b -> { BlockEsp.select(block); refresh(); })
                .bounds(left+12,top+62+(i-page*rows)*25,panelWidth-24,22).build();
            button.setTooltip(Tooltip.create(Component.literal(id + " — click to select/unselect")));
            entries.add(addRenderableWidget(button));
        }
        previous.active=page>0; next.active=page+1<pages;
    }
    @Override public void render(GuiGraphics g,int mx,int my,float dt) {
        g.fill(0,0,width,height,0xA5090E17); g.fill(left,top,left+panelWidth,top+panelHeight,0xFF101925);
        g.fill(left,top,left+panelWidth,top+2,0xFF63E3C2);
        g.drawString(font,"BLOCK ESP",left+12,top+13,0xFFE8EEF8,false);
        g.drawString(font,(page+1)+" / "+pages+"  |  Selected: "+BlockEsp.selectedCount(),left+120,top+13,0xFF8C9CB4,false);
        g.drawString(font,"24 blocks / 128 markers max",left+12,top+panelHeight-20,0xFF8C9CB4,false);
        if(entries.isEmpty()) g.drawString(font,"No matching blocks",left+16,top+68,0xFF8C9CB4,false);
        toggle.setMessage(Component.literal("ESP: "+(BlockEsp.active()?"ON":"OFF")));
        super.render(g,mx,my,dt);
    }
    @Override public void onClose() { minecraft.setScreen(parent); }
    @Override public boolean isPauseScreen() { return false; }
}
