package io.serialized.client.projection.query;

import okhttp3.HttpUrl;
import org.apache.commons.lang3.Validate;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

import static io.serialized.client.projection.ProjectionType.SINGLE;

public class ListProjectionQuery implements ProjectionQuery {

  private final Class responseClass;
  private final Function<HttpUrl, HttpUrl> urlBuilder;
  private final UUID tenantId;

  private ListProjectionQuery(Function<HttpUrl, HttpUrl> urlBuilder, Class responseClass, UUID tenantId) {
    this.urlBuilder = urlBuilder;
    this.responseClass = responseClass;
    this.tenantId = tenantId;
  }

  @Override
  public HttpUrl constructUrl(HttpUrl rootUrl) {
    return urlBuilder.apply(rootUrl);
  }

  @Override
  public Optional<UUID> tenantId() {
    return Optional.ofNullable(this.tenantId);
  }

  @Override
  public Optional<Class> responseClass() {
    return Optional.ofNullable(responseClass);
  }

  public static class Builder {

    private final String projectionName;
    private Integer skip;
    private Integer limit;
    private String sort;
    private String reference;
    private UUID tenantId;

    public Builder(String projectionName) {
      this.projectionName = projectionName;
    }

    public Builder skip(int skip) {
      this.skip = skip;
      return this;
    }

    public Builder limit(int limit) {
      this.limit = limit;
      return this;
    }

    public Builder sortDescending(String field) {
      this.sort = "-" + field;
      return this;
    }

    public Builder sortAscending(String field) {
      this.sort = field;
      return this;
    }

    public Builder withTenantId(UUID tenantId) {
      this.tenantId = tenantId;
      return this;
    }

    public Builder sort(String string) {
      this.sort = string;
      return this;
    }

    public Builder reference(String reference) {
      this.reference = reference;
      return this;
    }

    private HttpUrl urlBuilder(HttpUrl rootUrl) {
      HttpUrl.Builder projections = rootUrl.newBuilder()
          .addPathSegment("projections")
          .addPathSegment(SINGLE.name().toLowerCase())
          .addPathSegment(projectionName);

      Optional.ofNullable(skip).ifPresent(limit -> projections.addQueryParameter("skip", String.valueOf(skip)));
      Optional.ofNullable(limit).ifPresent(limit -> projections.addQueryParameter("limit", String.valueOf(limit)));
      Optional.ofNullable(sort).ifPresent(limit -> projections.addQueryParameter("sort", sort));
      Optional.ofNullable(reference).ifPresent(reference -> projections.addQueryParameter("reference", reference));

      return projections.build();
    }

    public ListProjectionQuery build(Class responseClass) {
      Validate.notEmpty(projectionName, "'projectionName' must be set");
      return new ListProjectionQuery(this::urlBuilder, responseClass, tenantId);
    }

  }

}
