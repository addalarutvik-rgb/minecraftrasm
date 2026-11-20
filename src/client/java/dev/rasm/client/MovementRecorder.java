package dev.rasm.client;

import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.phys.Vec3;

public final class MovementRecorder {
    public static final MovementRecorder INSTANCE=new MovementRecorder();
    private record Frame(Input input,float yaw,float pitch,boolean sprinting) {}
    private record Clip(Vec3 start,Vec3 end,boolean checkpoint) {}
    private final MovementTape<Frame> tape=new MovementTape<>();
    private final MovementTimeline<Frame> timeline=new MovementTimeline<>();
    private final Map<Integer,Clip> clips=new LinkedHashMap<>();
    private LocalPlayer owner;
    private ClientLevel level;
    private Vec3 start, runOrigin, expectedCheckpoint;
    private boolean releasing, recordMode=true;
    private int selected=-1;
    private String notice="Record mode: press 5 to start, then 5 to save the clip.";
    private MovementRecorder() {}
    public boolean active() { return recording() || playing() || expectedCheckpoint!=null; }
    boolean recording() { return tape.recording(); }
    boolean playing() { return timeline.playing(); }
    boolean recordMode() { return recordMode; }
    void mode(boolean record) { stop(); recordMode=record; }
    List<Integer> ids() { return timeline.ids(); }
    List<Integer> sequence() { return timeline.sequence(); }
    int selected() { return selected; }
    void select(int id) { selected=id; }
    String clipLabel(int id) { return "Clip "+id+" · "+String.format(Locale.ROOT,"%.1fs",timeline.length(id)/20.0)+(clips.get(id).checkpoint?" · checkpoint":""); }
    String status() { return recording()?"Recording: "+String.format(Locale.ROOT,"%.1fs",tape.size()/20.0):playing()?"Playing sequence":"Library: "+clips.size()+" / 16 · Sequence: "+sequence().size()+" / 32"; }
    String notice() { return notice; }
    String startLabel() {
        Vec3 pos=clips.containsKey(selected)?clips.get(selected).start:start;
        return pos==null?"No start position":String.format(Locale.ROOT,"Start: %.2f, %.2f, %.2f",pos.x,pos.y,pos.z);
    }
    void append(int id) { notice=timeline.append(id)?"Clip added to the end of the sequence.":"Sequence full (32 entries)."; }
    void move(int index,int delta) { timeline.move(index,delta); }
    void removeEntry(int index) { timeline.removeEntry(index); }
    void deleteSelected() { if(!active()) { timeline.delete(selected); clips.remove(selected); selected=ids().isEmpty()?-1:ids().getFirst(); } }
    private boolean ready(Minecraft c) { return c.player!=null && c.level!=null && c.player.isAlive() && !c.player.isSpectator() && !c.player.isPassenger() && !c.player.isSleeping(); }
    void press(Minecraft c) {
        if(active()) { stop(); return; }
        if(recordMode) record(c); else replay(c);
    }
    void record(Minecraft c) {
        if(!ready(c) || !FeatureSwitches.enabled("movement_replay") || RasmClient.movementBusy()) return;
        if(clips.size()>=16) { notice="Library full. Delete a clip before recording."; return; }
        if(!c.player.onGround() || c.player.getDeltaMovement().horizontalDistanceSqr()>0.01) { notice="Stand still on the starting block before recording."; return; }
        beginRecording(c);
    }
    private void beginRecording(Minecraft c) {
        tape.record(); releasing=false; owner=c.player; level=c.level; start=c.player.position();
        notice="Recording. Press 5 to stop/save, or checkpoint to split."; c.setScreen(null);
    }
    private void save(boolean checkpoint) {
        if(tape.size()==0) { tape.stop(); return; }
        var c=Minecraft.getInstance();
        if(c.player!=owner || c.level!=level) { tape.clear(); return; }
        int id=timeline.add(tape.snapshot());
        if(id!=-1) { clips.put(id,new Clip(start,owner.position(),checkpoint)); selected=id; boolean added=timeline.append(id); notice="Saved Clip "+id+(added?" and appended it to the sequence.":". Sequence full; add it later."); }
        tape.clear();
    }
    void checkpoint(Minecraft c) {
        if(recording()) {
            save(true);
            if(clips.size()<16 && ready(c)) beginRecording(c);
        } else if(clips.containsKey(selected)) {
            var clip=clips.get(selected); clips.put(selected,new Clip(clip.start,clip.end,!clip.checkpoint));
            notice=clip.checkpoint?"Checkpoint removed.":"Checkpoint added at this clip's recorded end.";
        }
    }
    void replay(Minecraft c) {
        if(active()) { stop(); return; }
        if(!ready(c) || !FeatureSwitches.enabled("movement_replay") || RasmClient.movementBusy()) return;
        if(sequence().isEmpty() || owner!=c.player || level!=c.level) { notice="Record a clip and add it to the sequence first."; return; }
        Vec3 first=clips.get(sequence().getFirst()).start;
        if(c.player.position().distanceToSqr(first)>0.64 || !c.player.onGround() || c.player.getDeltaMovement().horizontalDistanceSqr()>0.01) {
            notice="Return to the first clip's start and stand still on the ground."; return;
        }
        if(timeline.play()) { releasing=false; expectedCheckpoint=null; notice="Playing sequence once. Press 5 to stop."; c.setScreen(null); }
    }
    public void stop() {
        if(recording() || tape.size()>0) save(false);
        if(playing() || expectedCheckpoint!=null) releasing=true;
        timeline.stop(); expectedCheckpoint=null;
    }
    void clear() { tape.clear(); timeline.clear(); clips.clear(); selected=-1; start=null; owner=null; level=null; expectedCheckpoint=null; releasing=false; notice="Library and sequence cleared."; }
    void tick(Minecraft c) {
        if(owner!=null && (owner!=c.player || level!=c.level)) { clear(); return; }
        if(!recording() && tape.size()>0) { save(false); notice="60-second limit reached; clip saved."; }
        if(active() && (!ready(c) || c.screen!=null || !FeatureSwitches.enabled("movement_replay") || RasmClient.movementBusy())) stop();
    }
    public Input input(Input live) {
        Minecraft c=Minecraft.getInstance(); tick(c);
        if(c.player==null) return live;
        if(releasing) { releasing=false; c.player.setSprinting(false); return Input.EMPTY; }
        if(expectedCheckpoint!=null) {
            boolean reached=c.player.position().distanceToSqr(expectedCheckpoint)<=2.25;
            expectedCheckpoint=null;
            if(!reached) { stop(); releasing=false; c.player.setSprinting(false); notice="Checkpoint missed. Sequence stopped; return to the start."; return Input.EMPTY; }
            if(!playing()) { c.player.setSprinting(false); return Input.EMPTY; }
        }
        if(recording()) tape.append(new Frame(live,c.player.getYRot(),c.player.getXRot(),c.player.isSprinting()));
        else if(playing()) {
            if(c.options.keyAttack.isDown() || c.options.keyUse.isDown() || c.player.isUsingItem()) { stop(); return live; }
            var step=timeline.next(); var frame=step.frame(); var clip=clips.get(step.clip());
            if(step.first()) runOrigin=c.player.position();
            c.player.setYRot(frame.yaw); c.player.setXRot(frame.pitch); c.player.setSprinting(frame.sprinting);
            if(step.last() && clip.checkpoint) expectedCheckpoint=runOrigin.add(clip.end.subtract(clip.start));
            if(!playing()) { releasing=expectedCheckpoint==null; notice="Sequence complete."; }
            return frame.input;
        }
        return live;
    }
}
