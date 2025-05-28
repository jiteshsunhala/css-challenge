package com.css.challenge.service;

import com.css.challenge.config.KitchenTestConfiguration;
import com.css.challenge.model.Action;
import com.css.challenge.model.InputOrder;
import com.css.challenge.model.Order;
import com.css.challenge.model.Temp;
import java.time.Duration;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = KitchenTestConfiguration.class)
public class KitchenServiceTest {

  @Autowired private KitchenService kitchenService;

  @Autowired private ActionLedger actionLedger;

  @AfterEach
  public void setup() {
    kitchenService.cleanupShelves();
  }

  @Nested
  public class PlaceOrders {

    @Test
    public void testPlaceOrdersOnDesiredShelf() {
      for (int i = 0; i < 6; i++) {
        kitchenService.placeOrder(new Order(new InputOrder("h-" + i, "hot item", Temp.hot, 40)));
        kitchenService.placeOrder(new Order(new InputOrder("c-" + i, "cool item", Temp.cold, 40)));
      }

      for (int i = 0; i < 12; i++) {
        kitchenService.placeOrder(new Order(new InputOrder("r-" + i, "room item", Temp.room, 40)));
      }

      Assertions.assertEquals(6, kitchenService.getShelf(Temp.hot).getItemsCount());
      Assertions.assertEquals(6, kitchenService.getShelf(Temp.cold).getItemsCount());
      Assertions.assertEquals(12, kitchenService.getShelf(Temp.room).getItemsCount());
      Assertions.assertTrue(
          actionLedger.getActions().stream()
              .allMatch(action -> Action.ActionName.place.equals(action.getAction())));
    }

    @Test
    public void testPlaceHotAndColdOrdersOnNormalShelf() {
      for (int i = 0; i < 12; i++) {
        kitchenService.placeOrder(new Order(new InputOrder("h-" + i, "hot item", Temp.hot, 300)));
        kitchenService.placeOrder(new Order(new InputOrder("c-" + i, "cool item", Temp.cold, 300)));
      }

      Shelf hotOne = kitchenService.getShelf(Temp.hot);
      Shelf coldOne = kitchenService.getShelf(Temp.cold);
      Shelf normalOne = kitchenService.getShelf(Temp.room);

      Assertions.assertEquals(6, hotOne.getItemsCount());
      Assertions.assertEquals(6, coldOne.getItemsCount());
      Assertions.assertEquals(12, normalOne.getItemsCount());

      for (int i = 0; i < 6; i++) {
        Assertions.assertTrue(hotOne.contains("h-" + i));
        Assertions.assertTrue(coldOne.contains("c-" + i));

        int orderIndex = i + 6;
        Assertions.assertTrue(normalOne.contains("h-" + orderIndex));
        Assertions.assertTrue(normalOne.contains("c-" + orderIndex));
      }

      Assertions.assertTrue(
          actionLedger.getActions().stream()
              .allMatch(action -> Action.ActionName.place.equals(action.getAction())));
    }

    @Test
    public void testDiscardOnNormalShelf() {
      for (int i = 0; i < 18; i++) {
        kitchenService.placeOrder(new Order(new InputOrder("h-" + i, "hot item", Temp.hot, 300)));
      }

      Shelf hotOne = kitchenService.getShelf(Temp.hot);
      Shelf normalOne = kitchenService.getShelf(Temp.room);

      for (int i = 0; i < 6; i++) {
        Assertions.assertTrue(hotOne.contains("h-" + i));
        int id1 = i + 6, id2 = i + 12;
        Assertions.assertTrue(normalOne.contains("h-" + id1));
        Assertions.assertTrue(normalOne.contains("h-" + id2));
      }

      kitchenService.placeOrder(new Order(new InputOrder("h-18", "hot item", Temp.hot, 300)));

      Assertions.assertTrue(normalOne.contains("h-18"));
      Assertions.assertEquals(
          1,
          actionLedger.getActions().stream()
              .filter(action -> Action.ActionName.discard.equals(action.getAction()))
              .count());
    }

    @Test
    public void testDiscardStaleOrders() throws InterruptedException {
      Duration delayDuration = Duration.ofSeconds(2);
      for (int i = 0; i < 18; i++) {
        kitchenService.placeOrder(
            new Order(
                new InputOrder(
                    "h-" + i, "hot item", Temp.hot, i + (int) delayDuration.getSeconds())));
      }

      Shelf hotOne = kitchenService.getShelf(Temp.hot);
      Shelf normalOne = kitchenService.getShelf(Temp.room);

      for (int i = 0; i < 6; i++) {
        Assertions.assertTrue(hotOne.contains("h-" + i));
        int id1 = i + 6, id2 = i + 12;
        Assertions.assertTrue(normalOne.contains("h-" + id1));
        Assertions.assertTrue(normalOne.contains("h-" + id2));
      }

      Thread.sleep(delayDuration.toMillis());
      kitchenService.placeOrder(new Order(new InputOrder("h-18", "hot item", Temp.hot, 300)));

      Assertions.assertTrue(hotOne.contains("h-18"));
      Assertions.assertEquals(
          1,
          actionLedger.getActions().stream()
              .filter(action -> Action.ActionName.discard.equals(action.getAction()))
              .count());
    }

