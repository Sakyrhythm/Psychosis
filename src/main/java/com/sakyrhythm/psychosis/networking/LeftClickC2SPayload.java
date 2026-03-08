package com.sakyrhythm.psychosis.networking;

import com.sakyrhythm.psychosis.Psychosis;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

// 创建自定义 Payload 类
public record LeftClickC2SPayload(String source) implements CustomPayload {

    public static final CustomPayload.Id<LeftClickC2SPayload> ID = new Id<>(Identifier.of(Psychosis.MOD_ID, "left_click"));

    // 创建 CODEC - 用于注册
    public static final PacketCodec<PacketByteBuf, LeftClickC2SPayload> CODEC =
            PacketCodec.of(
                    LeftClickC2SPayload::write,  // 编码方法
                    LeftClickC2SPayload::new      // 解码方法
            );

    // 从 PacketByteBuf 读取数据的构造函数
    public LeftClickC2SPayload(PacketByteBuf buf) {
        this(buf.readString());
    }

    public void write(PacketByteBuf buf) {
        buf.writeString(source);
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }

    // 可以移除或保留这些自动生成的方法
    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (LeftClickC2SPayload) obj;
        return java.util.Objects.equals(this.source, that.source);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(source);
    }

    @Override
    public String toString() {
        return "LeftClickC2SPayload[" +
                "source=" + source + ']';
    }
}