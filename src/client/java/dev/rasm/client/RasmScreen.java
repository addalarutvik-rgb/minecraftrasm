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
        super(Component.literal("DiddyMore dashboard"));
        this.controls = controls;
        features.add(new Feature("Combat", "KB Flick", "Smooth 180 and back + Stun Slam", "flip", () -> controls.quickTurn(minecraft), null, true));
        features.add(new Feature("Combat", "Stun Slam", "Slot 1 > axe 8 > mace 2", "stun_slam", () -> controls.swaps.stunSlam(minecraft), null, true));
        features.add(new Feature("Combat", "Auto Stun", "Ground: axe shield stun / falling: mace", "auto_stun", controls::toggleAutoStun, controls::autoStunActive, false));
        features.add(new Feature("Combat", "Auto Shield Stun", "Aimed blocking player; pauses when busy", "auto_shield", controls::toggleAutoShield, controls::autoShieldActive, false));
        features.add(new Feature("Combat", "Shield Stun", "Slot 1 > axe 8 > slot 1", "shield_stun", () -> controls.swaps.shieldStun(minecraft), null, true));
        features.add(new Feature("Combat", "Lunge Swap", "Slot 1 > spear 3; one attack", "lunge", () -> controls.swaps.lunge(minecraft), null, true));
        features.add(new Feature("Combat", "Mace Swap", "Slot 1 > mace 2 > slot 1", "mace", () -> controls.swaps.mace(minecraft), null, true));
        features.add(new Feature("Movement", "Pearl Catch", "Pearl 5 > wind charge 4", "pearl_catch", () -> controls.swaps.pearlCatch(minecraft), null, true));
        features.add(new Feature("Movement", "Movement Recorder", "Record clips, checkpoints and sequences", "movement_replay", () -> minecraft.setScreen(new MovementScreen(this)), MovementRecorder.INSTANCE::active, false));
        features.add(new Feature("Movement", "Flight", "Camera-relative flight", "flight", () -> controls.toggleFlight(minecraft), controls::clientFlightActive, false));
        features.add(new Feature("Movement", "NoFall", "Toggle fall packet handling", "nofall", () -> NoFall.toggle(minecraft), NoFall::active, false));
        features.add(new Feature("Movement", "Server Flight", "Requires server permission", "server_flight", () -> controls.requestServerFlight(minecraft), null, false));
        features.add(new Feature("Visuals", "Clear Vision", "Remove Blindness and Darkness visuals", "clear_vision", ClearVision::toggle, ClearVision::active, false));
        features.add(new Feature("Visuals", "X-ray", "Reveal full ore blocks", "xray", () -> Xray.toggle(minecraft), Xray::active, false));
        features.add(new Feature("Visuals", "Ghost Block", "Place glass beneath your feet", "ghost", () -> controls.ghost.place(minecraft), null, false));
        features.add(new Feature("Visuals", "Clear Ghosts", "Remove every ghost block", "", () -> controls.ghost.clear(), null, false));
        features.add(new Feature("Visuals", "Glow Players", "Highlight all loaded players", "", () -> EntityGlow.toggleGroup("players"), () -> EntityGlow.groupActive("players"), false));
        features.add(new Feature("Visuals", "Glow Hostile", "Highlight hostile mobs", "", () -> EntityGlow.toggleGroup("hostile"), () -> EntityGlow.groupActive("hostile"), false));
        features.add(new Feature("Visuals", "Glow Passive", "Highlight passive / neutral mobs", "", () -> EntityGlow.toggleGroup("passive"), () -> EntityGlow.groupActive("passive"), false));
        features.add(new Feature("Visuals", "Custom Glow", "Choose a player or entity name", "", () -> minecraft.setScreen(new net.minecraft.client.gui.screens.ChatScreen("/glow ", false)), null, false));
        features.add(new Feature("Visuals", "Clear Glow", "Remove all custom glow filters", "", EntityGlow::reset, null, false));
        features.add(new Feature("Visuals", "Block ESP", "Choose blocks to highlight", "block_esp", () -> minecraft.setScreen(new BlockEspScreen(this)), BlockEsp::active, false));
    }

    private static String rememberedCategory = "All";
    private static final java.util.Set<String> pinned = new java.util.HashSet<>();
    private int sidebar, resultCount, cardHeight, rowStep;
    private boolean activeOnly;

    @Override protected void init() {
        category=rememberedCategory;
        panelWidth=Math.min(850,width-12); panelHeight=Math.min(460,height-12);
        left=(width-panelWidth)/2; top=(height-panelHeight)/2;
        sidebar=panelWidth<390?82:108;
        contentX=left+sidebar+13; contentWidth=panelWidth-sidebar-26;
        columns=contentWidth>=460?2:1;
        cardHeight=panelHeight>=300?44:34; rowStep=cardHeight+6;
        rows=Math.max(1,(panelHeight-104)/rowStep);
        int searchWidth=contentWidth-72;
        search=new EditBox(font,contentX+9,top+48,searchWidth-18,16,Component.literal("Search all features"));
        search.setBordered(false); search.setTextColor(RasmUi.TEXT);
        search.setMaxLength(64);search.setHint(Component.literal("Search features..."));search.setValue(query);
        search.setResponder(value->{query=value;page=0;refreshCards();});addRenderableWidget(search);
        addRenderableWidget(new RasmUi.Control(contentX+contentWidth-66,top+42,66,24,()->activeOnly?"Active only":"All states",
            ()->{activeOnly=!activeOnly;page=0;refreshCards();},()->activeOnly,false));
        addRenderableWidget(new RasmUi.Control(left+panelWidth-30,top+9,19,20,()->"X",this::onClose,()->false,false));
        String[] categories={"All","Combat","Movement","Visuals","Pinned"};
        int step=Math.min(32,Math.max(18,(panelHeight-126)/5));
        for(int i=0;i<categories.length;i++) addRenderableWidget(new Navigation(left+8,top+72+i*step,sidebar-15,step-3,categories[i]));
        addRenderableWidget(new RasmUi.Control(left+9,top+panelHeight-48,sidebar-18,19,()->"Keybinds",
            ()->minecraft.setScreen(new RasmKeybindScreen(this,controls)),()->false,false));
        addRenderableWidget(new RasmUi.Control(left+9,top+panelHeight-25,sidebar-18,19,()->"Panic",
            ()->controls.panic(minecraft),()->false,true)).setTooltip(Tooltip.create(Component.literal("Disable everything • "+controls.bindingLabel("panic"))));
        previous=addRenderableWidget(new RasmUi.Control(contentX+contentWidth-45,top+panelHeight-25,20,19,()->"<",()->{page--;refreshCards();},()->false,false));
        next=addRenderableWidget(new RasmUi.Control(contentX+contentWidth-20,top+panelHeight-25,20,19,()->">",()->{page++;refreshCards();},()->false,false));
        addRenderableWidget(new RasmUi.Control(contentX,top+panelHeight-25,64,19,RasmUi::themeName,RasmUi::nextTheme,()->false,false))
            .setTooltip(Tooltip.create(Component.literal("Accent: Mint / Violet / Amber / Rose / Ice")));
        cards.clear();refreshCards();
    }
    private boolean running(Feature f) { return controls.featureEnabled(f.id()) && f.enabled!=null && f.enabled.getAsBoolean(); }
    private final class Navigation extends Button {
        private final String name;
        Navigation(int x,int y,int w,int h,String name) {
            super(x,y,w,h,Component.literal(name),b->{category=name;rememberedCategory=name;page=0;search.setValue("");refreshCards();},DEFAULT_NARRATION);
            this.name=name;
        }
        @Override protected void renderContents(GuiGraphics g,int mx,int my,float dt) {
            boolean selected=category.equals(name)&&query.isBlank();int x=getX(),y=getY();
            if(selected||isHoveredOrFocused()) RasmUi.round(g,x,y,getWidth(),getHeight(),RasmUi.blend(RasmUi.PANEL,RasmUi.accent(),selected?0.16f:0.07f));
            if(selected) g.fill(x,y+4,x+2,y+getHeight()-4,RasmUi.accent());
            int inset=getWidth()<80?7:25;
            if(inset==25) RasmUi.icon(g,name,x+5,y+(getHeight()-16)/2,selected?RasmUi.accent():RasmUi.DIM);
            RasmUi.text(g,name,x+inset,y+(getHeight()-8)/2,getWidth()-inset-4,selected?RasmUi.TEXT:RasmUi.MUTED);
        }
    }

    private void refreshCards() {
        cards.forEach(this::removeWidget); cards.clear();
        String filter=query.toLowerCase(Locale.ROOT).trim();
        List<Feature> shown=features.stream().filter(f -> !activeOnly || running(f)).filter(f -> filter.isEmpty()
            ? category.equals("All") || (category.equals("Pinned") ? pinned.contains(f.id()) : f.category.equals(category))
            : (f.name+" "+f.detail+" "+f.category).toLowerCase(Locale.ROOT).contains(filter)).toList();
        resultCount=shown.size(); int capacity=rows*columns;
        pages=Math.max(1,(shown.size()+capacity-1)/capacity); page=Math.max(0,Math.min(page,pages-1));
        int cardWidth=(contentWidth-(columns-1)*7)/columns;
        for(int i=page*capacity;i<Math.min(shown.size(),(page+1)*capacity);i++) {
            int cell=i-page*capacity; Feature feature=shown.get(i);
            int x=contentX+(cell%columns)*(cardWidth+7), y=top+74+(cell/columns)*rowStep;
            cards.add(addRenderableWidget(new FeatureCard(x,y,cardWidth-33,feature)));
            cards.add(addRenderableWidget(new FeatureSwitch(x+cardWidth-29,y,29,feature)));
        }
        previous.active=page>0; next.active=page+1<pages;
    }

    @Override public void render(GuiGraphics g,int mx,int my,float delta) {
        RasmUi.frame(g,width,height,left,top,panelWidth,panelHeight);
        RasmUi.round(g,left+4,top+4,sidebar-4,panelHeight-8,0xFF10131F);
        g.fill(left+sidebar,top+12,left+sidebar+1,top+panelHeight-12,RasmUi.BORDER);
        RasmUi.box(g,left+12,top+12,27,25,RasmUi.blend(RasmUi.PANEL,RasmUi.accent(),0.18f),RasmUi.blend(RasmUi.BORDER,RasmUi.accent(),0.5f));
        g.drawString(font,"DM",left+19,top+21,RasmUi.accent(),false);
        g.drawString(font,"DiddyMore",left+12,top+44,RasmUi.TEXT,false);
        RasmUi.small(g,"CLIENT / 1.21.11",left+12,top+57,sidebar-20,RasmUi.DIM);
        String title=query.isBlank()?category:"Search results";
        g.drawString(font,title,contentX,top+14,RasmUi.TEXT,false);
        RasmUi.small(g,"YOUR CLIENT. YOUR CONTROLS.",contentX,top+28,contentWidth-35,RasmUi.MUTED);
        RasmUi.box(g,contentX,top+42,contentWidth-72,24,0xFF0C0F19,search.isFocused()?RasmUi.accent():RasmUi.BORDER);
        if(cards.isEmpty()) {
            RasmUi.icon(g,category,contentX+8,top+90,RasmUi.accent());
            RasmUi.text(g,activeOnly?"No active features":category.equals("Pinned")?"Keep your favorites here":"No matching features",contentX+30,top+94,contentWidth-38,RasmUi.TEXT);
            RasmUi.small(g,activeOnly?"Switch to All states to see every control.":"Right-click a feature to pin or unpin it.",contentX+8,top+116,contentWidth-16,RasmUi.MUTED);
        }
        g.fill(contentX,top+panelHeight-32,contentX+contentWidth,top+panelHeight-31,RasmUi.BORDER);
        if(contentWidth>280) RasmUi.small(g,resultCount+(resultCount==1?" feature  /  ":" features  /  ")+features.stream().filter(this::running).count()+" active",contentX+74,top+panelHeight-18,contentWidth-162,RasmUi.MUTED);
        g.drawString(font,(page+1)+" / "+pages,contentX+contentWidth-85,top+panelHeight-19,RasmUi.MUTED,false);
        super.render(g,mx,my,delta);
    }
    @Override public boolean mouseScrolled(double x,double y,double horizontal,double vertical) {
        if(x>=contentX && x<=contentX+contentWidth && y>=top+70 && y<top+panelHeight-32 && vertical!=0) {
            page=Math.max(0,Math.min(pages-1,page+(vertical<0?1:-1)));refreshCards();return true;
        }
        return super.mouseScrolled(x,y,horizontal,vertical);
    }

    private final class FeatureCard extends Button {
        private final Feature feature;
        private float hover;
        private long last=System.nanoTime();
        FeatureCard(int x,int y,int w,Feature feature) {
            super(x,y,w,cardHeight,Component.literal(feature.name),b->{
                if(!controls.featureEnabled(feature.id())) return;
                if(feature.closes) Minecraft.getInstance().setScreen(null);
                feature.action.run();
                if(activeOnly && minecraft.screen==RasmScreen.this) refreshCards();
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
            RasmUi.box(g,x,y,w,cardHeight,RasmUi.blend(RasmUi.SURFACE,RasmUi.accent(),hover*0.10f),
                running?RasmUi.blend(RasmUi.BORDER,RasmUi.accent(),0.65f):RasmUi.blend(RasmUi.BORDER,RasmUi.accent(),hover*0.25f));
            if(pinned.contains(feature.id())) g.fill(x+2,y+6,x+4,y+cardHeight-6,RasmUi.accent());
            RasmUi.round(g,x+7,y+7,18,18,RasmUi.blend(RasmUi.PANEL,RasmUi.accent(),permitted?0.10f:0));
            RasmUi.icon(g,feature.category,x+8,y+8,permitted?RasmUi.accent():RasmUi.DIM);
            String status=!permitted?"LOCK":feature.enabled==null?">":running?"ON":"IDLE";
            if(feature.id().equals("movement_replay")||feature.id().equals("block_esp")||feature.name.equals("Custom Glow")) status=permitted?">":"LOCK";
            int sw=font.width(status)+8;
            RasmUi.round(g,x+w-sw-7,y+5,sw,15,running?RasmUi.blend(RasmUi.PANEL,RasmUi.accent(),0.2f):RasmUi.PANEL);
            g.drawString(font,status,x+w-sw-3,y+9,running?RasmUi.accent():RasmUi.MUTED,false);
            RasmUi.text(g,feature.name,x+31,y+7,w-sw-43,permitted?RasmUi.TEXT:RasmUi.DIM);
            RasmUi.small(g,feature.detail,x+31,y+22,w-40,RasmUi.MUTED);
            if(cardHeight>40) {
                String key=feature.binding.isEmpty()?"Menu action":controls.bindingLabel(feature.binding);
                RasmUi.small(g,feature.category+"  /  "+key+(pinned.contains(feature.id())?"  /  Pinned":""),x+31,y+34,w-40,RasmUi.DIM);
            }

        }
    }

    private final class FeatureSwitch extends Button {
        private final Feature feature;
        FeatureSwitch(int x,int y,int w,Feature feature) {
            super(x,y,w,cardHeight,Component.literal("Enable or disable "+feature.name),b->{controls.toggleFeature(feature.id());if(activeOnly)refreshCards();},DEFAULT_NARRATION);
            this.feature=feature;
            setTooltipDelay(java.time.Duration.ofMillis(450));
            setTooltip(Tooltip.create(Component.literal("Master switch: OFF stops this feature and blocks its key. ON allows it.")));
        }
        @Override protected void renderContents(GuiGraphics g,int mx,int my,float dt) {
            boolean on=controls.featureEnabled(feature.id());
            int x=getX(),y=getY();
            RasmUi.box(g,x,y,getWidth(),cardHeight,RasmUi.PANEL,isHoveredOrFocused()?RasmUi.accent():RasmUi.BORDER);
            int yy=y+(cardHeight-22)/2;
            RasmUi.round(g,x+5,yy,19,10,on?RasmUi.blend(RasmUi.PANEL,RasmUi.accent(),0.5f):0xFF354056);
            RasmUi.round(g,x+(on?16:7),yy+2,6,6,on?RasmUi.accent():RasmUi.MUTED);
            RasmUi.small(g,on?"ON":"OFF",x+8,yy+16,20,on?RasmUi.accent():RasmUi.DIM);

        }
    }
    @Override public boolean isPauseScreen() { return false; }
}
