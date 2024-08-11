package connection.netty;

import client.MapleClient;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import connection.crypto.MapleCrypto;
//import org.apache.log4j.LogManager;
import java.util.HashMap;
import java.util.Map;

import static connection.netty.NettyClient.CLIENT_KEY;
import handling.login.LoginServer;
import tools.packet.LoginPacket;

public class ChannelAcceptor implements Runnable {

    public Map<String, Channel> channelPool = new HashMap<>();
    public handling.channel.ChannelServer channel;
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
                    
                    c.setChannel(channel.getChannel());
                    
//                    log.debug(String.format("Opened session with %s on channel %d", c.getIP(), channel.getChannelId()));
                    c.write(LoginPacket.sendConnect(riv, siv));

                    channelPool.put(c.getIP(), ch);

                    ch.attr(CLIENT_KEY).set(c);
                    ch.attr(MapleClient.CRYPTO_KEY).set(new MapleCrypto());

//                    EventManager.addFixedRateEvent(c::sendPing, 0, 10000);
                }
            });

            b.childOption(ChannelOption.TCP_NODELAY, true);
            b.childOption(ChannelOption.SO_KEEPALIVE, true);

            // Bind and start to accept incoming connections.
            ChannelFuture f = b.bind(channel.getPort()).sync();
//            log.info(String.format("Channel %d-%d listening on port %d", channel.getWorldId(), channel.getChannelId(), channel.getPort()));
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
