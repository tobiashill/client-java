package io.serialized.client.api;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.google.common.collect.ImmutableMap;
import io.dropwizard.testing.junit.DropwizardClientRule;
import io.serialized.client.ConcurrencyException;
import io.serialized.client.SerializedClientConfig;
import io.serialized.client.aggregate.*;
import io.serialized.client.aggregate.order.Order;
import io.serialized.client.aggregate.order.OrderPlaced;
import io.serialized.client.aggregate.order.OrderState;
import io.serialized.client.aggregate.order.OrderStatus;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import static io.serialized.client.aggregate.AggregateClient.aggregateClient;
import static io.serialized.client.aggregate.Event.newEvent;
import static io.serialized.client.aggregate.order.OrderPlaced.orderPlaced;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class AggregateClientIT {

  private final AggregateApiStub.AggregateApiCallback apiCallback = mock(AggregateApiStub.AggregateApiCallback.class);

  @Rule
  public final DropwizardClientRule dropwizard = new DropwizardClientRule(new AggregateApiStub(apiCallback));

  @Before
  public void setUp() {
    dropwizard.getObjectMapper().setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
  }

  @Test
  public void testSave() {
    UUID orderId = UUID.fromString("723ecfce-14e9-4889-98d5-a3d0ad54912f");
    String aggregateType = "order";

    AggregateClient<OrderState> orderClient = aggregateClient(aggregateType, OrderState.class, getConfig())
        .registerHandler(OrderPlaced.class, OrderState::handleOrderPlaced)
        .build();

    when(apiCallback.eventsStored(eq(orderId), any(EventBatch.class))).thenReturn(200);

    OrderState orderState = new OrderState();
    Order order = new Order(orderState);
    List<Event> events = order.placeOrder(orderId, 123L);

    orderClient.save(AggregateRequest.saveRequest().withAggregateId(orderId).withEvents(events).build());
  }

  @Test
  public void testSaveRawEventType() {
    UUID orderId = UUID.fromString("723ecfce-14e9-4889-98d5-a3d0ad54912f");
    String aggregateType = "order";

    AggregateClient<Object> orderClient = aggregateClient(aggregateType, Object.class, getConfig()).build();

    when(apiCallback.eventsStored(eq(orderId), any(EventBatch.class))).thenReturn(200);

    Event event = newEvent("order-placed").data("orderId", orderId, "customerId", UUID.randomUUID()).build();

    orderClient.save(AggregateRequest.saveRequest().withAggregateId(orderId).withEvent(event).build());
  }

  @Test(expected = ConcurrencyException.class)
  public void testConcurrencyExceptionDuringSave() {
    UUID orderId = UUID.fromString("723ecfce-14e9-4889-98d5-a3d0ad54912f");
    String aggregateType = "order";

    AggregateClient<OrderState> orderClient = aggregateClient(aggregateType, OrderState.class, getConfig())
        .registerHandler(OrderPlaced.class, OrderState::handleOrderPlaced)
        .build();

    when(apiCallback.eventsStored(eq(orderId), any(EventBatch.class))).thenReturn(409);

    OrderState orderState = new OrderState();
    Order order = new Order(orderState);
    List<Event> events = order.placeOrder(orderId, 123L);

    orderClient.save(AggregateRequest.saveRequest().withAggregateId(orderId).withEvents(events).build());
  }

  @Test
  public void testUpdate() throws IOException {
    UUID orderId = UUID.fromString("723ecfce-14e9-4889-98d5-a3d0ad54912f");
    String aggregateType = "order";

    AggregateClient<OrderState> orderClient = aggregateClient(aggregateType, OrderState.class, getConfig())
        .registerHandler(OrderPlaced.class, OrderState::handleOrderPlaced)
        .build();

    when(apiCallback.aggregateLoaded(aggregateType, orderId.toString())).thenReturn(getResource("/aggregate/load_aggregate.json"));
    when(apiCallback.eventsStored(eq(orderId), any(EventBatch.class))).thenReturn(200);

    orderClient.update(orderId, orderState -> new Order(orderState).cancel());
  }

  @Test
  public void testDeleteAggregateById() {
    UUID orderId = UUID.fromString("11111111-2222-3333-4444-555555555555");
    UUID deleteToken = UUID.fromString("99999999-9999-9999-9999-999999999999");
    String aggregateType = "order";

    AggregateClient<OrderState> orderClient = aggregateClient(aggregateType, OrderState.class, getConfig()).build();

    when(apiCallback.aggregateDeleteRequested(aggregateType, orderId.toString())).thenReturn(ImmutableMap.of("deleteToken", deleteToken));

    orderClient.delete(orderId).confirm();

    verify(apiCallback).aggregateDeletePerformed(aggregateType, orderId.toString(), deleteToken.toString());
  }

  @Test
  public void testDeleteAggregateByType() {
    UUID deleteToken = UUID.fromString("99999999-9999-9999-9999-999999999999");
    String aggregateType = "order";

    AggregateClient<OrderState> orderClient = aggregateClient(aggregateType, OrderState.class, getConfig()).build();

    when(apiCallback.aggregateTypeDeleteRequested(aggregateType)).thenReturn(ImmutableMap.of("deleteToken", deleteToken));

    orderClient.delete().confirm();

    verify(apiCallback).aggregateTypeDeletePerformed(aggregateType, deleteToken.toString());
  }

  @Test(expected = ConcurrencyException.class)
  public void testConcurrencyExceptionDuringUpdate() throws IOException {
    UUID orderId = UUID.fromString("723ecfce-14e9-4889-98d5-a3d0ad54912f");
    String aggregateType = "order";

    AggregateClient<OrderState> orderClient = aggregateClient(aggregateType, OrderState.class, getConfig())
        .registerHandler(OrderPlaced.class, OrderState::handleOrderPlaced)
        .build();

    when(apiCallback.aggregateLoaded(aggregateType, orderId.toString())).thenReturn(getResource("/aggregate/load_aggregate.json"));
    when(apiCallback.eventsStored(eq(orderId), any(EventBatch.class))).thenReturn(409);

    orderClient.update(orderId, orderState -> new Order(orderState).cancel());
  }

  @Test
  public void testLoadAggregateState() throws IOException {
    UUID orderId = UUID.fromString("723ecfce-14e9-4889-98d5-a3d0ad54912f");
    String aggregateType = "order";

    AggregateClient<OrderState> orderClient = aggregateClient(aggregateType, OrderState.class, getConfig())
        .registerHandler(OrderPlaced.class, OrderState::handleOrderPlaced)
        .build();

    when(apiCallback.aggregateLoaded(aggregateType, orderId.toString())).thenReturn(getResource("/aggregate/load_aggregate.json"));

    orderClient.update(orderId, orderState -> {
      assertThat(orderState.status(), is(OrderStatus.PLACED));
      return emptyList();
    });

  }

  @Test
  public void testAggregateExist() {
    UUID orderId = UUID.fromString("723ecfce-14e9-4889-98d5-a3d0ad54912f");
    String aggregateType = "order";

    AggregateClient<OrderState> orderClient = aggregateClient(aggregateType, OrderState.class, getConfig())
        .registerHandler(OrderPlaced.class, OrderState::handleOrderPlaced)
        .build();

    when(apiCallback.aggregateChecked(aggregateType, orderId.toString())).thenReturn(true);

    assertTrue(orderClient.exists(orderId));
    assertFalse(orderClient.exists(UUID.randomUUID()));
  }

  @Test
  public void testStoreEvents() {

    AggregateClient<OrderState> orderClient = getOrderClient("order");

    UUID aggregateId = UUID.randomUUID();
    when(apiCallback.eventsStored(eq(aggregateId), any(EventBatch.class))).thenReturn(200);

    orderClient.save(AggregateRequest.saveRequest().withAggregateId(aggregateId).withEvents(singletonList(orderPlaced("order-123", 1234L))).build());

    ArgumentCaptor<EventBatch> eventsStoredCaptor = ArgumentCaptor.forClass(EventBatch.class);
    verify(apiCallback).eventsStored(eq(aggregateId), eventsStoredCaptor.capture());

    EventBatch eventsStored = eventsStoredCaptor.getValue();
    List<Event> events = eventsStored.getEvents();
    assertThat(events.size(), is(1));
    Event event = events.get(0);
    assertThat(event.getEventType(), is(OrderPlaced.class.getSimpleName()));
    assertNotNull(event.getData());
  }

  @Test
  public void testStoreEventsForTenant() {

    AggregateClient<OrderState> orderClient = getOrderClient("order");

    UUID aggregateId = UUID.randomUUID();
    UUID tenantId = UUID.randomUUID();
    List<Event> events = singletonList(orderPlaced("order-123", 1234L));
    when(apiCallback.eventsStored(eq(aggregateId), any(EventBatch.class), any(UUID.class))).thenReturn(200);

    AggregateRequest aggregateRequest = AggregateRequest.saveRequest().withTenantId(tenantId).withAggregateId(aggregateId).withEvents(events).build();
    orderClient.save(aggregateRequest);

    ArgumentCaptor<EventBatch> eventsStoredCaptor = ArgumentCaptor.forClass(EventBatch.class);
    verify(apiCallback).eventsStored(eq(aggregateId), eventsStoredCaptor.capture(), eq(tenantId));

    EventBatch eventsStored = eventsStoredCaptor.getValue();
    assertThat(eventsStored.getEvents().size(), is(1));
    Event event = eventsStored.getEvents().get(0);
    assertThat(event.getEventType(), is(OrderPlaced.class.getSimpleName()));
    assertNotNull(event.getData());
  }

  private AggregateClient<OrderState> getOrderClient(String order) {
    return aggregateClient(order, OrderState.class, getConfig())
        .registerHandler("order-placed", OrderPlaced.class, OrderState::handleOrderPlaced)
        .build();
  }

  private SerializedClientConfig getConfig() {
    return SerializedClientConfig.serializedConfig()
        .rootApiUrl(dropwizard.baseUri() + "/api-stub/")
        .accessKey("aaaaa")
        .secretAccessKey("bbbbb").build();
  }

  private String getResource(String resource) throws IOException {
    return IOUtils.toString(getClass().getResourceAsStream(resource), "UTF-8");
  }

}
