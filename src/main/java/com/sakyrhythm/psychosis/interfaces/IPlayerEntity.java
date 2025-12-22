package com.sakyrhythm.psychosis.interfaces;

import org.spongepowered.asm.mixin.Unique;

public interface IPlayerEntity {
    int getDark();

    void setDark(int dark);

    void setNoticed(boolean noticed);

    // 假设 IPlayerEntity 中有 getNoticed()
    @Unique
    boolean getNoticed();

    void setDarkMsg1Sent(boolean sent);
    boolean getDarkMsg1Sent();

    void setDarkMsg2Sent(boolean sent);
    boolean getDarkMsg2Sent();

    void setDarkMsg3Sent(boolean sent);
    boolean getDarkMsg3Sent();

    void setDarkMsg4Sent(boolean sent);
    boolean getDarkMsg4Sent();

    int[] queryDarkEffectInfo();
}