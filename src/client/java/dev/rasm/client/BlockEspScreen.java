package dev.rasm.client;

import java.util.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

/** Searchable registry browser, using the same controls and theme as the dashboard. */
final class BlockEspScreen extends Screen {
    private final Screen parent;
    private final List<Button> entries=new ArrayList<>();
    private final List<Block> blocks=BuiltInRegistries.BLOCK.stream()
        .sorted(Comparator.comparing(b->BuiltInRegistries.BLOCK.getKey(b).toString())).toList();
    private String query="";
    private boolean selectedOnly;
    private int left,top,panelWidth,panelHeight,page,pages,rows,columns,resultCount;
    private Button previous,next;
    private EditBox search;
    BlockEspScreen(Screen parent) { super(Component.literal("Block library")); this.parent=parent; }
    @Override protected void init() {
        panelWidth=Math.min(710,width-12); panelHeight=Math.min(390,height-12);
        left=(width-panelWidth)/2; top=(height-panelHeight)/2;
        columns=Math.max(1,Math.min(4,(panelWidth-18)/122));
        rows=Math.max(1,(panelHeight-105)/49);
        int searchWidth=Math.min(260,panelWidth-150);
        search=new EditBox(font,left+panelWidth-searchWidth-36,top+15,searchWidth-16,16,Component.literal("Search block names and IDs"));
        search.setBordered(false);search.setTextColor(RasmUi.TEXT);
        search.setHint(Component.literal("Search blocks...")); search.setMaxLength(128); search.setValue(query);
        search.setResponder(value->{query=value;page=0;refresh();}); addRenderableWidget(search);
        addRenderableWidget(new RasmUi.Control(left+panelWidth-31,top+10,22,24,()->"X",this::onClose,()->false,false));
        addRenderableWidget(new RasmUi.Control(left+10,top+44,55,21,()->"All",()->{selectedOnly=false;page=0;refresh();},()->!selectedOnly,false));
        addRenderableWidget(new RasmUi.Control(left+70,top+44,94,21,()->"Selected ("+BlockEsp.selectedCount()+")",()->{selectedOnly=true;page=0;refresh();},()->selectedOnly,false));
        addRenderableWidget(new RasmUi.Control(left+panelWidth-96,top+44,86,21,()->BlockEsp.active()?"ESP: ON":"ESP: OFF",BlockEsp::toggle,BlockEsp::active,false))
            .setTooltip(Tooltip.create(Component.literal("Pause or resume highlights without losing selections.")));
        addRenderableWidget(new RasmUi.Control(left+10,top+panelHeight-27,76,19,()->"Clear all",()->{BlockEsp.clear();refresh();},()->false,true));
        previous=addRenderableWidget(new RasmUi.Control(left+panelWidth-59,top+panelHeight-27,22,19,()->"<",()->{page--;refresh();},()->false,false));
        next=addRenderableWidget(new RasmUi.Control(left+panelWidth-33,top+panelHeight-27,22,19,()->">",()->{page++;refresh();},()->false,false));
        entries.clear();refresh();
    }
    private void refresh() {
        entries.forEach(this::removeWidget);entries.clear();
        String filter=query.toLowerCase(Locale.ROOT).trim();
        List<Block> found=blocks.stream().filter(b->!selectedOnly||BlockEsp.selected(b))
            .filter(b->(BuiltInRegistries.BLOCK.getKey(b)+" "+b.getName().getString()).toLowerCase(Locale.ROOT).contains(filter)).toList();
        resultCount=found.size();int capacity=rows*columns;
        pages=Math.max(1,(found.size()+capacity-1)/capacity);page=Math.max(0,Math.min(page,pages-1));
        int tileWidth=(panelWidth-20-(columns-1)*6)/columns;
        for(int i=page*capacity;i<Math.min(found.size(),(page+1)*capacity);i++) {
            int cell=i-page*capacity;
            entries.add(addRenderableWidget(new BlockTile(left+10+(cell%columns)*(tileWidth+6),top+73+(cell/columns)*49,tileWidth,found.get(i))));
        }
        previous.active=page>0;next.active=page+1<pages;
    }
    @Override public void render(GuiGraphics g,int mx,int my,float dt) {
        RasmUi.frame(g,width,height,left,top,panelWidth,panelHeight);
        g.drawString(font,"Block library",left+12,top+12,RasmUi.TEXT,false);
        RasmUi.small(g,resultCount+" blocks / "+BlockEsp.markerCount()+" visible",left+12,top+26,110,RasmUi.MUTED);
        int searchWidth=Math.min(260,panelWidth-150);
        RasmUi.box(g,left+panelWidth-searchWidth-42,top+9,searchWidth,25,RasmUi.SURFACE,search.isFocused()?RasmUi.accent():RasmUi.BORDER);
        if(entries.isEmpty()) {
            g.drawString(font,selectedOnly?"No selected blocks":"No matching blocks",left+20,top+88,RasmUi.TEXT,false);
            RasmUi.text(g,selectedOnly?"Choose All to add blocks to your selection.":"Try a block name or its registry ID.",left+20,top+105,panelWidth-40,RasmUi.MUTED);
        }
        RasmUi.small(g,"24-block range / 128 markers",left+96,top+panelHeight-20,Math.max(0,panelWidth-206),RasmUi.MUTED);
        g.drawString(font,(page+1)+" / "+pages,left+panelWidth-102,top+panelHeight-21,RasmUi.MUTED,false);
        super.render(g,mx,my,dt);
    }
    private final class BlockTile extends Button {
        private final Block block;
        private final ItemStack icon;
        BlockTile(int x,int y,int width,Block block) {
            super(x,y,width,43,block.getName(),b->{BlockEsp.select(block);refresh();},DEFAULT_NARRATION);
            this.block=block;this.icon=block.asItem().getDefaultInstance();
            setTooltipDelay(java.time.Duration.ofMillis(450));
            setTooltip(Tooltip.create(Component.literal(block.getName().getString()+"\n"+BuiltInRegistries.BLOCK.getKey(block)+"\nClick to select / remove.")));
        }
        @Override protected void renderContents(GuiGraphics g,int mx,int my,float dt) {
            int x=getX(),y=getY(),w=getWidth();boolean selected=BlockEsp.selected(block);
            RasmUi.box(g,x,y,w,43,RasmUi.blend(RasmUi.SURFACE,RasmUi.accent(),selected?0.12f:isHoveredOrFocused()?0.06f:0),selected?RasmUi.accent():RasmUi.BORDER);
            RasmUi.round(g,x+5,y+7,24,25,0xFF101726);
            if(!icon.isEmpty()) g.renderItem(icon,x+9,y+11);
            else RasmUi.icon(g,"Visuals",x+9,y+11,RasmUi.DIM);
            RasmUi.text(g,block.getName().getString(),x+35,y+9,w-43,RasmUi.TEXT);
            String id=BuiltInRegistries.BLOCK.getKey(block).getPath();
            RasmUi.small(g,id,x+35,y+23,w-43,RasmUi.MUTED);
            if(selected) { g.fill(x+1,y+36,x+w-1,y+40,RasmUi.blend(RasmUi.SURFACE,RasmUi.accent(),0.45f)); }
        }
    }
    @Override public void onClose() { minecraft.setScreen(parent); }
    @Override public boolean isPauseScreen() { return false; }
}
