package io.serialized.client.projection;

import java.util.ArrayList;
import java.util.List;

import static java.util.Arrays.asList;

public class ProjectionDefinition {

  private String projectionName;
  private String feedName;
  private String type;
  private String idField;
  private List<ProjectionHandler> handlers;

  // For serialization
  private ProjectionDefinition() {
  }

  private ProjectionDefinition(String projectionName, String feedName, boolean aggregated, String idField, List<ProjectionHandler> handlers) {
    this.projectionName = projectionName;
    this.feedName = feedName;
    this.type = aggregated ? "aggregated" : "single";
    this.idField = idField;
    this.handlers = handlers;
  }

  public static SingleProjectionBuilder singleProjection(String projectionName) {
    return new SingleProjectionBuilder(projectionName);
  }

  public static AggregatedProjectionBuilder aggregatedProjection(String projectionName) {
    return new AggregatedProjectionBuilder(projectionName);
  }

  public String projectionName() {
    return projectionName;
  }

  public static class AggregatedProjectionBuilder {

    private List<ProjectionHandler> handlers = new ArrayList<>();
    private final String projectionName;
    private String feedName;

    public AggregatedProjectionBuilder(String projectionName) {
      this.projectionName = projectionName;
    }

    public AggregatedProjectionBuilder feed(String feedName) {
      this.feedName = feedName;
      return this;
    }

    public AggregatedProjectionBuilder addHandler(ProjectionHandler handler) {
      this.handlers.add(handler);
      return this;
    }

    public AggregatedProjectionBuilder withHandler(String eventType, ProjectionHandler.Function... functions) {
      ProjectionHandler.Builder builder = ProjectionHandler.handler(eventType);
      asList(functions).forEach(builder::addFunction);
      return addHandler(builder.build());
    }

    public ProjectionDefinition build() {
      return new ProjectionDefinition(projectionName, feedName, true, null, handlers);
    }
  }

  public static class SingleProjectionBuilder {

    private List<ProjectionHandler> handlers = new ArrayList<>();
    private final String projectionName;
    private String feedName;
    private String idField;

    public SingleProjectionBuilder(String projectionName) {
      this.projectionName = projectionName;
    }

    public SingleProjectionBuilder feed(String feedName) {
      this.feedName = feedName;
      return this;
    }

    public SingleProjectionBuilder addHandler(ProjectionHandler handler) {
      this.handlers.add(handler);
      return this;
    }

    public SingleProjectionBuilder withHandler(String eventType, ProjectionHandler.Function... functions) {
      ProjectionHandler.Builder builder = ProjectionHandler.handler(eventType);
      asList(functions).forEach(builder::addFunction);
      return addHandler(builder.build());
    }

    public SingleProjectionBuilder withIdField(String idField) {
      this.idField = idField;
      return this;
    }

    public ProjectionDefinition build() {
      return new ProjectionDefinition(projectionName, feedName, false, idField, handlers);
    }
  }

}
