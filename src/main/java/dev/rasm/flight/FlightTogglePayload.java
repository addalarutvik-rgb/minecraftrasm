package dev.rasm.flight;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** No target UUID or abilities come from the client; the sender is authoritative. */
public record FlightTogglePayload() implements CustomPacketPayload {
    public static final FlightTogglePayload INSTANCE = new FlightTogglePayload();
    public static final Type<FlightTogglePayload> TYPE = new Type<>(
        Identifier.fromNamespaceAndPath("rasmclient", "toggle_flight_v1"));
    public static final StreamCodec<RegistryFriendlyByteBuf, FlightTogglePayload> CODEC =
        StreamCodec.unit(INSTANCE);

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
