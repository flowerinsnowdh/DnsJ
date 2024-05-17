package online.flowerinsnow.dns_j.client;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.dns.*;
import okhttp3.*;
import online.flowerinsnow.dns_j.exception.UnexpectedException;
import online.flowerinsnow.dns_j.util.NettyDatagramDnsResponseDecoder;
import online.flowerinsnow.dns_j.util.NettyDnsQueryEncoder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;
import java.util.List;
import java.util.Objects;

public abstract class DoHClient {
    private DoHClient() {
    }

    public static @NotNull DnsResponse query(@NotNull URL url, @NotNull DnsQuery query, @Nullable Proxy proxy, @NotNull InetSocketAddress sender, @NotNull InetSocketAddress recipient) throws IOException {
        Objects.requireNonNull(url);
        Objects.requireNonNull(query);

        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        builder.setProtocols$okhttp(List.of(Protocol.HTTP_2));
        if (proxy != null) {
            builder.proxy(proxy);
        }
        OkHttpClient client = builder.build();

        Request request = new Request.Builder()
                .url(url)
                .header("Content-Type", "application/dns-message")
                .post(RequestBody.create(DoHClient.encode(query)))
                .build();
        try (Response response = client.newCall(request).execute()) {
            //noinspection DataFlowIssue
            return DoHClient.decode(response.body().bytes(), sender, recipient);
        }
    }

    private static byte[] encode(@NotNull DnsQuery query) {
        NettyDnsQueryEncoder encoder = new NettyDnsQueryEncoder();
        ByteBuf buf = null;
        try {
            buf = Unpooled.buffer();
            try {
                encoder.encode(query, buf);
            } catch (Exception e) {
                throw new UnexpectedException(e);
            }
            byte[] bytes = new byte[buf.writerIndex()];
            buf.readBytes(bytes);
            return bytes;
        } finally {
            if (buf != null) {
                buf.release();
            }
        }
    }

    private static @NotNull DnsResponse decode(@NotNull byte[] response, InetSocketAddress sender, InetSocketAddress recipient) {
        NettyDatagramDnsResponseDecoder decoder = new NettyDatagramDnsResponseDecoder();
        ByteBuf buf = null;
        try {
            buf = Unpooled.buffer();
            buf.writeBytes(response);
            try {
                return decoder.decode(sender, recipient, buf);
            } catch (Exception e) {
                throw new UnexpectedException(e);
            }
        } finally {
            if (buf != null) {
                buf.release();
            }
        }
    }
}
