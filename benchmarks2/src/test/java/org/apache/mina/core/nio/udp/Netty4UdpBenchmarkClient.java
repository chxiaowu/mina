/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 */
package org.apache.mina.core.nio.udp;

import io.netty.bootstrap.ChannelFactory;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.Channels;
import java.util.concurrent.CountDownLatch;

import org.apache.mina.core.BenchmarkClient;

/**
 * A Netty 3 based UDP client
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class Netty4UdpBenchmarkClient implements BenchmarkClient {

    private ChannelFactory factory;

    /**
     * 
     */
    public Netty4UdpBenchmarkClient() {
    }

    /**
     * {@inheritedDoc}
     */
    public void start(final int port, final CountDownLatch counter, final byte[] data) throws IOException {
        factory = new NioDatagramChannelFactory();
        ConnectionlessBootstrap bootstrap = new ConnectionlessBootstrap(factory);
        bootstrap.setOption("sendBufferSize", 65536);
        bootstrap.setPipelineFactory(new ChannelPipelineFactory() {
            public ChannelPipeline getPipeline() throws Exception {
                return Channels.pipeline(new SimpleChannelUpstreamHandler() {
                    private void sendMessage(ChannelHandlerContext ctx, byte[] data) {
                        ChannelBuffer buffer = ChannelBuffers.wrappedBuffer(data);
                        ctx.getChannel().write(buffer);
                    }

                    @Override
                    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
                        if (e.getMessage() instanceof ChannelBuffer) {
                            ChannelBuffer buffer = (ChannelBuffer) e.getMessage();
                            for (int i = 0; i < buffer.readableBytes(); ++i) {
                                counter.countDown();
                                if (counter.getCount() > 0) {
                                    sendMessage(ctx, data);
                                } else {
                                    ctx.getChannel().close();
                                }
                            }
                        } else {
                            throw new IllegalArgumentException(e.getMessage().getClass().getName());
                        }
                    }

                    @Override
                    public void channelConnected(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
                        sendMessage(ctx, data);
                    }

                });
            }
        });
        bootstrap.connect(new InetSocketAddress(port));
    }

    /**
     * {@inheritedDoc}
     */
    public void stop() throws IOException {
        factory.shutdown();
        factory.releaseExternalResources();
    }
}
