package dev.rasm.client;

import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

/** Shared dashboard styling. Theme choices are cosmetic and last for this session. */
final class RasmUi {
    static final int TEXT=0xFFE7EDF7, MUTED=0xFF93A0B7, DIM=0xFF60718A;
    static final int PANEL=0xFF101522, SURFACE=0xFF181F30, BORDER=0xFF2A3449, RED=0xFFFF849C;
    private static final int[] ACCENTS={0xFF70E2D2,0xFFB19AFF,0xFFFFC278};
    private static final String[] THEMES={"Mint", "Violet", "Amber"};
    private static int theme;
    static int accent() { return ACCENTS[theme]; }
    static String themeName() { return THEMES[theme]; }
    static void nextTheme() { theme=(theme+1)%ACCENTS.length; }
    static int blend(int a,int b,float t) {
        t=Math.max(0,Math.min(1,t));
        int r=(int)(((a>>16)&255)*(1-t)+((b>>16)&255)*t);
        int g=(int)(((a>>8)&255)*(1-t)+((b>>8)&255)*t);
        int bl=(int)((a&255)*(1-t)+(b&255)*t);
        return 0xFF000000|(r<<16)|(g<<8)|bl;
    }
    static void round(GuiGraphics g,int x,int y,int w,int h,int color) {
        if(w<5||h<5) { g.fill(x,y,x+w,y+h,color); return; }
        g.fill(x+2,y,x+w-2,y+h,color);
        g.fill(x,y+2,x+w,y+h-2,color);
        g.fill(x+1,y+1,x+w-1,y+h-1,color);
    }
    static void box(GuiGraphics g,int x,int y,int w,int h,int color,int border) {
        round(g,x,y,w,h,border); round(g,x+1,y+1,w-2,h-2,color);
    }
    static void text(GuiGraphics g,String text,int x,int y,int max,int color) {
        var font=Minecraft.getInstance().font;
        String display=text;
        if(font.width(display)>max) display=font.plainSubstrByWidth(display,Math.max(0,max-font.width("...")))+"...";
        g.drawString(font,display,x,y,color,false);
    }
    static void small(GuiGraphics g,String text,int x,int y,int max,int color) {
        g.pose().pushMatrix(); g.pose().translate(x,y).scale(0.8f);
        text(g,text,0,0,(int)(max/0.8f),color); g.pose().popMatrix();
    }
    static void frame(GuiGraphics g,int width,int height,int x,int y,int w,int h) {
        g.fillGradient(0,0,width,height,0xD0080C15,0xC00C1824);
        for(int i=8;i>0;i-=2) round(g,x-i,y+i,w+i*2,h,0x12000000);
        box(g,x,y,w,h,PANEL,BORDER);
        g.fillGradient(x+4,y+1,x+w-4,y+35,0x182EC5C9,0x00101522);
    }
    static void line(GuiGraphics g,float x,float y,float x2,float y2,int color) {
        g.pose().pushMatrix(); g.pose().translate(x,y).rotate((float)Math.atan2(y2-y,x2-x));
        g.fill(0,0,(int)Math.ceil(Math.hypot(x2-x,y2-y)),1,color); g.pose().popMatrix();
    }
    static void icon(GuiGraphics g,String category,int x,int y,int color) {
        switch(category) {
            case "Combat" -> { line(g,x+3,y+13,x+13,y+3,color); line(g,x+3,y+9,x+7,y+13,color); line(g,x+10,y+3,x+13,y+3,color); }
            case "Movement" -> { line(g,x+3,y+11,x+8,y+5,color); line(g,x+8,y+5,x+13,y+11,color); line(g,x+3,y+15,x+8,y+9,color); line(g,x+8,y+9,x+13,y+15,color); }
            case "Visuals" -> { line(g,x+1,y+8,x+8,y+3,color); line(g,x+8,y+3,x+15,y+8,color); line(g,x+15,y+8,x+8,y+13,color); line(g,x+8,y+13,x+1,y+8,color); round(g,x+6,y+6,4,4,color); }
            default -> { round(g,x+2,y+2,5,5,color); round(g,x+10,y+2,5,5,color); round(g,x+2,y+10,5,5,color); round(g,x+10,y+10,5,5,color); }
        }
    }
    static class Control extends Button {
        private final Supplier<String> label;
        private final BooleanSupplier selected;
        private final boolean danger;
        private float hover;
        private long last=System.nanoTime();
        Control(int x,int y,int w,int h,Supplier<String> label,Runnable action,BooleanSupplier selected,boolean danger) {
            super(x,y,w,h,Component.literal(label.get()),b->action.run(),DEFAULT_NARRATION);
            this.label=label; this.selected=selected; this.danger=danger;
        }
        @Override protected void renderContents(GuiGraphics g,int mx,int my,float dt) {
            long now=System.nanoTime(); float step=Math.min(1,(now-last)/100_000_000f); last=now;
            hover+=(isHoveredOrFocused()&&active?1-hover:-hover)*step;
            int accent=danger?RED:accent();
            boolean on=selected.getAsBoolean();
            int bg=blend(SURFACE,accent,on?0.19f:hover*0.12f);
            box(g,getX(),getY(),getWidth(),getHeight(),bg,on?blend(BORDER,accent,0.65f):blend(BORDER,accent,hover*0.4f));
            setMessage(Component.literal(label.get()));
            var font=Minecraft.getInstance().font;
            String display=font.plainSubstrByWidth(label.get(),getWidth()-8);
            g.drawString(font,display,getX()+(getWidth()-font.width(display))/2,getY()+(getHeight()-8)/2,
                !active?DIM:danger?RED:on?accent:TEXT,false);
        }
    }
    private RasmUi() {}
}
