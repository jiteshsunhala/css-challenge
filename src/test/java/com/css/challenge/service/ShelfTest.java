package com.css.challenge.service;

import com.css.challenge.model.InputOrder;
import com.css.challenge.model.Order;
import com.css.challenge.model.Temp;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ShelfTest {

  private final Shelf shelf = new Shelf("my-shelf", Temp.cold, 6);

  @Test
  public void addOrdersTest() {
    List<Order> orders =
        List.of(
            new Order(new InputOrder("1", "item-1", Temp.cold, 34)),
            new Order(new InputOrder("2", "item-2", Temp.cold, 46)),
            new Order(new InputOrder("3", "item-3", Temp.cold, 12)),
            new Order(new InputOrder("4", "item-4", Temp.cold, 6)));

    orders.forEach(shelf::addOrder);
    List<String> idsInOrder = List.of("4", "3", "1", "2");

    idsInOrder.forEach(
        id -> {
          Order removedOrder = shelf.getOrders().getFirst();
          Assertions.assertEquals(removedOrder.getId(), id);
          shelf.removeOrder(id);
        });
  }

  @Test
  public void discardStaleOrdersTest() throws InterruptedException {
    List<Order> orders =
        List.of(
            new Order(new InputOrder("1", "item-1", Temp.cold, 34)),
            new Order(new InputOrder("2", "item-2", Temp.cold, 46)),
            new Order(new InputOrder("3", "item-3", Temp.cold, 12)),
            new Order(new InputOrder("4", "item-4", Temp.cold, 2)));

    orders.forEach(shelf::addOrder);

    // wait for the first order to become stale.
    Thread.sleep(2500);

    List<Order> discardedOrders = shelf.discardStaleOrders();
    Assertions.assertEquals(1, discardedOrders.size());
    Order discardedOrder = discardedOrders.getFirst();

    Assertions.assertEquals("4", discardedOrder.getId());

    List<String> idsInOrder = List.of("3", "1", "2");

    idsInOrder.forEach(
        id -> {
          Order removedOrder = shelf.getOrders().getFirst();
          Assertions.assertEquals(removedOrder.getId(), id);
          shelf.removeOrder(id);
        });
  }
}
