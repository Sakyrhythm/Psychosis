package com.sakyrhythm.psychosis.networking;

import com.sakyrhythm.psychosis.Psychosis;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Consumer;

public class ModNetworking {

    private static final Logger LOGGER = LoggerFactory.getLogger("ModNetworking");

    // 1. 定义通道标识符 (Identifier)
    public static final Identifier ION_BEAM_PACKET_ID = Identifier.of(Psychosis.MOD_ID, "ion_beam");

    // 2. 定义数据载荷 (Payload)
    public record IonBeamPayload(
            double startX, double startY, double startZ,
            double endX, double endY, double endZ) implements CustomPayload {

        // 定义数据载荷的类型 (用于注册)
        public static final CustomPayload.Id<IonBeamPayload> ID = new CustomPayload.Id<>(ION_BEAM_PACKET_ID);

        // 定义数据包的编解码器
        public static final PacketCodec<PacketByteBuf, IonBeamPayload> CODEC = PacketCodec.tuple(
                PacketCodecs.DOUBLE, IonBeamPayload::startX,
                PacketCodecs.DOUBLE, IonBeamPayload::startY,
                PacketCodecs.DOUBLE, IonBeamPayload::startZ,
                PacketCodecs.DOUBLE, IonBeamPayload::endX,
                PacketCodecs.DOUBLE, IonBeamPayload::endY,
                PacketCodecs.DOUBLE, IonBeamPayload::endZ,
                IonBeamPayload::new
        );

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }


    /**
     * 服务器端和客户端的初始化方法，用于注册自定义数据包
     */
    public static void register() {
        // 注册 IonBeamPayload 的编解码器
        PayloadTypeRegistry.playC2S().register(IonBeamPayload.ID, IonBeamPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(IonBeamPayload.ID, IonBeamPayload.CODEC);
    }


    // =================================================================================
    // 服务器发送逻辑
    // =================================================================================

    /**
     * 服务器向客户端发送离子束信息，用于客户端渲染粒子效果。
     * @param world 当前世界
     * @param start 离子束起始点
     * @param end 离子束终止点
     */
    public static void sendBeamPacket(ServerWorld world, Vec3d start, Vec3d end) {
        IonBeamPayload payload = new IonBeamPayload(
                start.x, start.y, start.z,
                end.x, end.y, end.z
        );

        // 向世界中的所有玩家发送这个数据包
        for (ServerPlayerEntity player : world.getPlayers()) {
            ServerPlayNetworking.send(player, payload);
        }
        LOGGER.debug("Sent IonBeamPacket from {} to {} to {} players.", start, end, world.getPlayers().size());
    }

    // =================================================================================
    // 客户端接收逻辑
    // =================================================================================

    @Environment(EnvType.CLIENT)
    public static void registerClientReceiver(Consumer<IonBeamPayload> consumer) {
        ClientPlayNetworking.registerGlobalReceiver(
                IonBeamPayload.ID,
                (payload, context) -> {
                    // 确保在主线程执行粒子效果生成
                    context.client().execute(() -> {
                        consumer.accept(payload);
                    });
                }
        );
        LOGGER.info("Registered client receiver for IonBeamPayload.");
    }
}