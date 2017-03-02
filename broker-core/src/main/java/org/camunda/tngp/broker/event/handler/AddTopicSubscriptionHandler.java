package org.camunda.tngp.broker.event.handler;

import java.util.concurrent.CompletableFuture;

import org.agrona.DirectBuffer;
import org.camunda.tngp.broker.event.processor.TopicSubscription;
import org.camunda.tngp.broker.event.processor.TopicSubscriptionManager;
import org.camunda.tngp.broker.logstreams.BrokerEventMetadata;
import org.camunda.tngp.broker.transport.clientapi.ErrorResponseWriter;
import org.camunda.tngp.broker.transport.controlmessage.ControlMessageHandler;
import org.camunda.tngp.broker.transport.controlmessage.ControlMessageResponseWriter;
import org.camunda.tngp.protocol.clientapi.ControlMessageType;
import org.camunda.tngp.protocol.clientapi.ErrorCode;

public class AddTopicSubscriptionHandler implements ControlMessageHandler
{

    protected final TopicSubscription subscription = new TopicSubscription();

    protected final TopicSubscriptionManager subscriptionManager;
    protected final ControlMessageResponseWriter responseWriter;
    protected final ErrorResponseWriter errorResponseWriter;

    public AddTopicSubscriptionHandler(
            TopicSubscriptionManager subscriptionManager,
            ControlMessageResponseWriter responseWriter,
            ErrorResponseWriter errorResponseWriter)
    {
        this.subscriptionManager = subscriptionManager;
        this.responseWriter = responseWriter;
        this.errorResponseWriter = errorResponseWriter;
    }

    @Override
    public ControlMessageType getMessageType()
    {
        return ControlMessageType.ADD_TOPIC_SUBSCRIPTION;
    }

    @Override
    public CompletableFuture<Void> handle(DirectBuffer buffer, BrokerEventMetadata metadata)
    {
        subscription.reset();
        subscription.wrap(buffer);
        subscription.setChannelId(metadata.getReqChannelId());

        final CompletableFuture<Void> future = subscriptionManager.addSubscription(subscription);

        return future.handle((v, failure) ->
        {
            if (failure == null)
            {
                responseWriter
                    .brokerEventMetadata(metadata)
                    .dataWriter(subscription)
                    .tryWriteResponse();
            }
            else
            {
                sendError(buffer, metadata, "Cannot add topic subscription. %s", failure.getMessage());
            }
            return null;
        });
    }

    protected void sendError(DirectBuffer request, BrokerEventMetadata metadata, String message, Object... args)
    {
        errorResponseWriter
            .metadata(metadata)
            .errorCode(ErrorCode.REQUEST_PROCESSING_FAILURE)
            .errorMessage(message, args)
            .failedRequest(request, 0, request.capacity())
            .tryWriteResponseOrLogFailure();
    }

}
