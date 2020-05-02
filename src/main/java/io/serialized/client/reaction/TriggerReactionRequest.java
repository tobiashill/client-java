package io.serialized.client.reaction;

import java.util.UUID;

public class TriggerReactionRequest {

  public final String type;
  public final UUID tenantId;
  public final UUID reactionId;

  private TriggerReactionRequest(Builder builder) {
    this.type = builder.type;
    this.tenantId = builder.tenantId;
    this.reactionId = builder.reactionId;
  }

  public boolean hasTenantId() {
    return tenantId != null;
  }

  public static class Builder {

    private String type;
    private UUID tenantId;
    private UUID reactionId;

    public Builder(String type) {
      this.type = type;
    }

    public Builder withTenantId(UUID tenantId) {
      this.tenantId = tenantId;
      return this;
    }

    public Builder withReactionId(UUID reactionId) {
      this.reactionId = reactionId;
      return this;
    }

    public TriggerReactionRequest build() {
      return new TriggerReactionRequest(this);
    }

  }

}
