package dev.rasm.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;

final class MovementScreen extends Screen {
    private final Screen parent;
    private int left,top,w,h,page,pages,rows;
    private boolean sequence;
    private final MovementRecorder recorder=MovementRecorder.INSTANCE;
    MovementScreen(Screen parent) { super(Component.literal("Movement clip editor")); this.parent=parent; }
    @Override protected void init() {
        w=Math.min(620,width-12); h=Math.min(360,height-12); left=(width-w)/2; top=(height-h)/2;
        rows=Math.max(1,(h-178)/25); build();
    }
    private void button(int x,int y,int width,String label,Runnable action) {
        addRenderableWidget(new RasmUi.Control(x,y,width,22,()->label,action,()->false,false));
    }
    private void build() {
        clearWidgets();
        addRenderableWidget(new RasmUi.Control(left+12,top+32,90,22,()->"Mode: Record",()->{recorder.mode(true);build();},recorder::recordMode,false))
            .setTooltip(Tooltip.create(Component.literal("Replay/record key uses Record mode: press once to start, again to stop and save. Key is rebindable.")));
        addRenderableWidget(new RasmUi.Control(left+108,top+32,90,22,()->"Mode: Replay",()->{recorder.mode(false);build();},()->!recorder.recordMode(),false));
        button(left+w-34,top+8,22,"X",this::onClose);
        addRenderableWidget(new RasmUi.Control(left+12,top+60,82,22,()->"Clips",()->{sequence=false;page=0;build();},()->!sequence,false));
        addRenderableWidget(new RasmUi.Control(left+100,top+60,82,22,()->"Sequence",()->{sequence=true;page=0;build();},()->sequence,false));
        var list=sequence?recorder.sequence():recorder.ids();
        pages=Math.max(1,(list.size()+rows-1)/rows); page=Math.max(0,Math.min(page,pages-1));
        for(int i=page*rows;i<Math.min(list.size(),(page+1)*rows);i++) {
            int index=i,id=list.get(i),y=top+93+(i-page*rows)*25;
            int controls=sequence?86:70;
            addRenderableWidget(new RasmUi.Control(left+12,y,w-controls-30,22,
                ()->(sequence?(index+1)+". ":"")+recorder.clipLabel(id),()->{recorder.select(id);build();},()->recorder.selected()==id,false))
                .setTooltip(Tooltip.create(Component.literal("Select this clip to toggle its end checkpoint. Checkpoints verify the recorded displacement, without teleporting.")));
            if(sequence) {
                button(left+w-86,y,22,"↑",()->{recorder.move(index,-1);build();});
                button(left+w-60,y,22,"↓",()->{recorder.move(index,1);build();});
                button(left+w-34,y,22,"X",()->{recorder.removeEntry(index);build();});
            } else {
                button(left+w-74,y,36,"Add",()->{recorder.append(id);build();});
                button(left+w-34,y,22,"X",()->{recorder.select(id);recorder.deleteSelected();build();});
            }
        }
        button(left+w-60,top+60,22,"<",()->{if(page>0)page--;build();});
        button(left+w-34,top+60,22,">",()->{if(page+1<pages)page++;build();});
        int bw=(w-36)/2;
        button(left+12,top+h-58,bw,"Record new clip",()->{recorder.mode(true);recorder.record(minecraft);build();});
        button(left+24+bw,top+h-58,bw,"Play sequence",()->{recorder.mode(false);recorder.replay(minecraft);build();});
        button(left+12,top+h-30,bw,"Toggle end checkpoint",()->{recorder.checkpoint(minecraft);build();});
        button(left+24+bw,top+h-30,bw,"Stop",()->{recorder.stop();build();});
    }
    @Override public void render(GuiGraphics g,int mx,int my,float delta) {
        RasmUi.frame(g,width,height,left,top,w,h);
        g.drawString(font,"MOVEMENT CLIP EDITOR",left+12,top+15,RasmUi.TEXT,false);
        RasmUi.small(g,recorder.status(),left+204,top+39,w-220,RasmUi.MUTED);
        RasmUi.small(g,(page+1)+" / "+pages,left+w-98,top+68,35,RasmUi.MUTED);
        if((sequence?recorder.sequence():recorder.ids()).isEmpty())
            RasmUi.text(g,sequence?"Add clips from the Clips tab.":"Record your first movement clip below.",left+12,top+98,w-24,RasmUi.MUTED);
        RasmUi.small(g,recorder.startLabel(),left+12,top+h-86,w-24,RasmUi.MUTED);
        RasmUi.small(g,recorder.notice(),left+12,top+h-75,w-24,RasmUi.TEXT);
        super.render(g,mx,my,delta);
    }
    @Override public void onClose() { minecraft.setScreen(parent); }
    @Override public boolean isPauseScreen() { return false; }
}
