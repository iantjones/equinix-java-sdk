/*
 * Copyright 2021 Ian Jones. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
 * file except in compliance with the License.
 *
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under
 * the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS
 * OF ANY KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */

package com.eqixiac.equinix.messaging.transport.servicebus;

import com.azure.core.util.BinaryData;
import com.azure.core.util.IterableStream;
import com.azure.messaging.servicebus.ServiceBusReceivedMessage;
import com.azure.messaging.servicebus.ServiceBusReceiverClient;
import com.azure.messaging.servicebus.models.DeadLetterOptions;
import com.eqixiac.equinix.messaging.transport.EmgDelivery;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Unit tests with a mocked Azure client: no network, no real queue. */
class ServiceBusEmgReceiverTest {

    private final ServiceBusReceiverClient client = mock(ServiceBusReceiverClient.class);
    private final ServiceBusReceivedMessage message = mock(ServiceBusReceivedMessage.class);

    private EmgDelivery receiveOne() {
        when(client.getEntityPath()).thenReturn("prod_x_queue");
        when(client.receiveMessages(5, Duration.ofSeconds(2))).thenReturn(new IterableStream<>(List.of(message)));
        List<EmgDelivery> batch = ServiceBusEmgReceiver.wrap(client).receive(5, Duration.ofSeconds(2));
        assertEquals(1, batch.size());
        return batch.get(0);
    }

    @Test
    void exposesBodyIdAndDeliveryCount() {
        when(message.getBody()).thenReturn(BinaryData.fromString("{\"Task\":{}}"));
        when(message.getMessageId()).thenReturn("mid-1");
        when(message.getDeliveryCount()).thenReturn(3L);
        EmgDelivery d = receiveOne();
        assertArrayEquals("{\"Task\":{}}".getBytes(StandardCharsets.UTF_8), d.body());
        assertEquals("mid-1", d.messageId());
        assertEquals(3L, d.deliveryCount());
    }

    @Test
    void settlementMapsToTheAzureClient() {
        EmgDelivery d = receiveOne();
        d.complete();
        verify(client).complete(message);
        d.abandon();
        verify(client).abandon(message);
        d.deadLetter("EmgUnparseableMessage", "not JSON");
        ArgumentCaptor<DeadLetterOptions> options = ArgumentCaptor.forClass(DeadLetterOptions.class);
        verify(client).deadLetter(eq(message), options.capture());
        assertEquals("EmgUnparseableMessage", options.getValue().getDeadLetterReason());
        assertEquals("not JSON", options.getValue().getDeadLetterErrorDescription());
    }

    @Test
    void closeClosesTheClient() {
        when(client.getEntityPath()).thenReturn("q");
        ServiceBusEmgReceiver.wrap(client).close();
        verify(client).close();
    }

    @Test
    void emptyReceiveYieldsEmptyBatch() {
        when(client.getEntityPath()).thenReturn("q");
        when(client.receiveMessages(any(Integer.class), any(Duration.class))).thenReturn(new IterableStream<>(List.of()));
        assertEquals(List.of(), ServiceBusEmgReceiver.wrap(client).receive(10, Duration.ofSeconds(1)));
    }

    @Test
    void rejectedConnectionStringIsNeverEchoed() {
        String secret = "Endpoint=nonsense;SharedAccessKey=TOP-SECRET-KEY";
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> ServiceBusEmgReceiver.forQueue(secret, "q"));
        assertFalse(e.getMessage().contains("TOP-SECRET-KEY"), e.getMessage());
        assertEquals(null, e.getCause(), "the Azure cause may quote the connection string, so it is dropped");
    }

    @Test
    void toStringNamesTheEntityOnly() {
        when(client.getEntityPath()).thenReturn("prod_x_queue");
        assertEquals("ServiceBusEmgReceiver[prod_x_queue]", ServiceBusEmgReceiver.wrap(client).toString());
    }
}
