package com.css.challenge.service;

import com.css.challenge.model.Action;
import com.css.challenge.model.Order;
import com.css.challenge.model.Temp;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class KitchenService {

  private final Shelf heater = new Shelf("heater", Temp.hot, 6);
  private final Shelf cooler = new Shelf("cooler", Temp.cold, 6);
  private final Shelf normal = new Shelf("normal", Temp.room, 12);

  private final ReentrantLock lock = new ReentrantLock(true);

  private final ActionLedger actionLedger;

  public KitchenService(ActionLedger actionLedger) {
    this.actionLedger = actionLedger;
  }

  public void placeOrder(Order order) {
    lock.lock();
    try {
      Shelf ideal = getShelf(order.getTemp());

      // 1. add order to respective shelf.
      discardStaleOrders(List.of(ideal));
      if (ideal.hasCapacity()) {
        ideal.addOrder(order);
        log.info("Placed order: {} on shelf: {}", order.getId(), ideal.getName());
        actionLedger.updateLedger(Action.ActionName.place, order);

        return;
      }

      log.info("Shelf: {} is full..", ideal.getName());

      // 2. add order to normal room temperature shelf.
      discardStaleOrders(List.of(normal));
      if (normal.hasCapacity()) {
        normal.addOrder(order);
        log.info("Placed order: {} on shelf: {}", order.getId(), normal.getName());
        actionLedger.updateLedger(Action.ActionName.place, order);
        return;
      }

      log.info("Shelf: {} is full..", normal.getName());

      if (moveAndPlace(order)) {
        return;
      }

      log.info("No move happened.");

      Order discardedOrder = normal.replaceOrder(order);
      log.info("Discarded order: {}", discardedOrder.getId());
      log.info("Placed order: {}", order.getId());
      actionLedger.updateLedger(Action.ActionName.discard, discardedOrder);
      actionLedger.updateLedger(Action.ActionName.place, order);
    } finally {
      lock.unlock();
    }
  }

  private boolean moveAndPlace(Order order) {

    List<Order> normalOrders =
        normal.getOrders().stream().filter(o -> !Temp.room.equals(o.getTemp())).toList();

    for (Order o : normalOrders) {
      Shelf target = getShelf(o.getTemp());
      if (!target.hasCapacity()) {
        continue;
      }

      Optional<Order> removedOrder = normal.removeOrder(o.getId());
      target.addOrder(removedOrder.get());

      normal.addOrder(order);

      log.info(
          "Moved order: {} from normal shelf to shelf: {}",
          removedOrder.get().getId(),
          target.getName());
      log.info("Placed order: {} to normal shelf", order.getId());
      actionLedger.updateLedger(Action.ActionName.move, removedOrder.get());
      actionLedger.updateLedger(Action.ActionName.place, order);
      return true;
    }
    return false;
  }

  public void pickupOrder(String orderId) {
    lock.lock();
    try {
      discardStaleOrders(List.of(heater, cooler, normal));
      Stream.of(heater, cooler, normal)
          .forEach(
              rack -> {
                Optional<Order> pickedUpOrder = rack.removeOrder(orderId);
                pickedUpOrder.ifPresent(
                    order -> actionLedger.updateLedger(Action.ActionName.pickup, order));
              });
    } finally {
      lock.unlock();
    }
  }

  public Shelf getShelf(Temp temp) {
    return switch (temp) {
      case hot -> heater;
      case cold -> cooler;
      case room -> normal;
    };
  }

  private void discardStaleOrders(List<Shelf> shelves) {
    shelves.stream()
        .map(Shelf::discardStaleOrders)
        .flatMap(List::stream)
        .forEach(
            o -> {
              log.info("Discarded order: {}", o.getId());
              actionLedger.updateLedger(Action.ActionName.discard, o);
            });
  }

  public void cleanupShelves() {
    Stream.of(heater, cooler, normal).forEach(Shelf::cleanup);
    actionLedger.cleanup();
  }
}
