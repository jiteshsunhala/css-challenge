package com.css.challenge.service;

import com.css.challenge.model.Action;
import com.css.challenge.model.Order;
import com.css.challenge.util.TimeUtil;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class ActionLedger {

  private final ArrayList<Action> ledger = new ArrayList<>();

  public void updateLedger(Action.ActionName actionName, Order order) {
    ledger.add(new Action(actionName, order.getId(), TimeUtil.currentTimeMicros()));
  }

  public List<Action> getActions() {
    return ledger;
  }

  public void cleanup() {
    this.ledger.clear();
  }
}
