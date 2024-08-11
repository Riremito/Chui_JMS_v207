package connection;

import constants.ServerConstants;
import handling.SendPacketOpcode;
import io.netty.buffer.*;

import java.awt.Point;
import java.awt.Rectangle;
//import net.swordie.ms.client.guild.Guild;
//import handling.header.OutHeader;
import java.nio.charset.Charset;
//import util.FileTime;
import util.Position;
import util.Rect;
import util.Util;
//import org.apache.log4j.LogManager;

//import java.time.LocalDateTime;
import java.util.Arrays;

public class OutPacket extends Packet {
    private final ByteBuf baos = Unpooled.buffer()/*PooledByteBufAllocator.DEFAULT.buffer()*/;
    private short op;
//    private static final org.apache.log4j.Logger log = LogManager.getRootLogger();

    /**
     * Creates a new OutPacket with a given op. Immediately encodes the op.
     *
     * @param op The opcode of this OutPacket.
     */
    public OutPacket(short op) {
        super(new byte[]{});
//        baos = PooledByteBufAllocator.DEFAULT.buffer();
        encodeShort(op);
        this.op = op;
    }

    /**
     * Creates a new OutPacket with a given op. Immediately encodes the op.
     *
     * @param op The opcode of this OutPacket.
     */
    public OutPacket(int op) {
        this((short) op);
    }

    /**
     * Creates a new OutPacket, and initializes the data as empty.
     */
    public OutPacket() {
        super(new byte[]{});
//        baos = ByteBufAllocator.DEFAULT.buffer();
    }

    /**
     * Creates a new OutPacket with given data.
     *
     * @param data The data this net.swordie.ms.connection.packet has to be initialized with.
     */
    public OutPacket(byte[] data) {
        super(data);
//        baos = ByteBufAllocator.DEFAULT.buffer();
        encodeArr(data);
    }

    /**
     * Creates a new OutPacket with a given header. Immediately encodes the header's short value.
     *
     * @param header The header of this OutPacket.
     */
    public OutPacket(SendPacketOpcode header) {
        this(header.getValue());
    }

    /**
     * Returns the header of this OutPacket.
     *
     * @return the header of this OutPacket.
     */
    @Override
    public int getHeader() {
        return op;
    }

    /**
     * Encodes a single byte to this OutPacket.
     *
     * @param b The int to encode as a byte. Will be downcast, so be careful.
     */
    public void encodeByte(int b) {
        encodeByte((byte) b);
    }

    /**
     * Encodes a byte to this OutPacket.
     *
     * @param b The byte to encode.
     */
    public void encodeByte(byte b) {
        baos.writeByte(b);
    }
    
    public void encodeZeroBytes(final int i) {
        for (int x = 0; x < i; x++) {
            baos.writeByte((byte) 0);
        }
    }

    /**
     * Encodes a byte array to this OutPacket.
     * Named like this to prevent autocompletion of "by" to "byteArray" or similar names.
     *
     * @param bArr The byte array to encode.
     */
    public void encodeArr(byte[] bArr) {
        baos.writeBytes(bArr);
    }

    /**
     * Encodes a byte array to this OutPacket.
     *
     * @param arr the byte array, in string format (may contain '|' and whitespace to seperate bytes)
     */
    public void encodeArr(String arr) {
        encodeArr(Util.getByteArrayByString(arr));
    }

    /**
     * Encodes a character to this OutPacket, UTF-8.
     *
     * @param c The character to encode
     */
    public void encodeChar(char c) {
        baos.writeByte(c);
    }

    /**
     * Encodes a boolean to this OutPacket.
     *
     * @param b The boolean to encode (0/1)
     */
    public void encodeByte(boolean b) {
        baos.writeBoolean(b);
    }

    /**
     * Encodes a short to this OutPacket, in little endian.
     *
     * @param s The short to encode.
     */
    public void encodeShort(short s) {
        baos.writeShortLE(s);
    }

    public void encodeShortBE(short s) {
        baos.writeShort(s);
    }

    public void encodeIntBE(int i) {
        baos.writeInt(i);
    }

    /**
     * Encodes an integer to this OutPacket, in little endian.
     *
     * @param i The integer to encode.
     */
    public void encodeInt(int i) {
        baos.writeIntLE(i);
    }

