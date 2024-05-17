/*
 * Copyright 2019 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package online.flowerinsnow.dns_j.util;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.CorruptedFrameException;
import io.netty.handler.codec.dns.*;

import java.net.InetSocketAddress;

import static io.netty.util.internal.ObjectUtil.checkNotNull;

public class NettyDatagramDnsResponseDecoder {

    private final DnsRecordDecoder recordDecoder;

    public NettyDatagramDnsResponseDecoder() {
        this(DnsRecordDecoder.DEFAULT);
    }

    /**
     * Creates a new decoder with the specified {@code recordDecoder}.
     */
    public NettyDatagramDnsResponseDecoder(DnsRecordDecoder recordDecoder) {
        this.recordDecoder = checkNotNull(recordDecoder, "recordDecoder");
    }

    public final DnsResponse decode(InetSocketAddress sender, InetSocketAddress recipient, ByteBuf buffer) throws Exception {
        final int id = buffer.readUnsignedShort();

        final int flags = buffer.readUnsignedShort();
        if (flags >> 15 == 0) {
            throw new CorruptedFrameException("not a response");
        }

        final DnsResponse response = newResponse(
                sender,
                recipient,
                id,
                DnsOpCode.valueOf((byte) (flags >> 11 & 0xf)), DnsResponseCode.valueOf((byte) (flags & 0xf)));

        response.setRecursionDesired((flags >> 8 & 1) == 1);
        response.setAuthoritativeAnswer((flags >> 10 & 1) == 1);
        response.setTruncated((flags >> 9 & 1) == 1);
        response.setRecursionAvailable((flags >> 7 & 1) == 1);
        response.setZ(flags >> 4 & 0x7);

        boolean success = false;
        try {
            final int questionCount = buffer.readUnsignedShort();
            final int answerCount = buffer.readUnsignedShort();
            final int authorityRecordCount = buffer.readUnsignedShort();
            final int additionalRecordCount = buffer.readUnsignedShort();

            decodeQuestions(response, buffer, questionCount);
            if (!decodeRecords(response, DnsSection.ANSWER, buffer, answerCount)) {
                success = true;
                return response;
            }
            if (!decodeRecords(response, DnsSection.AUTHORITY, buffer, authorityRecordCount)) {
                success = true;
                return response;
            }

            decodeRecords(response, DnsSection.ADDITIONAL, buffer, additionalRecordCount);
            success = true;
            return response;
        } finally {
            if (!success) {
                response.release();
            }
        }
    }

    protected DnsResponse newResponse(InetSocketAddress sender, InetSocketAddress recipient,
                                      int id, DnsOpCode opCode, DnsResponseCode responseCode) {
        return new DatagramDnsResponse(sender, recipient, id, opCode, responseCode);
    }

    private void decodeQuestions(DnsResponse response, ByteBuf buf, int questionCount) throws Exception {
        for (int i = questionCount; i > 0; i --) {
            response.addRecord(DnsSection.QUESTION, recordDecoder.decodeQuestion(buf));
        }
    }

    private boolean decodeRecords(
            DnsResponse response, DnsSection section, ByteBuf buf, int count) throws Exception {
        for (int i = count; i > 0; i --) {
            final DnsRecord r = recordDecoder.decodeRecord(buf);
            if (r == null) {
                // Truncated response
                return false;
            }

            response.addRecord(section, r);
        }
        return true;
    }
}
