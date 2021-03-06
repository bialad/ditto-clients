/*
 * Copyright (c) 2019 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.ditto.client.messaging.internal;

import static org.eclipse.ditto.model.base.common.ConditionChecker.checkNotNull;

import org.eclipse.ditto.client.configuration.AuthenticationConfiguration;
import org.eclipse.ditto.client.configuration.AccessTokenAuthenticationConfiguration;

/**
 * Implements client access token authentication for a {@link com.neovisionaries.ws.client.WebSocket} channel.
 *
 * @since 1.0.0
 */
public final class AccessTokenAuthenticationProvider extends AbstractTokenAuthenticationProvider {

    private final AccessTokenAuthenticationConfiguration configuration;

    public AccessTokenAuthenticationProvider(final AccessTokenAuthenticationConfiguration configuration) {
        super(configuration.getAdditionalHeaders(), configuration.getJsonWebTokenSupplier(), configuration.getExpiryGracePeriod());
        this.configuration = checkNotNull(configuration, "configuration");
    }

    @Override
    public AuthenticationConfiguration getConfiguration() {
        return configuration;
    }

}
