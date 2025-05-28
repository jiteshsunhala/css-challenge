package com.css.challenge.util;

import java.time.Instant;

public class TimeUtil {

  public static long currentTimeMicros() {
    return getMicros(Instant.now());
  }

  public static long getMicros(Instant now) {
    return now.getEpochSecond() * 1_000_000 + now.getNano() / 1_000;
  }
}
