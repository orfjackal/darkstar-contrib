/*
 * Copyright (c) 2008, Esko Luontola. All Rights Reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 *     * Redistributions of source code must retain the above copyright notice,
 *       this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright notice,
 *       this list of conditions and the following disclaimer in the documentation
 *       and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package net.orfjackal.darkstar.rpc.comm;

import com.sun.sgs.app.*;
import net.orfjackal.darkstar.rpc.MessageReciever;
import net.orfjackal.darkstar.rpc.MessageSender;
import net.orfjackal.darkstar.rpc.core.futures.ServerFutureManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Serializable;
import java.nio.ByteBuffer;

/**
 * @author Esko Luontola
 * @since 15.6.2008
 */
public class ServerChannelAdapter implements ChannelListener, Serializable {
    private static final long serialVersionUID = 1L;
    private static final Logger logger = LoggerFactory.getLogger(ServerChannelAdapter.class);

    // server-to-client requests
    private volatile MessageReciever responseReciever;

    // client-to-server requests
    private volatile MessageReciever requestReciever;

    private final ManagedReference<? extends RpcGateway> gateway;
    private volatile ManagedReference<Channel> channel;

    public ServerChannelAdapter() {
        RpcGateway gateway = new RpcGatewayImpl(new MyRequestSender(), new MyResponseSender(), new ServerFutureManager());
        this.gateway = AppContext.getDataManager().createReference(new ManagedRpcGateway(gateway));
    }

    public void setChannel(Channel channel) {
        this.channel = AppContext.getDataManager().createReference(channel);
    }

    public RpcGateway getGateway() {
        return gateway.get();
    }

    public void receivedMessage(Channel channel, ClientSession sender, ByteBuffer message) {
        byte header = message.get();
        if (header == RpcGatewayImpl.REQUEST_TO_SERVER) {
            requestReciever.receivedMessage(ByteBufferUtils.asByteArray(message));
        } else if (header == RpcGatewayImpl.RESPONSE_FROM_CLIENT) {
            responseReciever.receivedMessage(ByteBufferUtils.asByteArray(message));
        } else {
            logger.warn("Unexpected header {} on channel {} from sender {}", new Object[]{header, channel, sender});
        }
    }

    private void sendToChannel(ByteBuffer buf) {
        if (channel == null) {
            throw new IllegalStateException("No connection");
        }
        channel.get().send(null, buf);
    }

    private class MyRequestSender implements MessageSender, Serializable {
        private static final long serialVersionUID = 1L;

        public void send(byte[] message) throws IOException {
            ByteBuffer buf = ByteBuffer.allocate(message.length + 1);
            buf.put(RpcGatewayImpl.REQUEST_TO_CLIENT);
            buf.put(message);
            buf.flip();
            sendToChannel(buf);
        }

        public void setCallback(MessageReciever callback) {
            responseReciever = callback;
        }
    }

    private class MyResponseSender implements MessageSender, Serializable {
        private static final long serialVersionUID = 1L;

        public void send(byte[] message) throws IOException {
            ByteBuffer buf = ByteBuffer.allocate(message.length + 1);
            buf.put(RpcGatewayImpl.RESPONSE_FROM_SERVER);
            buf.put(message);
            buf.flip();
            sendToChannel(buf);
        }

        public void setCallback(MessageReciever callback) {
            requestReciever = callback;
        }
    }
}
