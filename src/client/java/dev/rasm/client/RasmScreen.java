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
    private final RasmClient controls;
    private final List<Feature> features = new ArrayList<>();
    private final List<Button> cards = new ArrayList<>();
    private int left, top, panelWidth, panelHeight, contentX, contentWidth, rows, columns, page, pages;
    private String category = "Combat", query = "";
    private EditBox search;
    private Button previous, next;
    private record Feature(String category, String name, String detail, String binding,
                           Runnable action, BooleanSupplier enabled, boolean closes) {
        String id() { return binding.isEmpty() ? name : binding; }
    }

    RasmScreen(RasmClient controls) {
        super(Component.literal("Rasm dashboard"));
        this.controls = controls;
        features.add(new Feature("Combat", "KB Flick", "180 flick + Stun Slam", "flip", () -> controls.quickTurn(minecraft), null, true));
        features.add(new Feature("Combat", "Stun Slam", "Slot 1 > axe 8 > mace 2", "stun_slam", () -> controls.swaps.stunSlam(minecraft), null, true));
        features.add(new Feature("Combat", "Shield Stun", "Slot 1 > axe 8 > slot 1", "shield_stun", () -> controls.swaps.shieldStun(minecraft), null, true));
        features.add(new Feature("Combat", "Lunge Swap", "Slot 1 > spear 3; one attack", "lunge", () -> controls.swaps.lunge(minecraft), null, true));
        features.add(new Feature("Combat", "Mace Swap", "Slot 1 > mace 2 > slot 1", "mace", () -> controls.swaps.mace(minecraft), null, true));
        features.add(new Feature("Movement", "Pearl Catch", "Pearl 5 > wind charge 4", "pearl_catch", () -> controls.swaps.pearlCatch(minecraft), null, true));
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
        features.add(new Feature("Combat", "Auto Stun Slam", "Once per fall; aim at a player", "auto_stun", controls::toggleAutoStun, controls::autoStunActive, false));
        features.add(new Feature("Visuals", "Block ESP", "Choose blocks to highlight", "block_esp", () -> minecraft.setScreen(new BlockEspScreen(this)), BlockEsp::active, false));
    }

    private static String rememberedCategory = "All";
    private static final java.util.Set<String> pinned = new java.util.HashSet<>();
    private int sidebar, resultCount;

    @Override protected void init() {
        category = rememberedCategory;
        panelWidth = Math.min(710, width - 12);
        panelHeight = Math.min(390, height - 12);
        left = (width - panelWidth) / 2; top = (height - panelHeight) / 2;
        sidebar = panelWidth < 390 ? 82 : 102;
        contentX = left + sidebar + 12; contentWidth = panelWidth - sidebar - 24;
        // Single rows at normal GUI scale keep full feature names readable.
        columns = contentWidth >= 480 ? 2 : 1;
        rows = Math.max(1, (panelHeight - 92) / 38);
        int searchWidth = Math.min(248, panelWidth - 166);
        search = new EditBox(font,left+panelWidth-searchWidth-39,top+15,searchWidth-12,16,Component.literal("Search all features"));
        search.setBordered(false); search.setTextColor(RasmUi.TEXT);
        search.setMaxLength(64); search.setHint(Component.literal("Search features...")); search.setValue(query);
        search.setResponder(value -> { query=value; page=0; refreshCards(); }); addRenderableWidget(search);
        addRenderableWidget(new RasmUi.Control(left+panelWidth-31,top+10,22,24,()->"X",this::onClose,()->false,false));
        String[] categories={"All", "Combat", "Movement", "Visuals", "Pinned"};
        int tabStep = Math.min(29, Math.max(20, (panelHeight-102)/5));
        for(int i=0;i<categories.length;i++) {
            String name=categories[i];
            addRenderableWidget(new RasmUi.Control(left+9,top+49+i*tabStep,sidebar-12,tabStep-3,()->name,
                ()->{ category=name; rememberedCategory=name; page=0; search.setValue(""); refreshCards(); },()->category.equals(name)&&query.isBlank(),false));
        }
        addRenderableWidget(new RasmUi.Control(left+9,top+panelHeight-50,sidebar-12,19,()->"Keybinds",
            ()->minecraft.setScreen(new KeyBindsScreen(this,minecraft.options)),()->false,false));
        addRenderableWidget(new RasmUi.Control(left+9,top+panelHeight-27,sidebar-12,19,()->"Panic",
            ()->controls.panic(minecraft),()->false,true)).setTooltip(Tooltip.create(Component.literal("Disable everything • "+controls.bindingLabel("panic"))));
        previous=addRenderableWidget(new RasmUi.Control(contentX+contentWidth-49,top+panelHeight-27,22,19,()->"<",()->{page--;refreshCards();},()->false,false));
        next=addRenderableWidget(new RasmUi.Control(contentX+contentWidth-23,top+panelHeight-27,22,19,()->">",()->{page++;refreshCards();},()->false,false));
        addRenderableWidget(new RasmUi.Control(contentX,top+panelHeight-27,60,19,RasmUi::themeName,RasmUi::nextTheme,()->false,false))
            .setTooltip(Tooltip.create(Component.literal("Change accent color: Mint / Violet / Amber")));
        cards.clear(); refreshCards();
    }

    private void refreshCards() {
        cards.forEach(this::removeWidget); cards.clear();
        String filter=query.toLowerCase(Locale.ROOT).trim();
        List<Feature> shown=features.stream().filter(f -> filter.isEmpty()
            ? category.equals("All") || (category.equals("Pinned") ? pinned.contains(f.id()) : f.category.equals(category))
            : (f.name+" "+f.detail+" "+f.category).toLowerCase(Locale.ROOT).contains(filter)).toList();
        resultCount=shown.size(); int capacity=rows*columns;
        pages=Math.max(1,(shown.size()+capacity-1)/capacity); page=Math.max(0,Math.min(page,pages-1));
        int cardWidth=(contentWidth-(columns-1)*7)/columns;
        for(int i=page*capacity;i<Math.min(shown.size(),(page+1)*capacity);i++) {
            int cell=i-page*capacity; Feature feature=shown.get(i);
            int x=contentX+(cell%columns)*(cardWidth+7), y=top+63+(cell/columns)*38;
            cards.add(addRenderableWidget(new FeatureCard(x,y,cardWidth-45,feature)));
            cards.add(addRenderableWidget(new FeatureSwitch(x+cardWidth-42,y,42,feature)));
        }
        previous.active=page>0; next.active=page+1<pages;
    }

    @Override public void render(GuiGraphics g,int mx,int my,float delta) {
        RasmUi.frame(g,width,height,left,top,panelWidth,panelHeight);
        g.fill(left+sidebar,top+43,left+sidebar+1,top+panelHeight-8,RasmUi.BORDER);
        RasmUi.round(g,left+11,top+12,21,21,RasmUi.blend(RasmUi.SURFACE,RasmUi.accent(),0.2f));
        RasmUi.icon(g,"All",left+13,top+14,RasmUi.accent());
        g.drawString(font,"R A S M",left+40,top+12,RasmUi.TEXT,false);
        RasmUi.small(g,"CONTROL CENTER",left+40,top+25,95,RasmUi.MUTED);
        int searchWidth=Math.min(248,panelWidth-166);
        RasmUi.box(g,left+panelWidth-searchWidth-45,top+9,searchWidth,25,RasmUi.SURFACE,search.isFocused()?RasmUi.accent():RasmUi.BORDER);
        String title=query.isBlank()?category:"Search results";
        g.drawString(font,title,contentX,top+47,RasmUi.TEXT,false);
        String count=resultCount+(resultCount==1?" feature":" features");
        g.drawString(font,count,contentX+contentWidth-font.width(count),top+47,RasmUi.MUTED,false);
        if(cards.isEmpty()) {
            g.drawString(font,category.equals("Pinned")&&query.isBlank()?"Your favorites go here":"No matching features",contentX+8,top+77,RasmUi.TEXT,false);
            RasmUi.text(g,"Right-click a feature to pin it.",contentX+8,top+94,contentWidth-16,RasmUi.MUTED);
        }
        RasmUi.small(g,"MC 1.21.11",left+12,top+38,sidebar-14,RasmUi.DIM);
        g.drawString(font,(page+1)+" / "+pages,contentX+contentWidth-90,top+panelHeight-21,RasmUi.MUTED,false);
        super.render(g,mx,my,delta);
    }

    private final class FeatureCard extends Button {
        private final Feature feature;
        private float hover;
        private long last=System.nanoTime();
        FeatureCard(int x,int y,int w,Feature feature) {
            super(x,y,w,33,Component.literal(feature.name),b->{
                if(!controls.featureEnabled(feature.id())) return;
                if(feature.closes) Minecraft.getInstance().setScreen(null);
                feature.action.run();
            },DEFAULT_NARRATION);
            this.feature=feature;
            setTooltipDelay(java.time.Duration.ofMillis(450));
            setTooltip(Tooltip.create(Component.literal(feature.detail+"\nKey: "+(feature.binding.isEmpty()?"Menu":controls.bindingLabel(feature.binding))+"\nRight-click to pin / unpin. Switch OFF blocks the hotkey.")));
        }
        @Override protected boolean isValidClickButton(net.minecraft.client.input.MouseButtonInfo button) {
            return button.button()==0 || button.button()==1;
        }
        @Override public void onClick(net.minecraft.client.input.MouseButtonEvent event,boolean doubleClick) {
            if(event.button()==1) {
                if(!pinned.remove(feature.id())) pinned.add(feature.id());
                if(category.equals("Pinned")) refreshCards();
            } else super.onClick(event,doubleClick);
        }
        @Override protected void renderContents(GuiGraphics g,int mx,int my,float dt) {
            boolean permitted=controls.featureEnabled(feature.id());
            boolean running=permitted && feature.enabled!=null && feature.enabled.getAsBoolean();
            long now=System.nanoTime(); float step=Math.min(1,(now-last)/100_000_000f);last=now;
            hover+=((isHoveredOrFocused()?1:0)-hover)*step;
            int x=getX(),y=getY(),w=getWidth();
            RasmUi.box(g,x,y,w,33,RasmUi.blend(RasmUi.SURFACE,RasmUi.accent(),hover*0.07f),
                running?RasmUi.blend(RasmUi.BORDER,RasmUi.accent(),0.6f):RasmUi.BORDER);
            if(pinned.contains(feature.id())) g.fill(x+2,y+5,x+4,y+28,RasmUi.accent());
            RasmUi.icon(g,feature.category,x+7,y+8,permitted?RasmUi.accent():RasmUi.DIM);
            String status=!permitted?"LOCK":feature.enabled==null?"RUN":running?"ON":"OFF";
            if(feature.id().equals("block_esp")||feature.name.equals("Custom Glow")) status=permitted?"OPEN":"LOCK";
            int statusWidth=font.width(status)+10;
            RasmUi.round(g,x+w-statusWidth-6,y+8,statusWidth,16,running?RasmUi.blend(RasmUi.SURFACE,RasmUi.accent(),0.2f):0xFF111824);
            g.drawString(font,status,x+w-statusWidth-1,y+12,running?RasmUi.accent():RasmUi.MUTED,false);
            RasmUi.text(g,feature.name,x+28,y+6,w-statusWidth-40,permitted?RasmUi.TEXT:RasmUi.DIM);
            String key=feature.binding.isEmpty()?"":controls.bindingLabel(feature.binding)+"  /  ";
            RasmUi.small(g,key+feature.detail,x+28,y+20,w-statusWidth-40,RasmUi.MUTED);
        }
    }

    private final class FeatureSwitch extends Button {
        private final Feature feature;
        FeatureSwitch(int x,int y,int w,Feature feature) {
            super(x,y,w,33,Component.literal("Enable or disable "+feature.name),b->controls.toggleFeature(feature.id()),DEFAULT_NARRATION);
            this.feature=feature;
            setTooltipDelay(java.time.Duration.ofMillis(450));
            setTooltip(Tooltip.create(Component.literal("Master switch: OFF stops this feature and blocks its key. ON allows it.")));
        }
        @Override protected void renderContents(GuiGraphics g,int mx,int my,float dt) {
            boolean on=controls.featureEnabled(feature.id());
            int x=getX(),y=getY();
            RasmUi.box(g,x,y,getWidth(),33,0xFF141C2A,isHoveredOrFocused()?RasmUi.accent():RasmUi.BORDER);
            RasmUi.round(g,x+9,y+5,24,11,on?RasmUi.blend(RasmUi.SURFACE,RasmUi.accent(),0.5f):0xFF354056);
            RasmUi.round(g,x+(on?23:11),y+7,8,7,on?RasmUi.accent():RasmUi.MUTED);
            RasmUi.small(g,on?"ON":"OFF",x+14,y+22,25,on?RasmUi.accent():RasmUi.MUTED);
        }
    }
    @Override public boolean isPauseScreen() { return false; }
}
