package online.flowerinsnow.dns_j.config;

import java.net.Proxy;
import java.net.SocketAddress;

public class Config {
    private String domainNameServer;
    private SocketAddress bind;
    private Proxy proxy;

    public String getDomainNameServer() {
        return this.domainNameServer;
    }

    public void setDomainNameServer(String domainNameServer) {
        this.domainNameServer = domainNameServer;
    }

    public SocketAddress getBind() {
        return this.bind;
    }

    public void setBind(SocketAddress bind) {
        this.bind = bind;
    }

    public Proxy getProxy() {
        return this.proxy;
    }

    public void setProxy(Proxy proxy) {
        this.proxy = proxy;
    }
}
