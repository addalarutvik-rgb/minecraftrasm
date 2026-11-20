package dev.rasm.client;

import java.util.ArrayList;
import java.util.List;

/** One bounded, session-local recording. Playback consumes each frame exactly once. */
public final class MovementTape<T> {
    public static final int LIMIT = 1200;
    private final List<T> frames = new ArrayList<>();
    private boolean recording, playing;
    private int cursor;
    public void record() { clear(); recording = true; }
    public void append(T frame) {
        if (!recording) return;
        frames.add(frame);
        if (frames.size() == LIMIT) recording = false;
    }
    public boolean play() {
        if (recording || frames.isEmpty()) return false;
        cursor = 0; playing = true; return true;
    }
    public T next() {
        if (!playing) return null;
        T frame = frames.get(cursor++);
        if (cursor == frames.size()) playing = false;
        return frame;
    }
    public void stop() { recording = false; playing = false; cursor = 0; }
    public void clear() { stop(); frames.clear(); }
    public List<T> snapshot() { return List.copyOf(frames); }
    public int size() { return frames.size(); }
    public boolean recording() { return recording; }
    public boolean playing() { return playing; }
}
