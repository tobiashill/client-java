package io.serialized.client.api;

import io.dropwizard.testing.junit.DropwizardClientRule;
import io.serialized.client.SerializedClientConfig;
import io.serialized.client.feed.*;
import org.apache.commons.io.IOUtils;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class FeedClientIT {

  private FeedApiStub.FeedApiCallback apiCallback = mock(FeedApiStub.FeedApiCallback.class);

  @Rule
  public final DropwizardClientRule DROPWIZARD = new DropwizardClientRule(new FeedApiStub(apiCallback));

  @Test
  public void listFeeds() throws IOException {

    FeedClient feedClient = getFeedClient();

    when(apiCallback.feedOverviewLoaded()).thenReturn(getResource("/feed/feeds.json"));

    List<Feed> feeds = feedClient.listFeeds();
    assertThat(feeds.size(), is(1));
    assertThat(feeds.get(0).aggregateType(), is("games"));
    assertThat(feeds.get(0).aggregateCount(), is(10L));
    assertThat(feeds.get(0).batchCount(), is(48L));
    assertThat(feeds.get(0).eventCount(), is(96L));
  }

  @Test
  public void feedEntries() throws IOException {

    FeedClient feedClient = getFeedClient();
    String feedName = "games";

    ArgumentCaptor<FeedApiStub.QueryParams> queryParams = ArgumentCaptor.forClass(FeedApiStub.QueryParams.class);
    when(apiCallback.feedEntriesLoaded(eq(feedName), queryParams.capture())).thenReturn(getResource("/feed/feedentries.json"));

    FeedResponse feedResponse = feedClient.feed(feedName).execute();

    assertThat(queryParams.getValue().getLimit(), is(1000));
    assertThat(feedResponse.entries().size(), is(48));
    assertThat(feedResponse.events().size(), is(96));
  }

  @Test
  public void feedEntriesWithLimit() throws IOException {

    FeedClient feedClient = getFeedClient();
    String feedName = "games";

    int limit = 10;
    ArgumentCaptor<FeedApiStub.QueryParams> queryParams = ArgumentCaptor.forClass(FeedApiStub.QueryParams.class);
    when(apiCallback.feedEntriesLoaded(eq(feedName), queryParams.capture())).thenReturn(getResource("/feed/feedentries-limit.json"));

    FeedResponse feedResponse = feedClient.feed(feedName).limit(limit).execute();

    assertThat(queryParams.getValue().getLimit(), is(10));
    assertThat(feedResponse.entries().size(), is(10));
    assertThat(feedResponse.events().size(), is(20));
  }

  @Test
  public void feedEntriesWithLimitAndSince() throws IOException {

    FeedClient feedClient = getFeedClient();
    String feedName = "games";

    int limit = 10;
    ArgumentCaptor<FeedApiStub.QueryParams> queryParams = ArgumentCaptor.forClass(FeedApiStub.QueryParams.class);
    when(apiCallback.feedEntriesLoaded(eq(feedName), queryParams.capture())).thenReturn(getResource("/feed/feedentries-limit.json"));

    FeedResponse feedResponse = feedClient.feed(feedName).limit(limit).execute(3);

    assertThat(queryParams.getValue().getLimit(), is(10));
    assertThat(queryParams.getValue().getSince(), is(3L));
    assertThat(feedResponse.entries().size(), is(10));
    assertThat(feedResponse.events().size(), is(20));
  }

  @Test
  public void feedEntriesWithCallback() throws IOException {

    FeedClient feedClient = getFeedClient();
    String feedName = "games";

    ArgumentCaptor<FeedApiStub.QueryParams> queryParams = ArgumentCaptor.forClass(FeedApiStub.QueryParams.class);
    when(apiCallback.feedEntriesLoaded(eq(feedName), queryParams.capture())).thenReturn(getResource("/feed/feedentries-limit.json"));

    AtomicInteger entries = new AtomicInteger();
    AtomicInteger events = new AtomicInteger();
    AtomicLong lastProcessedEntry = new AtomicLong();

    feedClient.feed(feedName).limit(10).execute(3, new FeedEntryProcessor() {
      @Override
      public void process(FeedEntry feedEntry) {
        entries.incrementAndGet();
        events.addAndGet(feedEntry.events().size());
      }

      @Override
      public void onSuccess(Long sequenceNumber) {
        lastProcessedEntry.set(sequenceNumber);
      }
    });

    assertThat(queryParams.getValue().getLimit(), is(10));
    assertThat(queryParams.getValue().getSince(), is(3L));

    assertThat(entries.get(), is(10));
    assertThat(events.get(), is(20));

    assertThat(lastProcessedEntry.get(), is(13L));
  }

  private FeedClient getFeedClient() {
    return FeedClient.feedClient(
        SerializedClientConfig.serializedConfig()
            .rootApiUrl(DROPWIZARD.baseUri() + "/api-stub/")
            .accessKey("aaaaa")
            .secretAccessKey("bbbbb")
            .build())
        .build();
  }

  private String getResource(String resource) throws IOException {
    return IOUtils.toString(getClass().getResourceAsStream(resource), "UTF-8");
  }

}
