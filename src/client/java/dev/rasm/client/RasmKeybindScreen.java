package dev.rasm.client;

import java.util.List;
import java.util.Locale;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/** Rebind every registered Rasm control without leaving the dashboard. */
final class RasmKeybindScreen extends Screen {
    private final Screen parent;
    private final List<KeyMapping> bindings;
    private KeyMapping listening;
    private int left, top, panelWidth, panelHeight, page, rows, pages;
    private String query = "";
    private EditBox search;
    RasmKeybindScreen(Screen parent, RasmClient controls) {
        super(Component.literal("DiddyMore keybinds"));
        this.parent = parent; bindings = controls.bindings();
    }
    @Override protected void init() {
        panelWidth = Math.min(600, width - 12); panelHeight = Math.min(390, height - 12);
        left = (width-panelWidth)/2; top = (height-panelHeight)/2;
        rows = Math.max(1, (panelHeight-108)/30);
        build();
    }
    private void build() {
        clearWidgets();
        search = new EditBox(font,left+12,top+36,panelWidth-24,18,Component.literal("Search keybinds"));
        search.setHint(Component.literal("Search controls...")); search.setValue(query);
        search.setResponder(value -> { query=value; page=0; build(); setFocused(search); });
        addRenderableWidget(search);
        var shown = bindings.stream().filter(k -> Component.translatable(k.getName()).getString()
            .toLowerCase(Locale.ROOT).contains(query.toLowerCase(Locale.ROOT))).toList();
        pages = Math.max(1, (shown.size()+rows-1)/rows); page = Math.min(page,pages-1);
        for (int i=page*rows; i<Math.min(shown.size(),(page+1)*rows); i++) {
            KeyMapping binding = shown.get(i); int y=top+62+(i-page*rows)*30;
            addRenderableWidget(new RasmUi.Control(left+panelWidth-160,y,98,25,
                () -> listening==binding ? "Press a key..." : binding.getTranslatedKeyMessage().getString(),
                () -> { listening=binding; setFocused(null); }, () -> listening==binding, conflict(binding)))
                .setTooltip(Tooltip.create(Component.literal("Click, then press a key or mouse button. Backspace clears; Esc cancels. Red means another control uses this key.")));
            addRenderableWidget(new RasmUi.Control(left+panelWidth-57,y,45,25,()->"Reset",
                () -> { listening=binding; assign(binding.getDefaultKey()); },()->false,false));
        }
        addRenderableWidget(new RasmUi.Control(left+panelWidth-34,top+9,22,22,()->"X",this::onClose,()->false,false));
        addRenderableWidget(new RasmUi.Control(left+12,top+panelHeight-28,62,20,()->"Back",this::onClose,()->false,false));
        var prev=addRenderableWidget(new RasmUi.Control(left+panelWidth-62,top+panelHeight-28,22,20,()->"<",()->{page--;build();},()->false,false));
        var next=addRenderableWidget(new RasmUi.Control(left+panelWidth-34,top+panelHeight-28,22,20,()->">",()->{page++;build();},()->false,false));
        prev.active=page>0; next.active=page+1<pages;
    }
    private boolean conflict(KeyMapping binding) {
        if(binding.isUnbound()) return false;
        for(KeyMapping other : minecraft.options.keyMappings) if(other!=binding && binding.same(other)) return true;
        return false;
    }
    private void assign(InputConstants.Key key) {
        listening.setKey(key); listening.setDown(false); listening=null;
        KeyMapping.resetMapping(); minecraft.options.save(); build();
    }
    @Override public boolean keyPressed(KeyEvent event) {
        if(listening!=null) {
            if(event.key()==GLFW.GLFW_KEY_ESCAPE) { listening=null; build(); }
            else assign(event.key()==GLFW.GLFW_KEY_BACKSPACE ? InputConstants.UNKNOWN : InputConstants.getKey(event));
            return true;
        }
        return super.keyPressed(event);
    }
    @Override public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if(listening!=null) { assign(InputConstants.Type.MOUSE.getOrCreate(event.button())); return true; }
        return super.mouseClicked(event,doubleClick);
    }
    @Override public void render(GuiGraphics g,int mx,int my,float delta) {
        RasmUi.frame(g,width,height,left,top,panelWidth,panelHeight);
        g.drawString(font,"KEYBINDS",left+12,top+16,RasmUi.TEXT,false);
        var shown=bindings.stream().filter(k -> Component.translatable(k.getName()).getString()
            .toLowerCase(Locale.ROOT).contains(query.toLowerCase(Locale.ROOT))).toList();
        for(int i=page*rows;i<Math.min(shown.size(),(page+1)*rows);i++) {
            var binding=shown.get(i); int y=top+62+(i-page*rows)*30;
            RasmUi.text(g,Component.translatable(binding.getName()).getString(),left+12,y+9,panelWidth-183,
                conflict(binding)?RasmUi.RED:RasmUi.TEXT);
        }
        RasmUi.small(g,listening==null?"Click a binding to change it. Changes save automatically.":"Press a key / mouse button. Backspace: clear. Esc: cancel.",
            left+12,top+panelHeight-44,panelWidth-24,RasmUi.MUTED);
        g.drawString(font,(page+1)+" / "+pages,left+panelWidth-102,top+panelHeight-22,RasmUi.MUTED,false);
        super.render(g,mx,my,delta);
    }
    @Override public void onClose() { listening=null; minecraft.setScreen(parent); }
    @Override public boolean isPauseScreen() { return false; }
}
