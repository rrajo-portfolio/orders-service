package com.portfolio.orders.generated.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonValue;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.*;
import jakarta.annotation.Generated;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Gets or Sets OrderStatus
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2025-11-24T01:05:45.244357200+01:00[Europe/Madrid]", comments = "Generator version: 7.5.0")
public enum OrderStatus {
  
  PENDING("PENDING"),
  
  CONFIRMED("CONFIRMED"),
  
  SHIPPED("SHIPPED"),
  
  DELIVERED("DELIVERED"),
  
  CANCELLED("CANCELLED");

  private String value;

  OrderStatus(String value) {
    this.value = value;
  }

  @JsonValue
  public String getValue() {
    return value;
  }

  @Override
  public String toString() {
    return String.valueOf(value);
  }

  @JsonCreator
  public static OrderStatus fromValue(String value) {
    for (OrderStatus b : OrderStatus.values()) {
      if (b.value.equals(value)) {
        return b;
      }
    }
    throw new IllegalArgumentException("Unexpected value '" + value + "'");
  }
}

