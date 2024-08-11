package server.movement;

/**
 * Created by Weber on 2017/9/12.
 */
public enum MovementKind {

    PLAYER_MOVEMENT(0x01),
    MOB_MOVEMENT(0x02),
    PET_MOVEMENT(0x03),
    SUMMON_MOVEMENT(0x04),
    DRAGON_MOVEMENT(0x05),
    ANDROID_MOVEMENT(0x06),
    FAMILIAR_MOVMENT(0x07),
    NPC_MOVMENT(0x08),
    ;

    int type;

    MovementKind(int type) {
        this.type = type;
    }

    public int getValue() {
        return this.type;
    }
}
