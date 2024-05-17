package online.flowerinsnow.dns_j.util;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.dns.*;
import io.netty.util.NetUtil;
import online.flowerinsnow.dns_j.object.record.MXRecord;
import online.flowerinsnow.dns_j.object.record.SRVRecord;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;

public abstract class DNSRecordParser {
    private DNSRecordParser() {
    }

    public static @NotNull String parseA(ByteBuf content) {
        Objects.requireNonNull(content);
        byte[] bytes = new byte[4];
        content.readBytes(bytes);
        return NetUtil.bytesToIpAddress(bytes);
    }

    public static @NotNull String parseAAAA(@NotNull ByteBuf content) {
        Objects.requireNonNull(content);
        byte[] bytes = new byte[16];
        content.readBytes(bytes);
        return NetUtil.bytesToIpAddress(bytes);
    }

    /**
     * <p>解析域名内容</p>
     * <p>适用于以下内容</p>
     * <ul>
     *     <li>CNAME</li>
     *     <li>NS</li>
     *     <li>PTR</li>
     * </ul>
     * @param content 数据
     * @return 域名内容
     */
    public static @NotNull String parseName(@NotNull ByteBuf content) {
        /*
        Loop: {
            uint8 len
            char[len] name
        }
         */
        return DNSRecordParser.parseContentName(content);
    }

    /**
     * <p>解析 MX 记录内容</p>
     *
     * @param content 内容
     * @return MX 记录内容
     */
    public static @NotNull MXRecord parseMX(@NotNull ByteBuf content) {
        /*
        uint16 preference
        Loop: {
            uint8 len
            char[len] mailExchange
        }
         */
        Objects.requireNonNull(content);
        int preference = content.readUnsignedShort();
        String mailExchange = DNSRecordParser.parseName(content);
        return new MXRecord(preference, mailExchange);
    }

    /**
     * <p>解析 TXT 记录内容</p>
     *
     * @param content 数据
     * @return TXT 记录内容
     */
    public static @NotNull String parseTXT(@NotNull ByteBuf content) {
        Objects.requireNonNull(content);
        /*
        uint8 len
        char[len] txt
         */
        short length = content.readUnsignedByte();
        byte[] bytes = new byte[length];
        content.readBytes(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    /**
     * <p>解析 TXT 记录内容</p>
     *
     * @param content 数据
     * @return TXT 记录内容
     */
    public static @NotNull SRVRecord parseSRV(@NotNull ByteBuf content) {
        Objects.requireNonNull(content);
        /*
        uint16 priority
        uint16 weight
        uint16 port
        Loop: {
            uint8 len
            char[len] target
        }
         */
        int priority = content.readUnsignedShort();
        int weight = content.readUnsignedShort();
        int port = content.readUnsignedShort();
        String target = DNSRecordParser.parseName(content);
        return new SRVRecord(priority, weight, port, target);
    }

    private static @NotNull String parseContentName(@NotNull ByteBuf content) {
        Objects.requireNonNull(content);
        StringBuilder sb = new StringBuilder();
        byte len;
        boolean first = true;
        while ((len = content.readByte()) > 0) {
            if (!first) {
                sb.append('.');
            } else {
                first = false;
            }
            ByteBuf read = content.readBytes(len);
            byte[] bytes = new byte[len];
            read.readBytes(bytes);
            read.release();
            sb.append(new String(bytes, StandardCharsets.US_ASCII));
        }
        return sb.toString();
    }

    public static void logMsg(@NotNull Logger logger, @NotNull DnsMessage msg) {
        Objects.requireNonNull(logger);
        Objects.requireNonNull(msg);
        logger.info("Transaction ID: {}", msg.id());
        Map.of(
                DnsSection.QUESTION, "Queries",
                DnsSection.ANSWER, "Answers",
                DnsSection.AUTHORITY, "Authoritative nameservers",
                DnsSection.ADDITIONAL, "Additional"
        ).forEach((section, text) -> {
            int count = msg.count(section);
            if (count > 0) {
                logger.info("{}({})", text, count);
            }
            for (int i = 0; i < count; i++) {
                DNSRecordParser.logRecord(logger, msg.recordAt(section, i));
            }
        });
    }

    public static void logRecord(@NotNull Logger logger, DnsRecord record) {
        DnsRecordType type = record.type();
        if (record instanceof DnsQuestion) {
            logger.info("(TYPE={}) {}", type.name(), record.name());
        } else if (record instanceof DnsRawRecord rr) {
            ByteBuf copy = null;
            try {
                copy = rr.content().copy();
                logger.info("(TYPE={}) (TTL={}) {}", type.name(), record.timeToLive(), rr.name());
                if (type == DnsRecordType.A) {
                    logger.info("  Address: {}", DNSRecordParser.parseA(copy));
                } else if (type == DnsRecordType.NS) {
                    logger.info("  Name Server: {}", DNSRecordParser.parseName(copy));
                } else if (type == DnsRecordType.CNAME) {
                    logger.info("  CNAME: {}", DNSRecordParser.parseName(copy));
                } else if (type == DnsRecordType.PTR) {
                    logger.info("  Domain Name: {}", DNSRecordParser.parseName(copy));
                } else if (type == DnsRecordType.MX) {
                    MXRecord mxRecord = DNSRecordParser.parseMX(copy);
                    logger.info("  Preference: {}", mxRecord.preference());
                    logger.info("  Mail Exchange: {}", mxRecord.mailExchange());
                } else if (type == DnsRecordType.TXT) {
                    MXRecord mxRecord = DNSRecordParser.parseMX(copy);
                    logger.info("  TXT: {}", mxRecord.preference());
                } else if (type == DnsRecordType.SRV) {
                    SRVRecord srvRecord = DNSRecordParser.parseSRV(copy);
                    logger.info("  Priority: {}", srvRecord.priority());
                    logger.info("  Weight: {}", srvRecord.weight());
                    logger.info("  Port: {}", srvRecord.port());
                    logger.info("  Target: {}", srvRecord.target());
                } else if (type == DnsRecordType.AAAA) {
                    logger.info("  AAAA Address: {}", DNSRecordParser.parseAAAA(copy));
                }
            } finally {
                if (copy != null) {
                    copy.release();
                }
            }
        }
    }
}
