package io.serialized.client.api;

import io.dropwizard.testing.junit.DropwizardClientRule;
import io.serialized.client.SerializedClientConfig;
import io.serialized.client.projection.Function;
import io.serialized.client.projection.ProjectionApiStub;
import io.serialized.client.projection.ProjectionClient;
import io.serialized.client.projection.ProjectionDefinition;
import io.serialized.client.projection.ProjectionDefinitions;
import io.serialized.client.projection.ProjectionHandler;
import io.serialized.client.projection.ProjectionResponse;
import io.serialized.client.projection.ProjectionsResponse;
import org.apache.commons.io.IOUtils;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

import static io.serialized.client.SerializedClientConfig.serializedConfig;
import static io.serialized.client.projection.EventSelector.eventSelector;
import static io.serialized.client.projection.Function.inc;
import static io.serialized.client.projection.Function.set;
import static io.serialized.client.projection.Function.setref;
import static io.serialized.client.projection.ProjectionDefinitions.newDefinitionList;
import static io.serialized.client.projection.ProjectionHandler.handler;
import static io.serialized.client.projection.TargetSelector.targetSelector;
import static io.serialized.client.projection.query.ProjectionQueries.aggregated;
import static io.serialized.client.projection.query.ProjectionQueries.list;
import static io.serialized.client.projection.query.ProjectionQueries.single;
import static java.util.Arrays.asList;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ProjectionClientIT {

  public static class OrderBalanceProjection {
    public long orderAmount;
  }

  public static class OrderTotalsProjection {
    public long orderAmount;
    public long orderCount;
  }

  private ProjectionApiStub.ProjectionApiCallback apiCallback = mock(ProjectionApiStub.ProjectionApiCallback.class);

  @Rule
  public final DropwizardClientRule DROPWIZARD = new DropwizardClientRule(new ProjectionApiStub(apiCallback));

  @Test
  public void testCreateDefinitionFromJson() throws IOException {

    String projection = getResource("/projection/simpleDefinition.json");

    ProjectionClient projectionClient = getProjectionClient();

    projectionClient.createDefinition(projection);

    ArgumentCaptor<ProjectionDefinition> captor = ArgumentCaptor.forClass(ProjectionDefinition.class);
    verify(apiCallback, times(1)).definitionCreated(captor.capture());
  }

  @Test
  public void testUpdateDefinitionFromJson() throws IOException {

    String projection = getResource("/projection/simpleDefinition.json");

    ProjectionClient projectionClient = getProjectionClient();

    projectionClient.createOrUpdate(projection);

    ArgumentCaptor<ProjectionDefinition> captor = ArgumentCaptor.forClass(ProjectionDefinition.class);
    verify(apiCallback, times(1)).definitionUpdated(captor.capture());
  }

  @Test
  public void testCreateProjectionDefinition() {

    ProjectionClient projectionClient = getProjectionClient();

    ProjectionDefinition highScoreProjection =
        ProjectionDefinition.singleProjection("high-score")
            .feed("game")
            .withIdField("winner")
            .addHandler("GameFinished",
                set().with(targetSelector("playerName")).with(eventSelector("winner")).build(),
                inc().with(targetSelector("wins")).build(),
                setref().with(targetSelector("wins")).build())
            .build();

    projectionClient.createDefinition(highScoreProjection);

    ArgumentCaptor<ProjectionDefinition> captor = ArgumentCaptor.forClass(ProjectionDefinition.class);
    verify(apiCallback, times(1)).definitionCreated(captor.capture());

    ProjectionDefinition value = captor.getValue();
    assertThat(value.getProjectionName(), is("high-score"));
    assertThat(value.getFeedName(), is("game"));
    assertThat(value.getIdField(), is("winner"));
    assertThat(value.getHandlers().size(), is(1));
    assertThat(value.getHandlers().get(0).getFunctions().size(), is(3));

    assertThat(value.getHandlers().get(0).getFunctions().get(0).getFunction(), is("set"));
    assertThat(value.getHandlers().get(0).getFunctions().get(0).getTargetSelector(), is("$.projection.playerName"));
    assertThat(value.getHandlers().get(0).getFunctions().get(0).getEventSelector(), is("$.event.winner"));

    assertThat(value.getHandlers().get(0).getFunctions().get(1).getFunction(), is("inc"));
    assertThat(value.getHandlers().get(0).getFunctions().get(1).getTargetSelector(), is("$.projection.wins"));

    assertThat(value.getHandlers().get(0).getFunctions().get(2).getFunction(), is("setref"));
    assertThat(value.getHandlers().get(0).getFunctions().get(2).getTargetSelector(), is("$.projection.wins"));
  }

  @Test
  public void testUpdateProjectionDefinition() {

    ProjectionClient projectionClient = getProjectionClient();

    ProjectionDefinition highScoreProjection =
        ProjectionDefinition.singleProjection("high-score")
            .feed("game")
            .withIdField("winner")
            .addHandler("GameFinished",
                set().with(targetSelector("playerName")).with(eventSelector("winner")).build(),
                inc().with(targetSelector("wins")).build(),
                setref().with(targetSelector("wins")).build())
            .build();

    projectionClient.createOrUpdate(highScoreProjection);

    ArgumentCaptor<ProjectionDefinition> captor = ArgumentCaptor.forClass(ProjectionDefinition.class);
    verify(apiCallback, times(1)).definitionUpdated(captor.capture());

    ProjectionDefinition value = captor.getValue();
    assertThat(value.getProjectionName(), is("high-score"));
    assertThat(value.getFeedName(), is("game"));
    assertThat(value.getIdField(), is("winner"));
    assertThat(value.getHandlers().size(), is(1));
    assertThat(value.getHandlers().get(0).getFunctions().size(), is(3));

    assertThat(value.getHandlers().get(0).getFunctions().get(0).getFunction(), is("set"));
    assertThat(value.getHandlers().get(0).getFunctions().get(0).getTargetSelector(), is("$.projection.playerName"));
    assertThat(value.getHandlers().get(0).getFunctions().get(0).getEventSelector(), is("$.event.winner"));

    assertThat(value.getHandlers().get(0).getFunctions().get(1).getFunction(), is("inc"));
    assertThat(value.getHandlers().get(0).getFunctions().get(1).getTargetSelector(), is("$.projection.wins"));

    assertThat(value.getHandlers().get(0).getFunctions().get(2).getFunction(), is("setref"));
    assertThat(value.getHandlers().get(0).getFunctions().get(2).getTargetSelector(), is("$.projection.wins"));
  }

  @Test
  public void testCreateProjectionWithSingleHandler() {

    ProjectionClient projectionClient = getProjectionClient();

    ProjectionDefinition projectionDefinition =
        ProjectionDefinition.singleProjection("high-score")
            .feed("games")
            .withIdField("winner")
            .addHandler(handler("GameFinished", inc().with(targetSelector("wins")).build())).build();

    projectionClient.createOrUpdate(projectionDefinition);

    ArgumentCaptor<ProjectionDefinition> captor = ArgumentCaptor.forClass(ProjectionDefinition.class);
    verify(apiCallback, times(1)).definitionUpdated(captor.capture());

    ProjectionDefinition value = captor.getValue();
    assertThat(value.getProjectionName(), is("high-score"));
    assertThat(value.getFeedName(), is("games"));
    assertThat(value.getIdField(), is("winner"));
    assertThat(value.getHandlers().size(), is(1));

    ProjectionHandler projectionHandler = value.getHandlers().get(0);
    assertThat(projectionHandler.getEventType(), is("GameFinished"));
    assertThat(projectionHandler.getFunctions().size(), is(1));
    Function function = projectionHandler.getFunctions().get(0);
    assertThat(function.getFunction(), is("inc"));
    assertThat(function.getEventSelector(), nullValue());
    assertThat(function.getTargetSelector(), is("$.projection.wins"));
    assertThat(function.getEventFilter(), nullValue());
    assertThat(function.getTargetFilter(), nullValue());
    assertThat(function.getRawData(), nullValue());
  }

  @Test
  public void testCreateAggregatedProjectionDefinition() {

    ProjectionClient projectionClient = getProjectionClient();

    ProjectionDefinition projectionDefinition =
        ProjectionDefinition.aggregatedProjection("game-count")
            .feed("games")
            .addHandler(handler("GameFinished", inc().with(targetSelector("count")).build())).build();

    projectionClient.createOrUpdate(projectionDefinition);

    ArgumentCaptor<ProjectionDefinition> captor = ArgumentCaptor.forClass(ProjectionDefinition.class);
    verify(apiCallback, times(1)).definitionUpdated(captor.capture());

    ProjectionDefinition value = captor.getValue();
    assertThat(value.getProjectionName(), is("game-count"));
    assertThat(value.getFeedName(), is("games"));
    assertThat(value.getIdField(), nullValue());
    assertThat(value.getHandlers().size(), is(1));

    ProjectionHandler projectionHandler = value.getHandlers().get(0);
    assertThat(projectionHandler.getEventType(), is("GameFinished"));
    assertThat(projectionHandler.getFunctions().size(), is(1));
    Function function = projectionHandler.getFunctions().get(0);
    assertThat(function.getFunction(), is("inc"));
    assertThat(function.getEventSelector(), nullValue());
    assertThat(function.getTargetSelector(), is("$.projection.count"));
    assertThat(function.getEventFilter(), nullValue());
    assertThat(function.getTargetFilter(), nullValue());
    assertThat(function.getRawData(), nullValue());
  }

  @Test
  public void testDeleteDefinition() {
    ProjectionClient projectionClient = getProjectionClient();

    String projectionName = "orders";
    projectionClient.deleteDefinition(projectionName);
    verify(apiCallback, times(1)).definitionDeleted(projectionName);
  }

  @Test
  public void testGetDefinition() {
    ProjectionClient projectionClient = getProjectionClient();

    String projectionName = "game-count";
    String feedName = "games";
    ProjectionDefinition expected =
        ProjectionDefinition.aggregatedProjection(projectionName)
            .feed(feedName)
            .addHandler(handler("GameFinished", inc().with(targetSelector("count")).build())).build();

    when(apiCallback.definitionFetched()).thenReturn(expected);

    ProjectionDefinition definition = projectionClient.getDefinition(projectionName);
    assertThat(definition.getProjectionName(), is(projectionName));
    assertThat(definition.getFeedName(), is(feedName));
    assertThat(definition.getHandlers().size(), is(1));
  }

  @Test
  public void testListDefinitions() {

    ProjectionClient projectionClient = getProjectionClient();

    String projectionName = "game-count";
    String feedName = "games";

    ProjectionDefinitions expected = newDefinitionList(asList(ProjectionDefinition.aggregatedProjection(projectionName)
        .feed(feedName)
        .addHandler(handler("GameFinished", inc().with(targetSelector("count")).build())).build()));

    when(apiCallback.definitionsFetched()).thenReturn(expected);

    ProjectionDefinitions definitions = projectionClient.listDefinitions();
    ProjectionDefinition definition = definitions.getDefinitions().get(0);
    assertThat(definition.getProjectionName(), is(projectionName));
    assertThat(definition.getFeedName(), is(feedName));
    assertThat(definition.getHandlers().size(), is(1));
  }

  @Test
  public void testSingleProjection() throws IOException {

    ProjectionClient projectionClient = getProjectionClient();

    String projectionName = "orders";
    String projectionId = "84e3565e-cd61-44e7-9769-c4663588c4dd";
    when(apiCallback.singleProjectionFetched(projectionName, projectionId)).thenReturn(getResource("/projection/getSingleProjection.json"));

    ProjectionResponse<OrderBalanceProjection> projection = projectionClient.query(
        single("orders")
            .id(projectionId)
            .build(OrderBalanceProjection.class));

    assertThat(projection.projectionId, is(projectionId));
    assertThat(projection.updatedAt, is(1505754083976L));
    assertThat(projection.data.orderAmount, is(12345L));
  }

  @Test
  public void testSingleProjectionForTenant() throws IOException {

    ProjectionClient projectionClient = getProjectionClient();

    String projectionName = "orders";
    String projectionId = "84e3565e-cd61-44e7-9769-c4663588c4dd";
    UUID tenantId = UUID.randomUUID();
    when(apiCallback.singleProjectionFetched(projectionName, projectionId, tenantId)).thenReturn(getResource("/projection/getSingleProjection.json"));

    ProjectionResponse<OrderBalanceProjection> projection = projectionClient.query(
        single("orders")
            .id(projectionId)
            .withTenantId(tenantId)
            .build(OrderBalanceProjection.class));

    assertThat(projection.projectionId, is(projectionId));
    assertThat(projection.updatedAt, is(1505754083976L));
    assertThat(projection.data.orderAmount, is(12345L));
  }

  @Test
  public void testListSingleProjections() throws IOException {

    ProjectionClient projectionClient = getProjectionClient();

    String projectionName = "orders";
    String reference = "externalId";
    when(apiCallback.singleProjectionsFetched(projectionName, reference, "-createdAt", 5, 10))
        .thenReturn(getResource("/projection/listSingleProjections.json"));

    ProjectionsResponse<Map> projections = projectionClient.query(
        list("orders").skip(5).limit(10).sortDescending("createdAt").reference(reference)
            .build(Map.class));

    assertThat(projections.projections.size(), is(1));
    assertThat(projections.hasMore, is(false));
  }

  @Test
  public void testAggregatedProjection() throws IOException {

    ProjectionClient projectionClient = getProjectionClient();

    when(apiCallback.aggregatedProjectionFetched("order-totals")).thenReturn(getResource("/projection/getAggregatedProjection.json"));

    ProjectionResponse<OrderTotalsProjection> projection = projectionClient.query(
        aggregated("order-totals")
            .build(OrderTotalsProjection.class));

    assertThat(projection.projectionId, is("order-totals"));
    assertThat(projection.updatedAt, is(1505850788368L));
    assertThat(projection.data.orderAmount, is(1000L));
    assertThat(projection.data.orderCount, is(2L));
  }


  private ProjectionClient getProjectionClient() {
    return ProjectionClient.projectionClient(getConfig()).build();
  }

  private SerializedClientConfig getConfig() {
    return serializedConfig()
        .rootApiUrl(DROPWIZARD.baseUri() + "/api-stub/")
        .accessKey("aaaaa")
        .secretAccessKey("bbbbb")
        .build();
  }

  private String getResource(String resource) throws IOException {
    return IOUtils.toString(getClass().getResourceAsStream(resource), "UTF-8");
  }

}
