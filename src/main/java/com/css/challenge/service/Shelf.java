package com.css.challenge.service;

import com.css.challenge.model.Order;
import com.css.challenge.model.Temp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.PriorityQueue;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Shelf {
  @Getter private final String name;
  @Getter private final Temp temp;
  @Getter private final int capacity;
  protected PriorityQueue<Order> orders;

  public Shelf(String name, Temp temp, int capacity) {
    this.name = name;
    this.temp = temp;
    this.capacity = capacity;
    this.orders = new PriorityQueue<>();
  }

  public void addOrder(Order order) {
    if (orders.size() == capacity || order.isStale()) {
      return;
    }

    order.place(Instant.now(), this.temp);
    orders.add(order);
  }

  public Optional<Order> removeOrder(String orderId) {
    Optional<Order> pickedUpOrder =
        getOrders().stream().filter(o -> orderId.equals(o.getId())).findFirst();
    pickedUpOrder.ifPresent(order -> orders.remove(order));

    return pickedUpOrder;
  }

  public boolean hasCapacity() {
    return orders.size() != capacity;
  }

  public Order replaceOrder(Order order) {
    Order discardedOrder = orders.poll();
    addOrder(order);
    return discardedOrder;
  }

  public List<Order> getOrders() {
    return orders.stream().toList();
  }

  public List<Order> discardStaleOrders() {
    List<Order> removedOrders = new ArrayList<>();
    while (!orders.isEmpty()) {
      Order oldest = orders.peek();
      if (oldest.isStale()) {
        removedOrders.add(orders.poll());
      } else {
        break;
      }
    }

    return removedOrders;
  }

  public int getItemsCount() {
    return orders.size();
  }

  public boolean contains(String orderId) {
    return getOrders().stream().anyMatch(o -> o.getId().equals(orderId));
  }

  public void cleanup() {
    orders.clear();
  }
}