    /**
     * Encodes a long to this OutPacket, in little endian.
     *
     * @param l The long to encode.
     */
    public void encodeLong(long l) {
        baos.writeLongLE(l);
    }

    /**
     * Encodes a String to this OutPacket.
     * Structure: short(size) + char array of <code>s</code>.
     *
     * @param s The String to encode.
     */
    public void encodeString(String s) {
        if (s == null) {
            s = "";
        }
        if (s.getBytes(ServerConstants.ASCII).length/*s.length()*/ > Short.MAX_VALUE) {
            return;
        }
        encodeShort((short) s.getBytes(ServerConstants.ASCII).length/*s.length()*/);
        encodeString(s, (short) s.getBytes(ServerConstants.ASCII).length/*s.length()*/);
    }

    /**
     * Writes a String as a character array to this OutPacket.
     * If <code>s.length()</code> is smaller than length, the open spots are filled in with zeros.
     *
     * @param s      The String to encode.
     * @param length The maximum length of the buffer.
     */
    public void encodeString(String s, short length) {
        if (s == null) {
            s = "";
        }
        if (s.getBytes(ServerConstants.ASCII).length > 0) {
            encodeArr(s.getBytes(ServerConstants.ASCII));
        }
        for (int i = s.getBytes(ServerConstants.ASCII).length; i < length; i++) {
            encodeByte((byte) 0);
        }
    }

    @Override
    public void setData(byte[] nD) {
        super.setData(nD);
        baos.clear();
        encodeArr(nD);
    }

    @Override
    public byte[] getData() {
        return ByteBufUtil.getBytes(baos);
/*        if (baos.hasArray()) {
            return baos.array();
        } else {
            byte[] arr = new byte[baos.writerIndex()];
            baos.nioBuffer().get(arr, 0, baos.writerIndex());
            return arr;
        }*/
    }

    @Override
    public Packet clone() {
        return new OutPacket(getData());
    }

    /**
     * Returns the length of the ByteArrayOutputStream.
     *
     * @return The length of baos.
     */
    @Override
    public int getLength() {
        return getData().length;
    }

    @Override
    public String toString() {
        return String.format("%s, %s/0x%s\t| %s", SendPacketOpcode.nameOf(op)/*OutHeader.getOutHeaderByOp(op)*/, op, Integer.toHexString(op).toUpperCase()
                , Util.readableByteArray(Arrays.copyOfRange(getData(), 2, getData().length)));
    }

    public void encodeShort(int value) {
        encodeShort((short) value);
    }

    public void encodeString(String name, int length) {
        encodeString(name, (short) length);
    }

/*    public void encodeFT(FileTime fileTime) {
        if (fileTime == null) {
            encodeLong(0);
        } else {
            fileTime.encode(this);
        }
    }*/

    public void encodePosition(Point position) {
        if (position != null) {
            encodeShort(position.x);
            encodeShort(position.y);
        } else {
            encodeShort(0);
            encodeShort(0);
        }
    }

/*    public void encodePosition(Position position) {
        if (position != null) {
            encodeShort(position.getX());
            encodeShort(position.getY());
        } else {
            encodeShort(0);
            encodeShort(0);
        }
    }*/
    
    public void encodeRectInt(Rectangle rect) {
        encodeInt(rect.x);
        encodeInt(rect.y);
        encodeInt(rect.x + rect.width);
        encodeInt(rect.y + rect.height);
    }

/*    public void encodeRectInt(Rect rect) {
        encodeInt(rect.getLeft());
        encodeInt(rect.getTop());
        encodeInt(rect.getRight());
        encodeInt(rect.getBottom());
    }*/

    public void encodePositionInt(Position position) {
        encodeInt(position.getX());
        encodeInt(position.getY());
    }

/*    public void encodeFT(long currentTime) {
        encodeFT(new FileTime(currentTime));
    }*/

    public void encodeTime(boolean dynamicTerm, int time) {
        encodeByte(dynamicTerm);
        encodeInt(time);
    }

    public void encodeTime(int time) {
        encodeByte(false);
        encodeInt(time);
    }

/*    public void release() {
        baos.release();
    }*/

/*    public void encodeFT(LocalDateTime localDateTime) {
        encodeFT(FileTime.fromDate(localDateTime));
    }*/

    public void encode(Encodable encodable) {
        encodable.encode(this);
    }
}
