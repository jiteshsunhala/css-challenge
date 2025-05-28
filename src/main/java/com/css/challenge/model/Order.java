package com.css.challenge.model;

import java.time.Duration;
import java.time.Instant;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class Order implements Comparable<Order> {
  private String id;
  private String name;
  private Temp temp;
  private long startFreshness;
  private int decayCoefficient;
  private Instant placedTimestamp;
  private Instant freshnessLastTimestamp;

  public Order(InputOrder inputOrder) {
    this.id = inputOrder.getId();
    this.name = inputOrder.getName();
    this.temp = inputOrder.getTemp();
    this.startFreshness = inputOrder.getFreshness();
    placedTimestamp = null;
  }

  public void place(Instant newPlacedTimestamp, Temp placedShelfTemperature) {
    if (placedTimestamp != null) {
      evaluateNewFreshness(newPlacedTimestamp);
    }

    placedTimestamp = newPlacedTimestamp;
    evaluateFreshnessLastTimestamp(placedShelfTemperature);
  }

  private void evaluateNewFreshness(Instant newPlacedTimestamp) {
    Duration duration = Duration.between(newPlacedTimestamp, placedTimestamp);
    startFreshness -= (duration.getSeconds() * decayCoefficient);
  }

  private void evaluateFreshnessLastTimestamp(Temp placedShelfTemperature) {
    decayCoefficient = temp.equals(placedShelfTemperature) ? 1 : 2;
    long timeRemainingToDecay = Math.ceilDiv(startFreshness, decayCoefficient);
    freshnessLastTimestamp = placedTimestamp.plusSeconds(timeRemainingToDecay);
  }

  @Override
  public int compareTo(Order that) {
    return (int)
        (this.freshnessLastTimestamp.getEpochSecond()
            - that.getFreshnessLastTimestamp().getEpochSecond());
  }

  public boolean isStale() {
    if (placedTimestamp == null) {
      return false;
    }
    return Instant.now().isAfter(freshnessLastTimestamp);
  }
}
