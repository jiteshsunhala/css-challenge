package com.css.challenge.model;

import java.util.List;

public record Response(Options options, List<Action> actions) {
  public record Options(long rate, long min, long max) {}
}
