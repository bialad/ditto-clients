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
package org.eclipse.ditto.client.messaging;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;

import org.eclipse.ditto.client.configuration.AuthenticationConfiguration;
import org.eclipse.ditto.client.configuration.MessagingConfiguration;
import org.eclipse.ditto.client.internal.bus.AdaptableBus;
import org.eclipse.ditto.client.internal.bus.Classification;
import org.eclipse.ditto.model.messages.Message;
import org.eclipse.ditto.protocoladapter.Adaptable;
import org.eclipse.ditto.protocoladapter.ProtocolFactory;
import org.eclipse.ditto.protocoladapter.TopicPath;
import org.eclipse.ditto.signals.commands.base.Command;
import org.eclipse.ditto.signals.commands.base.CommandResponse;
import org.eclipse.ditto.signals.events.base.Event;

/**
 * Interface to be used when implementing a messaging provider for the Things client.
 *
 * @since 1.0.0
 */
public interface MessagingProvider {

    /**
     * Initializes the Messaging Provider by opening the underlying connections, etc.
     * Blocks the calling thread until messaging provider is ready.
     * @deprecated since 1.3.0. Use {@code initializeAsync} instead.
     */
    @Deprecated
    default void initialize() {
        initializeAsync().toCompletableFuture().join();
    }

    /**
     * Perform initialization asynchronously.
     *
     * @return a future that completes after initialization completed.
     * @since 1.3.0
     */
    CompletionStage<?> initializeAsync();

    /**
     * Returns the {@code AuthenticationConfiguration} of this provider.
     *
     * @return the configuration.
     */
    AuthenticationConfiguration getAuthenticationConfiguration();

    /**
     * Returns the {@code MessagingConfiguration} of this provider.
     *
     * @return the configuration.
     */
    MessagingConfiguration getMessagingConfiguration();

    /**
     * Returns the {@code ExecutorService} of this provider.
     *
     * @return the executor service.
     */
    ExecutorService getExecutorService();

    /**
     * Returns the {@code AdaptableBus} to which all incoming messages are published.
     *
     * @return the adaptable bus.
     * @since 1.1.0
     */
    AdaptableBus getAdaptableBus();

    /**
     * Register a subscription message by key to send on reconnect.
     * Replace previously registered subscription messages with the same key.
     * It should be a no-op for messaging providers over channels where no subscription message is allowed,
     * e. g., MQTT.
     *
     * @param key the key of the subscription message.
     * @param message the subscription message.
     * @return this object.
     */
    MessagingProvider registerSubscriptionMessage(Object key, String message);

    /**
     * Remove a subscription message to send on reconnect by its key.
     * It should be a no-opo for messaging providers over channels where no subscription message is allowed,
     * e. g., MQTT.
     *
     * @param key the key with which the subscription message is registered.
     * @return this object.
     */
    MessagingProvider unregisterSubscriptionMessage(Object key);

    /**
     * Send a message into the channel provided by this provider.
     *
     * @param message the message to emit.
     * @since 1.1.0
     */
    void emit(String message);

    /**
     * Emit an adaptable message in a fire-and-forget manner.
     *
     * @param message the message to emit.
     * @since 1.1.0
     */
    default void emitAdaptable(Adaptable message) {
        emit(ProtocolFactory.wrapAsJsonifiableAdaptable(message).toJsonString());
    }

    /**
     * Send Ditto Protocol {@link Adaptable} using the underlying connection and expect a response.
     *
     * @param adaptable the adaptable to be sent
     * @return a CompletableFuture containing the correlated response to the sent {@code dittoProtocolAdaptable}
     */
    default CompletableFuture<Adaptable> sendAdaptable(Adaptable adaptable) {
        final String correlationId = adaptable.getDittoHeaders()
                .getCorrelationId()
                .orElseGet(() -> UUID.randomUUID().toString());
        final Adaptable adaptableToSend = adaptable.getDittoHeaders()
                .getCorrelationId()
                .map(cid -> adaptable)
                .orElseGet(() -> adaptable.setDittoHeaders(
                        adaptable.getDittoHeaders().toBuilder().correlationId(correlationId).build())
                );
        final Duration timeout = getMessagingConfiguration().getTimeout();
        final CompletableFuture<Adaptable> result = getAdaptableBus()
                .subscribeOnceForAdaptable(Classification.forCorrelationId(correlationId), timeout)
                .toCompletableFuture();
        emitAdaptable(adaptableToSend);
        return result;
    }

    /**
     * Send message using the underlying connection.
     * Throw {@code UnsupportedOperationException}.
     * Protocol-relevant concerns are moved away from messaging providers into API handles.
     *
     * @param message ignored.
     * @param channel ignored.
     * @throws UnsupportedOperationException always.
     */
    @Deprecated
    default void send(Message<?> message, TopicPath.Channel channel) {
        throw new UnsupportedOperationException();
    }

    /**
     * Throw {@code UnsupportedOperationException}.
     * Protocol-relevant concerns are moved away from messaging providers into API handles.
     *
     * @param command ignored.
     * @param channel ignored.
     * @throws UnsupportedOperationException always.
     */
    @Deprecated
    default void sendCommand(Command<?> command, TopicPath.Channel channel) {
        throw new UnsupportedOperationException();
    }

    /**
     * Throw {@code UnsupportedOperationException}.
     * Protocol-relevant concerns are moved away from messaging providers into API handles.
     *
     * @param commandResponse ignored.
     * @param channel ignored.
     * @throws UnsupportedOperationException always.
     */
    @Deprecated
    default void sendCommandResponse(CommandResponse<?> commandResponse, TopicPath.Channel channel) {
        throw new UnsupportedOperationException();
    }

    /**
     * Throw {@code UnsupportedOperationException}.
     * Protocol-relevant concerns are moved away from messaging providers into API handles.
     * Use {@code this#emitAdaptable(Adaptable)} or {@code this#emit(String)} instead.
     *
     * @param event ignored.
     * @param channel ignored.
     * @throws UnsupportedOperationException always.
     */
    @Deprecated
    default void emitEvent(Event<?> event, TopicPath.Channel channel) {
        throw new UnsupportedOperationException();
    }

    /**
     * Throw {@code UnsupportedOperationException}.
     * Protocol-relevant concerns are moved away from messaging providers into API handles.
     *
     * @param commandResponseHandler ignored.
     * @throws UnsupportedOperationException always.
     */
    @Deprecated
    default void registerReplyHandler(Consumer<CommandResponse<?>> commandResponseHandler) {
        throw new UnsupportedOperationException();
    }

    /**
     * Throw {@code UnsupportedOperationException}.
     * Protocol-relevant concerns are moved away from messaging providers into API handles.
     *
     * @param name ignored.
     * @param registrationConfig ignored.
     * @param handler ignored.
     * @param receiptFuture ignored.
     * @return nothing.
     * @throws UnsupportedOperationException always.
     */
    @Deprecated
    default boolean registerMessageHandler(String name,
            Map<String, String> registrationConfig,
            Consumer<Message<?>> handler,
            CompletableFuture<Void> receiptFuture) {
        throw new UnsupportedOperationException();
    }

    /**
     * Throw {@code UnsupportedOperationException}.
     * Protocol-relevant concerns are moved away from messaging providers into API handles.
     *
     * @param name ignored.
     * @param future ignored.
     * @throws UnsupportedOperationException always.
     */
    @Deprecated
    default void deregisterMessageHandler(String name, CompletableFuture<Void> future) {
        throw new UnsupportedOperationException();
    }

    /**
     * Close the underlying connection.
     */
    void close();

}
