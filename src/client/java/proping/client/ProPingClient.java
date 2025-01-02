package com.example.proping;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.Packet;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.util.math.Vec3d;

import java.util.concurrent.ConcurrentLinkedQueue;

@Environment(EnvType.CLIENT)
public class PingReducerMod implements ClientModInitializer {
    private static final ConcurrentLinkedQueue<Packet<?>> packetQueue = new ConcurrentLinkedQueue<>();
    private static final MinecraftClient client = MinecraftClient.getInstance();

    @Override
    public void onInitializeClient() {
        // Listen to client ticks to handle packet smoothing
        ClientTickEvents.END_CLIENT_TICK.register(minecraftClient -> processPackets());

        // Intercept specific packets
        ClientPlayNetworking.registerGlobalReceiver(PlayerPositionLookS2CPacket.ID, (client, handler, buf, responseSender) -> {
            PlayerPositionLookS2CPacket packet = new PlayerPositionLookS2CPacket(buf);
            smoothPosition(packet);
            packetQueue.add(packet);
        });
    }

    private void processPackets() {
        // Throttle packets to reduce spam
        while (!packetQueue.isEmpty()) {
            Packet<?> packet = packetQueue.poll();
            if (packet != null) {
                client.getNetworkHandler().sendPacket(packet);
            }
        }
    }

    private void smoothPosition(PlayerPositionLookS2CPacket packet) {
        if (client.player != null) {
            Vec3d currentPos = client.player.getPos();
            Vec3d targetPos = new Vec3d(packet.getX(), packet.getY(), packet.getZ());

            // Smooth position updates
            double smoothingFactor = 0.5; // Adjust this to change the smoothing effect
            double smoothedX = currentPos.x + (targetPos.x - currentPos.x) * smoothingFactor;
            double smoothedY = currentPos.y + (targetPos.y - currentPos.y) * smoothingFactor;
            double smoothedZ = currentPos.z + (targetPos.z - currentPos.z) * smoothingFactor;

            client.player.updatePosition(smoothedX, smoothedY, smoothedZ);
        }
    }
}
