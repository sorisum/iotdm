/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.onem2m.protocols.http.tx;

import org.eclipse.jetty.client.ContentExchange;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.opendaylight.iotdm.onem2m.protocols.common.Onem2mProtocolTxChannel;
import org.opendaylight.iotdm.onem2m.protocols.http.Onem2mHttpSecureConnectionConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Common HTTP and HTTPS client base class for HTTP(s) notifier and routing plugins.
 */
public abstract class Onem2mHttpClient implements Onem2mProtocolTxChannel<Onem2mHttpClientConfiguration> {
    private static final Logger LOG = LoggerFactory.getLogger(Onem2mHttpClient.class);

    protected HttpClient client = null;
    protected String pluginName = "http"; // http by default

    @Override
    public void start(Onem2mHttpClientConfiguration configuration)
            throws IllegalArgumentException, RuntimeException {

        SslContextFactory sslContextFactory = null;

        try {
            if (null != configuration &&
                configuration.isSecureConnection() &&
                null != configuration.getSecureConnectionConfig() &&
                null != configuration.getSecureConnectionConfig().getTrustStoreConfig()) {

                sslContextFactory = new SslContextFactory(false);
                sslContextFactory.setTrustStore(
                        configuration.getSecureConnectionConfig().getTrustStoreConfig().getTrustStoreFile());
                sslContextFactory.setTrustStorePassword(
                        configuration.getSecureConnectionConfig().getTrustStoreConfig().getTrustStorePassword());

                // TODO: Jetty 8 requires also keystore to be set, can be removed for upper versions
                if (null != configuration.getSecureConnectionConfig().getKeyStoreConfig()) {
                    sslContextFactory.setKeyStorePath(
                            configuration.getSecureConnectionConfig().getKeyStoreConfig().getKeyStoreFile());
                    sslContextFactory.setKeyStorePassword(
                            configuration.getSecureConnectionConfig().getKeyStoreConfig().getKeyStorePassword());
                    if (null != configuration.getSecureConnectionConfig().getKeyStoreConfig().getKeyManagerPassword()) {
                        sslContextFactory.setKeyManagerPassword(
                                configuration.getSecureConnectionConfig().getKeyStoreConfig().getKeyManagerPassword());
                    } else {
                        sslContextFactory.setKeyManagerPassword(
                                configuration.getSecureConnectionConfig().getKeyStoreConfig().getKeyStorePassword());
                    }
                } else {
                    LOG.error("Keystore not configured, required by jetty 8");
                }

                client = new HttpClient(sslContextFactory);
                this.pluginName = "https";
                LOG.info("Starting HTTPS client.");
            } else {
                client = new HttpClient();
                LOG.info("Starting HTTP client.");
            }

            client.start();
        } catch (Exception e) {
            LOG.error("Failed to start client:: {}", e);
        }
    }

    @Override
    public void close() {
        try {
            client.stop();
        } catch (Exception e) {
            LOG.error("Failed to close client: {}", e);
        }
    }

    /**
     * Sends request passed in the content exchange.
     * @param ex The content exchange including all data to be sent.
     * @throws IOException
     */
    public void send(ContentExchange ex) throws IOException {
        this.client.send(ex);
    }
}