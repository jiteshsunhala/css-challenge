package com.css.challenge.model;

import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public class InputOrder {

  public InputOrder() {}

  public InputOrder(String id, String name, Temp temp, int freshness) {
    this.id = id;
    this.name = name;
    this.temp = temp;
    this.freshness = freshness;
  }

  private String id;
  private String name;
  private Temp temp;
  private int freshness;
}
