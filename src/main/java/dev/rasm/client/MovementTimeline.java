package dev.rasm.client;

import java.util.*;

/** A bounded clip library and ordered, repeatable sequence, played without gaps. */
public final class MovementTimeline<T> {
    public static final int MAX_CLIPS=16, MAX_SEQUENCE=32;
    public record Step<T>(int clip, T frame, boolean first, boolean last) {}
    private final Map<Integer,List<T>> clips=new LinkedHashMap<>();
    private final List<Integer> queue=new ArrayList<>();
    private List<Integer> run=List.of();
    private int nextId=1, clipCursor, frameCursor;
    private boolean playing;
    public int add(List<T> frames) {
        if(frames.isEmpty() || clips.size()>=MAX_CLIPS) return -1;
        int id=nextId++; clips.put(id,List.copyOf(frames)); return id;
    }
    public boolean append(int id) {
        if(playing || !clips.containsKey(id) || queue.size()>=MAX_SEQUENCE) return false;
        queue.add(id); return true;
    }
    public void removeEntry(int index) { if(!playing && index>=0 && index<queue.size()) queue.remove(index); }
    public void move(int index,int delta) {
        int target=index+delta;
        if(!playing && index>=0 && index<queue.size() && target>=0 && target<queue.size()) Collections.swap(queue,index,target);
    }
    public void delete(int id) { if(!playing) { clips.remove(id); queue.removeIf(value->value==id); } }
    public List<Integer> ids() { return List.copyOf(clips.keySet()); }
    public List<Integer> sequence() { return List.copyOf(queue); }
    public int length(int id) { return clips.getOrDefault(id,List.of()).size(); }
    public boolean play() {
        if(playing || queue.isEmpty()) return false;
        run=List.copyOf(queue); clipCursor=frameCursor=0; playing=true; return true;
    }
    public Step<T> next() {
        if(!playing) return null;
        int id=run.get(clipCursor); var frames=clips.get(id);
        var step=new Step<>(id,frames.get(frameCursor),frameCursor==0,frameCursor==frames.size()-1);
        if(++frameCursor==frames.size()) { frameCursor=0; if(++clipCursor==run.size()) playing=false; }
        return step;
    }
    public boolean playing() { return playing; }
    public void stop() { playing=false; run=List.of(); }
    public void clear() { stop(); clips.clear(); queue.clear(); nextId=1; }
}
