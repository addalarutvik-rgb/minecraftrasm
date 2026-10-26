package dev.rasm.client;

import java.util.*;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import dev.rasm.client.mixin.GameRendererFovInvoker;

/** Bounded incremental loaded-world scan; HUD wireframes remain visible through terrain. */
public final class BlockEsp {
    public static final int RANGE = 24, MAX_MARKERS = 128;
    private static final int SIDE = RANGE * 2 + 1, VOLUME = SIDE * SIDE * SIDE;
    private static final Set<Block> selected = new HashSet<>();
    private static final Set<BlockPos> matches = new LinkedHashSet<>();
    private static ClientLevel world;
    private static BlockPos origin;
    private static int cursor;
    private static boolean enabled;
    private BlockEsp() {}
    public static boolean active() { return enabled && FeatureSwitches.enabled("block_esp"); }
    static int selectedCount() { return selected.size(); }
    static int markerCount() { return matches.size(); }
    static boolean selected(Block block) { return selected.contains(block); }
    static void select(Block block) {
        if (!selected.remove(block)) selected.add(block);
        enabled = !selected.isEmpty(); matches.clear(); cursor = 0;
    }
    static void toggle() { enabled = !enabled; if (!enabled) matches.clear(); }
    static void clear() { selected.clear(); disable(); }
    public static void disable() { enabled = false; matches.clear(); }
    public static void reset() { clear(); world = null; origin = null; cursor = 0; }
    static void register() {
        HudRenderCallback.EVENT.register((g, delta) -> {
            Minecraft client = Minecraft.getInstance();
            if (!active() || client.level != world || client.player == null || client.options.hideGui) return;
            var camera = client.gameRenderer.getMainCamera();
            Vec3 eye = camera.position();
            Quaternionf inverse = new Quaternionf(camera.rotation()).conjugate();
            float fov = ((GameRendererFovInvoker)client.gameRenderer).rasm$getFov(camera, delta.getGameTimeDeltaPartialTick(false), true);
            float scale = (float)(client.getWindow().getGuiScaledHeight() / (2 * Math.tan(Math.toRadians(fov / 2))));
            float cx = client.getWindow().getGuiScaledWidth() / 2f, cy = client.getWindow().getGuiScaledHeight() / 2f;
            for (BlockPos pos : matches) {
                Vector3f[] corners = new Vector3f[8];
                for (int i = 0; i < 8; i++) corners[i] = new Vector3f((float)(pos.getX() + (i & 1) - eye.x),
                    (float)(pos.getY() + ((i >> 1) & 1) - eye.y), (float)(pos.getZ() + ((i >> 2) & 1) - eye.z)).rotate(inverse);
                for (int i = 0; i < 8; i++) for (int bit : new int[]{1,2,4}) if ((i & bit) == 0) {
                    Vector3f a = new Vector3f(corners[i]), b = new Vector3f(corners[i | bit]);
                    if (a.z >= -0.05f && b.z >= -0.05f) continue;
                    if (a.z >= -0.05f) a.lerp(b, (-0.05f - a.z) / (b.z - a.z));
                    if (b.z >= -0.05f) b.lerp(a, (-0.05f - b.z) / (a.z - b.z));
                    float x1 = cx - a.x * scale / a.z, y1 = cy + a.y * scale / a.z;
                    float x2 = cx - b.x * scale / b.z, y2 = cy + b.y * scale / b.z;
                    // Reject enormous near-plane projections; keep HUD geometry bounded.
                    if (Math.max(Math.max(Math.abs(x1), Math.abs(x2)), Math.max(Math.abs(y1), Math.abs(y2))) > 10000) continue;
                    float length = (float)Math.hypot(x2-x1, y2-y1);
                    g.pose().pushMatrix();
                    g.pose().translate(x1,y1).rotate((float)Math.atan2(y2-y1,x2-x1));
                    g.fill(0,0,(int)Math.ceil(length),1,0xEE63E3C2);
                    g.pose().popMatrix();
                }
            }
        });
    }
    static void tick(Minecraft client) {
        if (client.level != world) { world = client.level; matches.clear(); origin = null; cursor = 0; }
        if (!active() || client.player == null || world == null || selected.isEmpty()) return;
        BlockPos center = client.player.blockPosition();
        if (origin == null || origin.distManhattan(center) > 8) { origin = center; cursor = 0; matches.clear(); }
        matches.removeIf(pos -> !world.hasChunkAt(pos) || pos.distSqr(center) > RANGE * RANGE || !selected.contains(world.getBlockState(pos).getBlock()));
        BlockPos.MutableBlockPos probe = new BlockPos.MutableBlockPos();
        for (int n = 0; n < 4096; n++) {
            int index = cursor++;
            if (cursor == VOLUME) cursor = 0;
            probe.set(origin.getX() + index % SIDE - RANGE, origin.getY() + index / SIDE % SIDE - RANGE,
                origin.getZ() + index / (SIDE * SIDE) - RANGE);
            if (probe.distSqr(center) > RANGE * RANGE || probe.getY() < world.getMinY() || probe.getY() > world.getMaxY() || !world.hasChunkAt(probe)) continue;
            if (selected.contains(world.getBlockState(probe).getBlock()) && matches.size() < MAX_MARKERS) matches.add(probe.immutable());
        }
    }
}
