package dev.rasm.client;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MovementTapeTest {
    @Test void replaysJumpAndReleaseExactlyOnceAndCanReplayAgain() {
        var tape=new MovementTape<String>();
        assertFalse(tape.play());
        tape.record(); tape.append("forward+jump"); tape.append("forward"); tape.append("idle");
        assertFalse(tape.play()); tape.stop(); assertTrue(tape.play());
        assertEquals("forward+jump",tape.next()); assertEquals("forward",tape.next());
        assertEquals("idle",tape.next()); assertFalse(tape.playing()); assertNull(tape.next());
        assertTrue(tape.play()); assertEquals("forward+jump",tape.next());
    }
    @Test void stoppingPlaybackPreservesRecordingButClearRemovesIt() {
        var tape=new MovementTape<Integer>(); tape.record(); tape.append(1); tape.append(2); tape.stop(); tape.play();
        tape.next(); tape.stop(); assertNull(tape.next()); assertEquals(2,tape.size());
        tape.play(); assertEquals(1,tape.next()); tape.clear(); assertFalse(tape.play());
    }
    @Test void boundedRecordingStopsAtOneMinuteAndNewRecordingReplacesOld() {
        var tape=new MovementTape<Integer>(); tape.record();
        for(int i=0;i<MovementTape.LIMIT+100;i++) tape.append(i);
        assertEquals(1200,tape.size()); assertFalse(tape.recording());
        assertTrue(tape.play()); tape.record(); assertEquals(0,tape.size()); assertFalse(tape.playing());
        tape.append(99); tape.stop(); tape.play(); assertEquals(99,tape.next());
    }
}
