package connection.netty;

import client.MapleClient;
import constants.ServerConstants;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import connection.crypto.MapleCrypto;
//import org.apache.log4j.LogManager;
//import handling.EventManager;

import java.util.HashMap;
import java.util.Map;

import static connection.netty.NettyClient.CLIENT_KEY;
import handling.login.LoginServer;
import tools.packet.LoginPacket;

/**
 * Created by Tim on 2/18/2017.
 */
public class LoginAcceptor implements Runnable{

    public static Map<String, Channel> channelPool = new HashMap<>();
//    private static final org.apache.log4j.Logger log = LogManager.getRootLogger();
    @Override
    public void run() {
        // Taken from http://netty.io/wiki/user-guide-for-4.x.html

        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup);
            b.channel(NioServerSocketChannel.class);
            b.childHandler(new ChannelInitializer<SocketChannel>() {

                @Override
                protected void initChannel(SocketChannel ch) {

                    ch.pipeline().addLast(new PacketDecoder(), new ChannelHandler(), new PacketEncoder());

                    String address = ch.remoteAddress().toString().split(":")[0];
                    String IP = address.substring(address.indexOf('/') + 1, address.length());
                    LoginServer.removeIPAuth(IP);
                    
                    byte[] siv = new byte[]{70, 114, 30, 82};
                    byte[] riv = new byte[]{82, 48, 25, 115};

                    MapleClient c = new MapleClient(ch, siv, riv);
                    // remove after debug stage
//                    log.debug(String.format("Opened session with %s in LoginAcceptor", c.getIP()));
                    c.setChannel(-1);

                    c.write(LoginPacket.sendConnect(riv, siv));
                    
//                  c.write("hi".getBytes());

                    channelPool.put(c.getIP(), ch);

                    ch.attr(CLIENT_KEY).set(c);
                    ch.attr(MapleClient.CRYPTO_KEY).set(new MapleCrypto());

//                    EventManager.addFixedRateEvent(c::sendPing, 0, 10000);
                }
            });

//            b.option(ChannelOption.ALLOCATOR, new PooledByteBufAllocator(true));

            b.childOption(ChannelOption.TCP_NODELAY, true);
            b.childOption(ChannelOption.SO_KEEPALIVE, true);

            // Bind and start to accept incoming connections.
            ChannelFuture f = b.bind(8484/*ServerConstants.LOGIN_PORT*/).sync();

            // Wait until the server socket is closed.
            // In this example, this does not happen, but you can do that to gracefully
            // shut down your server.
            f.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
        }
    }
}
