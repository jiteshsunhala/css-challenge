package com.css.challenge.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public class Action {
  private ActionName action;
  private String id;
  private long timestamp;

  public enum ActionName {
    place,
    move,
    pickup,
    discard
  }
}
