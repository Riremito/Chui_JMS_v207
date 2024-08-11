package handling;


import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface Handler {

    RecvPacketOpcode op() default RecvPacketOpcode.NO;

    RecvPacketOpcode[] ops() default {};
}
