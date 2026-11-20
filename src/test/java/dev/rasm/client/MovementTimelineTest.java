package dev.rasm.client;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
class MovementTimelineTest {
    @Test void repeatsAndChainsDifferentClipsWithoutEmptyTicks() {
        var t=new MovementTimeline<String>();
        int a=t.add(List.of("jump","land")), b=t.add(List.of("left"));
        t.append(a);t.append(a);t.append(b);assertTrue(t.play());
        assertTrue(t.next().first());assertTrue(t.next().last());
        assertEquals("jump",t.next().frame());assertEquals("land",t.next().frame());
        var last=t.next();assertEquals(b,last.clip());assertTrue(last.first());assertTrue(last.last());
        assertFalse(t.playing());assertNull(t.next());assertTrue(t.play());assertEquals(a,t.next().clip());
    }
    @Test void editOrderRemoveEntriesAndDeleteReferencedClips() {
        var t=new MovementTimeline<Integer>();int a=t.add(List.of(1)),b=t.add(List.of(2));
        t.append(a);t.append(b);t.append(a);t.move(1,-1);assertEquals(List.of(b,a,a),t.sequence());
        t.removeEntry(1);assertEquals(List.of(b,a),t.sequence());t.delete(a);assertEquals(List.of(b),t.sequence());
        t.play();t.delete(b);assertEquals(List.of(b),t.ids());t.stop();t.delete(b);assertFalse(t.play());
    }
    @Test void boundedLibraryAndQueueAndClear() {
        var t=new MovementTimeline<Integer>();assertEquals(-1,t.add(List.of()));
        int a=t.add(List.of(1));for(int i=1;i<16;i++)assertTrue(t.add(List.of(i))>0);
        assertEquals(-1,t.add(List.of(17)));
        for(int i=0;i<32;i++)assertTrue(t.append(a));assertFalse(t.append(a));
        t.clear();assertFalse(t.play());assertTrue(t.ids().isEmpty());assertTrue(t.sequence().isEmpty());
    }
}
