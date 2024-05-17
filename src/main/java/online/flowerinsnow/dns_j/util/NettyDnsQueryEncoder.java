package online.flowerinsnow.dns_j.util;

import groovyjarjarantlr4.v4.runtime.misc.NotNull;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.dns.DnsQuery;
import io.netty.handler.codec.dns.DnsRecordEncoder;
import online.flowerinsnow.dns_j.exception.UnexpectedException;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class NettyDnsQueryEncoder {
    @NotNull private final Object instance;
    public NettyDnsQueryEncoder() {
        try {
            Constructor<?> constructor = Class.forName("io.netty.handler.codec.dns.DnsQueryEncoder").getDeclaredConstructor(DnsRecordEncoder.class);
            constructor.setAccessible(true);
            this.instance = constructor.newInstance(DnsRecordEncoder.DEFAULT);
        } catch (NoSuchMethodException | ClassNotFoundException | InvocationTargetException | InstantiationException |
                 IllegalAccessException e) {
            throw new UnexpectedException(e);
        }
    }

    public void encode(DnsQuery query, ByteBuf out) throws Exception {
        try {
            Method method = this.instance.getClass().getDeclaredMethod("encode", DnsQuery.class, ByteBuf.class);
            method.setAccessible(true);
            method.invoke(this.instance, query, out);
        } catch (InvocationTargetException e) {
            throw (Exception) e.getCause();
        } catch (NoSuchMethodException | IllegalAccessException | RuntimeException e) {
            throw new UnexpectedException(e);
        }
    }
}