    @Test
    public void testMoveItemFromNormal() {
      for (int i = 0; i < 12; i++) {
        kitchenService.placeOrder(new Order(new InputOrder("h-" + i, "hot item", Temp.hot, 300)));
        kitchenService.placeOrder(new Order(new InputOrder("c-" + i, "cold item", Temp.cold, 300)));
      }

      Shelf hotOne = kitchenService.getShelf(Temp.hot);
      Shelf coldOne = kitchenService.getShelf(Temp.cold);
      Shelf normalOne = kitchenService.getShelf(Temp.room);

      Assertions.assertEquals(6, hotOne.getItemsCount());
      Assertions.assertEquals(6, coldOne.getItemsCount());
      Assertions.assertEquals(12, normalOne.getItemsCount());
      Assertions.assertTrue(
          actionLedger.getActions().stream()
              .allMatch(action -> Action.ActionName.place.equals(action.getAction())));

      kitchenService.pickupOrder("h-0");

      Assertions.assertEquals(5, hotOne.getItemsCount());
      Assertions.assertEquals(6, coldOne.getItemsCount());
      Assertions.assertEquals(12, normalOne.getItemsCount());
      Assertions.assertEquals(
          1,
          actionLedger.getActions().stream()
              .filter(action -> Action.ActionName.pickup.equals(action.getAction()))
              .count());
      Assertions.assertEquals(
          24,
          actionLedger.getActions().stream()
              .filter(action -> Action.ActionName.place.equals(action.getAction()))
              .count());

      kitchenService.placeOrder(new Order(new InputOrder("c-12", "cold item", Temp.cold, 300)));

      Assertions.assertEquals(6, hotOne.getItemsCount());
      Assertions.assertEquals(6, coldOne.getItemsCount());
      Assertions.assertEquals(12, normalOne.getItemsCount());
      Assertions.assertTrue(normalOne.contains("c-12"));

      Assertions.assertEquals(
          1,
          actionLedger.getActions().stream()
              .filter(action -> Action.ActionName.pickup.equals(action.getAction()))
              .count());
      Assertions.assertEquals(
          1,
          actionLedger.getActions().stream()
              .filter(action -> Action.ActionName.move.equals(action.getAction()))
              .count());
      Assertions.assertEquals(
          25,
          actionLedger.getActions().stream()
              .filter(action -> Action.ActionName.place.equals(action.getAction()))
              .count());
    }
  }

  @Nested
  public class PickupOrders {

    @Test
    public void testItemOnShelfPickup() {
      for (int i = 0; i < 6; i++) {
        kitchenService.placeOrder(new Order(new InputOrder("h-" + i, "hot item", Temp.hot, 300)));
      }

      Shelf hotOne = kitchenService.getShelf(Temp.hot);

      Assertions.assertEquals(6, hotOne.getItemsCount());
      Assertions.assertTrue(
          actionLedger.getActions().stream()
              .allMatch(action -> Action.ActionName.place.equals(action.getAction())));

      kitchenService.pickupOrder("h-0");

      Assertions.assertEquals(5, hotOne.getItemsCount());
      Assertions.assertEquals(
          6,
          actionLedger.getActions().stream()
              .filter(action -> Action.ActionName.place.equals(action.getAction()))
              .count());
      Assertions.assertEquals(
          1,
          actionLedger.getActions().stream()
              .filter(action -> Action.ActionName.pickup.equals(action.getAction()))
              .count());
    }

    @Test
    public void testStaleItemPickup() throws InterruptedException {
      Duration delayDuration = Duration.ofSeconds(2);
      for (int i = 0; i < 6; i++) {
        kitchenService.placeOrder(
            new Order(
                new InputOrder(
                    "h-" + i, "hot item", Temp.hot, i + (int) delayDuration.getSeconds())));
      }

      Shelf hotOne = kitchenService.getShelf(Temp.hot);

      Assertions.assertEquals(6, hotOne.getItemsCount());
      Assertions.assertTrue(
          actionLedger.getActions().stream()
              .allMatch(action -> Action.ActionName.place.equals(action.getAction())));

      Thread.sleep(delayDuration.toMillis());
      kitchenService.pickupOrder("h-0");

      Assertions.assertEquals(5, hotOne.getItemsCount());
      Assertions.assertEquals(
          6,
          actionLedger.getActions().stream()
              .filter(action -> Action.ActionName.place.equals(action.getAction()))
              .count());
      Assertions.assertFalse(
          actionLedger.getActions().stream()
              .anyMatch(action -> Action.ActionName.pickup.equals(action.getAction())));
    }
  }
}
