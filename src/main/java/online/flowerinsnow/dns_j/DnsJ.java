package online.flowerinsnow.dns_j;

import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import io.netty.handler.codec.dns.DnsQuery;
import io.netty.handler.codec.dns.DnsResponse;
import online.flowerinsnow.dns_j.client.DoHClient;
import online.flowerinsnow.dns_j.config.Config;
import online.flowerinsnow.dns_j.server.UDPDNSServer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.*;
import java.nio.charset.StandardCharsets;

public class DnsJ {
    private static UDPDNSServer server;

    public static void main(String[] args) {
        final Logger logger = LogManager.getLogger("Main");

        // 加载配置文件（Groovy脚本）
        Binding binding = new Binding();
        Config config = new Config();
        binding.setProperty("config", config);

        // 运行配置文件（Groovy脚本）
        GroovyShell shell = new GroovyShell(binding);
        try (InputStream in = DnsJ.class.getResourceAsStream("/config.groovy")) {
            //noinspection DataFlowIssue
            try (InputStreamReader isr = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                shell.evaluate(isr);
            }
        } catch (IOException e) {
            logger.error("读取 config.groovy 失败", e);
            System.exit(-1);
            return;
        }

        // 检查 DoH 地址是否合法
        URL dnsServerURL;
        try {
            dnsServerURL = URI.create(config.getDomainNameServer()).toURL();
        } catch (MalformedURLException e) {
            logger.error("{} 不是合法 DoH 服务器", config.getDomainNameServer());
            logger.throwing(e);
            System.exit(-1);
            return;
        }

        // 创建服务器
        DnsJ.server = new UDPDNSServer(config.getBind()) {
            @Override
            public @NotNull DnsResponse getResponse(@NotNull DnsQuery query, @NotNull InetSocketAddress sender, @NotNull InetSocketAddress recipient) {
                try {
                    return DoHClient.query(dnsServerURL, query, config.getProxy(), sender, recipient);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        };
        // 启动服务器
        try {
            DnsJ.server.start();
        } catch (BindException e) {
            logger.error("地址 {} 正在使用，绑定失败", config.getBind());
            DnsJ.server.close();
            System.exit(-1);
            return;
        }

        // 添加关闭时释放代码 hook
        Runtime.getRuntime().addShutdownHook(new Thread(DnsJ.server::close));
    }
}
