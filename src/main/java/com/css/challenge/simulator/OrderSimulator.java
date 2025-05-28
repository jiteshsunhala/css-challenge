package com.css.challenge.simulator;

import com.css.challenge.model.InputOrder;
import com.css.challenge.model.Order;
import com.css.challenge.model.Response;
import com.css.challenge.service.ActionLedger;
import com.css.challenge.service.ApiService;
import com.css.challenge.service.KitchenService;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Random;
import java.util.concurrent.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class OrderSimulator implements CommandLineRunner {

  private final DelayQueue<PickupOrder> pickupOrders = new DelayQueue<>();
  private volatile boolean producerDone = false;

  private final KitchenService kitchenService;
  private final ActionLedger actionLedger;
  private final ApiService apiService;

  public OrderSimulator(
      KitchenService kitchenService, ActionLedger actionLedger, ApiService apiService) {
    this.actionLedger = actionLedger;
    this.kitchenService = kitchenService;
    this.apiService = apiService;
  }

  @Override
  public void run(String... args) throws InterruptedException, ExecutionException {

    if (args.length != 3) {
      throw new IllegalArgumentException("3 parameters are required");
    }

    long placeRateMicros = Long.parseLong(args[0]);
    long pickupMinMicros = Long.parseLong(args[1]);
    long pickupMaxMicros = Long.parseLong(args[2]);

    if (pickupMaxMicros <= pickupMinMicros) {
      throw new IllegalArgumentException("Pickup min should be less than pickup max");
    }

    execute(placeRateMicros, pickupMinMicros, pickupMaxMicros);
  }

  private void execute(long placeRateMicros, long pickupMinMicros, long pickupMaxMicros)
      throws InterruptedException, ExecutionException {
    ExecutorService executorService = Executors.newFixedThreadPool(2);

    executorService.execute(new PlaceExecutor(placeRateMicros, pickupMinMicros, pickupMaxMicros));
    executorService.execute(new PickupExecutor());

    executorService.shutdown();
    executorService.awaitTermination(1, TimeUnit.HOURS);

    Response response =
        new Response(
            new Response.Options(placeRateMicros, pickupMinMicros, pickupMaxMicros),
            actionLedger.getActions());
    apiService.submitResponse(response);
  }

  public record PickupOrder(String orderId, Instant pickupTimestamp) implements Delayed {

    @Override
    public int compareTo(Delayed that) {
      return Long.compare(
          this.getDelay(TimeUnit.MILLISECONDS), that.getDelay(TimeUnit.MILLISECONDS));
    }

    @Override
    public long getDelay(TimeUnit unit) {
      return unit.convert(Duration.between(Instant.now(), pickupTimestamp));
    }
  }

  class PickupExecutor implements Runnable {

    @Override
    public void run() {
      while (true) {
        try {
          PickupOrder pickupOrder = pickupOrders.poll(500, TimeUnit.MILLISECONDS);
          if (pickupOrder != null) {
            kitchenService.pickupOrder(pickupOrder.orderId());
            log.info("Picked order: {} at: {}", pickupOrder.orderId(), Instant.now());
          } else if (producerDone && pickupOrders.isEmpty()) {
            log.info("No more elements to consume...");
            break;
          }
        } catch (InterruptedException e) {
          throw new RuntimeException(e);
        }
      }
    }
  }

  class PlaceExecutor implements Runnable {

    private final Random random = new Random();
    private final long placeRateMicros, pickupMinMicros, pickupMaxMicros;

    PlaceExecutor(long placeRateMicros, long pickupMinMicros, long pickupMaxMicros) {
      this.placeRateMicros = placeRateMicros;
      this.pickupMaxMicros = pickupMaxMicros;
      this.pickupMinMicros = pickupMinMicros;
    }

    @Override
    public void run() {
      try {
        List<InputOrder> inputOrderList = apiService.getInputOrders();
        long pickupDuration = pickupMaxMicros - pickupMinMicros;
        for (InputOrder inputOrder : inputOrderList) {
          Order order = new Order(inputOrder);
          kitchenService.placeOrder(order);
          long randomMillis = random.nextLong() % pickupDuration;
          if (randomMillis < 0) {
            randomMillis += pickupDuration;
          }

          Instant pickupTime =
              Instant.now().plus(pickupMinMicros + randomMillis, ChronoUnit.MICROS);
          log.info("Placed order {} with pickup at: {}", order.getId(), pickupTime);
          pickupOrders.add(new PickupOrder(order.getId(), pickupTime));
          Thread.sleep(placeRateMicros / 1000);
        }
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      } finally {
        producerDone = true;
      }
    }
  }
}
