/*
 * Licensed to Crate under one or more contributor license agreements.
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.  Crate licenses this file
 * to you under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.  You may
 * obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 *
 * However, if you have executed another commercial license agreement
 * with Crate these terms will supersede the license and you may use the
 * software solely pursuant to the terms of the relevant commercial
 * agreement.
 */

package io.crate.http.netty;

import io.crate.blob.BlobService;
import io.crate.blob.exceptions.RedirectService;
import io.crate.blob.v2.BlobIndicesService;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.network.NetworkService;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.util.BigArrays;
import org.elasticsearch.http.netty.NettyHttpServerTransport;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.handler.stream.ChunkedWriteHandler;

public class CrateNettyHttpServerTransport extends NettyHttpServerTransport {

    private final BlobService blobService;
    protected final RedirectService redirectService;
    private final BlobIndicesService blobIndicesService;

    @Inject
    public CrateNettyHttpServerTransport(Settings settings,
                                         NetworkService networkService,
                                         BigArrays bigArrays,
                                         BlobService blobService,
                                         RedirectService redirectService,
                                         BlobIndicesService blobIndicesService) {
        super(settings, networkService, bigArrays);
        this.blobService = blobService;
        this.redirectService = redirectService;
        this.blobIndicesService = blobIndicesService;
    }

    @Override
    public ChannelPipelineFactory configureServerChannelPipelineFactory() {
        return new CrateHttpChannelPipelineFactory(this, redirectService, false, detailedErrorsEnabled);
    }

    protected static class CrateHttpChannelPipelineFactory extends HttpChannelPipelineFactory {

        private final CrateNettyHttpServerTransport transport;
        private final RedirectService redirectService;
        private final boolean sslEnabled;

        public CrateHttpChannelPipelineFactory(CrateNettyHttpServerTransport transport,
                                               RedirectService redirectService,
                                               boolean sslEnabled,
                                               boolean detailedErrorsEnabled) {
            super(transport, detailedErrorsEnabled);
            this.transport = transport;
            this.redirectService = redirectService;
            this.sslEnabled = sslEnabled;
        }

        @Override
        public ChannelPipeline getPipeline() throws Exception {
            ChannelPipeline pipeline = super.getPipeline();

            HttpBlobHandler blobHandler = new HttpBlobHandler(
                transport.blobService, redirectService, transport.blobIndicesService, sslEnabled);
            pipeline.addBefore("aggregator", "blob_handler", blobHandler);

            if (sslEnabled) {
                // required for blob support with ssl enabled (zero copy doesn't work with https)
                pipeline.addBefore("blob_handler", "chunkedWriter", new ChunkedWriteHandler());
            }
            return pipeline;
        }
    }
}
