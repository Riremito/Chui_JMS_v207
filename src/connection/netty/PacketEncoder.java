/*
    This file is part of Desu: MapleStory v62 Server Emulator
    Copyright (C) 2014  Zygon <watchmystarz@hotmail.com>

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/
package connection.netty;

//import handlers.header.OutHeader;
import connection.OutPacket;
import connection.crypto.MapleCrypto;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import connection.Packet;
import constants.ServerConstants;
import tools.HexTool;
import tools.StringUtil;
import util.Util;
//import org.apache.log4j.LogManager;

/**
 * Implementation of a Netty encoder pattern so that encryption of MapleStory
 * packets is possible. Follows steps using the special MapleAES as well as
 * ShandaCrypto (which became non-used after v149.2 in GMS).
 *
 * @author Zygon
 */
public final class PacketEncoder extends MessageToByteEncoder<OutPacket/*byte[]*/> {
//    private static final org.apache.log4j.Logger log = LogManager.getRootLogger();

    @Override
    protected void encode(ChannelHandlerContext chc, OutPacket outPacket/*byte[] data*/, ByteBuf bb) {
        
        byte[] data = outPacket.getData();
        NettyClient c = chc.channel().attr(NettyClient.CLIENT_KEY).get();
        MapleCrypto mCr = chc.channel().attr(NettyClient.CRYPTO_KEY).get();

        if (c != null) {

/*            if(!OutHeader.isSpamHeader(OutHeader.getOutHeaderByOp(outPacket.getHeader()))) {
                System.out.println("[Out]\t| " + outPacket);
            }*/

            if (ServerConstants.ShowSendP) {
                System.out.println("[發送]\t| " + outPacket);
            }

            byte[] iv = c.getSendIV();
            byte[] head = MapleCrypto.getHeader(data.length, iv);

            c.acquireEncoderState();
            try {
                mCr.crypt(data, iv);
                c.setSendIV(MapleCrypto.getNewIv(iv));
                bb.writeBytes(head);
                bb.writeBytes(data);
            } finally {
                c.releaseEncodeState();
            }
        } else {
            if (ServerConstants.ShowSendP) {
                System.out.println("[初次發送] | OnConnect,\t| " + Util.readableByteArray(data));
            }
            bb.writeBytes(data);
        }
        
//        outPacket.release();
//        bb.release();
    }
}
