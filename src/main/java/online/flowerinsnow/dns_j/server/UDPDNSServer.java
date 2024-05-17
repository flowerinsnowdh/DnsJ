package online.flowerinsnow.dns_j.server;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.handler.codec.dns.*;
import online.flowerinsnow.dns_j.exception.UnexpectedException;
import online.flowerinsnow.dns_j.util.DNSRecordParser;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.net.BindException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Objects;

public abstract class UDPDNSServer implements AutoCloseable {
    @NotNull private final Logger logger;
    @NotNull private final SocketAddress bind;
    private Channel channel;

    private final EventLoopGroup group = new NioEventLoopGroup();

    public UDPDNSServer(@NotNull SocketAddress bind) {
        this.logger = LogManager.getLogger(UDPDNSServer.class.getSimpleName());
        this.bind = Objects.requireNonNull(bind);
    }

    public void start() throws BindException {
        try {
            this.channel = new Bootstrap()
                    .group(this.group)
                    .channel(NioDatagramChannel.class)
                    .option(ChannelOption.SO_BROADCAST, true)
                    .handler(new ChannelInitializer<>() {
                        @Override
                        protected void initChannel(Channel ch) {
                            ch.pipeline()
                                    .addLast(new DatagramDnsQueryDecoder())
                                    .addLast(new DatagramDnsResponseEncoder())
                                    .addLast(new SimpleChannelInboundHandler<DatagramDnsQuery>() {
                                        @Override
                                        protected void channelRead0(ChannelHandlerContext ctx, DatagramDnsQuery msg) throws Exception {
                                            final Logger logger = UDPDNSServer.this.logger;
                                            // log 查询内容
                                            logger.info("--------- BEGIN DNS QUERY ({}) ---------", msg.sender());
                                            DNSRecordParser.logMsg(logger, msg);
                                            logger.info("--------- END DNS QUERY ({})---------", msg.sender());

                                            // 通过 DoH 服务器获取响应内容
                                            DnsResponse response = UDPDNSServer.this.getResponse(msg, msg.recipient(), msg.sender());
                                            // log 响应内容
                                            logger.info("--------- BEGIN DNS RESPONSE ({}) ---------", msg.sender());
                                            DNSRecordParser.logMsg(logger, response);
                                            logger.info("--------- END DNS RESPONSE ({}) ---------", msg.sender());
                                            // 将响应内容返回给查询客户端
                                            ctx.writeAndFlush(response).sync();
                                        }
                                    });
                        }
                    })
                    .bind(this.bind)
                    .addListener((ChannelFutureListener) future -> {
                        if (future.isSuccess()) {
                            UDPDNSServer.this.logger.info("服务器启动于 {}", future.channel().localAddress());
                        }
                    })
                    .sync()
                    .channel();
        } catch (InterruptedException e) {
            this.logger.warn("线程中断");
        }
    }

    public abstract @NotNull DnsResponse getResponse(@NotNull DnsQuery query, @NotNull InetSocketAddress sender, @NotNull InetSocketAddress recipient);

    @Override
    public void close() {
        try {
            if (this.channel != null) {
                this.channel.close().sync();
            }
            this.group.shutdownGracefully();
        } catch (InterruptedException e) {
            throw new UnexpectedException(e);
        }
    }
}
