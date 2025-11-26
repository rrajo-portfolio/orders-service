package com.portfolio.orders.generated.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.portfolio.orders.generated.model.CreateOrderItem;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * UpdateOrderRequest
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2025-11-24T01:05:45.244357200+01:00[Europe/Madrid]", comments = "Generator version: 7.5.0")
public class UpdateOrderRequest {

  private String notes;

  @Valid
  private List<@Valid CreateOrderItem> items = new ArrayList<>();

  public UpdateOrderRequest() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public UpdateOrderRequest(String notes, List<@Valid CreateOrderItem> items) {
    this.notes = notes;
    this.items = items;
  }

  public UpdateOrderRequest notes(String notes) {
    this.notes = notes;
    return this;
  }

  /**
   * Get notes
   * @return notes
  */
  @NotNull @Size(max = 300) 
  @Schema(name = "notes", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("notes")
  public String getNotes() {
    return notes;
  }

  public void setNotes(String notes) {
    this.notes = notes;
  }

  public UpdateOrderRequest items(List<@Valid CreateOrderItem> items) {
    this.items = items;
    return this;
  }

  public UpdateOrderRequest addItemsItem(CreateOrderItem itemsItem) {
    if (this.items == null) {
      this.items = new ArrayList<>();
    }
    this.items.add(itemsItem);
    return this;
  }

  /**
   * Get items
   * @return items
  */
  @NotNull @Valid @Size(min = 1) 
  @Schema(name = "items", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("items")
  public List<@Valid CreateOrderItem> getItems() {
    return items;
  }

  public void setItems(List<@Valid CreateOrderItem> items) {
    this.items = items;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    UpdateOrderRequest updateOrderRequest = (UpdateOrderRequest) o;
    return Objects.equals(this.notes, updateOrderRequest.notes) &&
        Objects.equals(this.items, updateOrderRequest.items);
  }

  @Override
  public int hashCode() {
    return Objects.hash(notes, items);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class UpdateOrderRequest {\n");
    sb.append("    notes: ").append(toIndentedString(notes)).append("\n");
    sb.append("    items: ").append(toIndentedString(items)).append("\n");
    sb.append("}");
    return sb.toString();
  }

  /**
   * Convert the given object to string with each line indented by 4 spaces
   * (except the first line).
   */
  private String toIndentedString(Object o) {
    if (o == null) {
      return "null";
    }
    return o.toString().replace("\n", "\n    ");
  }
}

