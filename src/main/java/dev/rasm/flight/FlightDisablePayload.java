package dev.rasm.flight;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Idempotent panic shutdown; never grants flight. */
public record FlightDisablePayload() implements CustomPacketPayload {
    public static final FlightDisablePayload INSTANCE = new FlightDisablePayload();
    public static final Type<FlightDisablePayload> TYPE = new Type<>(
        Identifier.fromNamespaceAndPath("rasmclient", "disable_flight_v1"));
    public static final StreamCodec<RegistryFriendlyByteBuf, FlightDisablePayload> CODEC =
        StreamCodec.unit(INSTANCE);

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
