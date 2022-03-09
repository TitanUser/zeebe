/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Zeebe Community License 1.1. You may not use this file
 * except in compliance with the Zeebe Community License 1.1.
 */
package io.camunda.zeebe.engine.state.instance;

import io.camunda.zeebe.db.ColumnFamily;
import io.camunda.zeebe.db.TransactionContext;
import io.camunda.zeebe.db.ZeebeDb;
import io.camunda.zeebe.db.impl.DbCompositeKey;
import io.camunda.zeebe.db.impl.DbForeignKey;
import io.camunda.zeebe.db.impl.DbLong;
import io.camunda.zeebe.db.impl.DbNil;
import io.camunda.zeebe.engine.state.ZbColumnFamilies;
import io.camunda.zeebe.engine.state.mutable.MutableTimerInstanceState;
import java.util.function.Consumer;

public final class DbTimerInstanceState implements MutableTimerInstanceState {

  private final ColumnFamily<DbCompositeKey<DbForeignKey<DbLong>, DbLong>, TimerInstance>
      timerInstanceColumnFamily;
  private final DbLong timerKey;
  private final DbForeignKey<DbLong> elementInstanceKey;
  private final DbCompositeKey<DbForeignKey<DbLong>, DbLong> elementAndTimerKey;

  private final ColumnFamily<
          DbCompositeKey<DbLong, DbCompositeKey<DbForeignKey<DbLong>, DbLong>>, DbNil>
      dueDateColumnFamily;
  private final DbLong dueDateKey;
  private final DbCompositeKey<DbLong, DbCompositeKey<DbForeignKey<DbLong>, DbLong>>
      dueDateCompositeKey;

  private long nextDueDate;

  public DbTimerInstanceState(
      final ZeebeDb<ZbColumnFamilies> zeebeDb, final TransactionContext transactionContext) {
    final TimerInstance timerInstance = new TimerInstance();
    timerKey = new DbLong();
    elementInstanceKey = DbElementInstanceState.foreignKey();
    elementAndTimerKey = new DbCompositeKey<>(elementInstanceKey, timerKey);
    timerInstanceColumnFamily =
        zeebeDb.createColumnFamily(
            ZbColumnFamilies.TIMERS, transactionContext, elementAndTimerKey, timerInstance);

    dueDateKey = new DbLong();
    dueDateCompositeKey = new DbCompositeKey<>(dueDateKey, elementAndTimerKey);
    dueDateColumnFamily =
        zeebeDb.createColumnFamily(
            ZbColumnFamilies.TIMER_DUE_DATES,
            transactionContext,
            dueDateCompositeKey,
            DbNil.INSTANCE);
  }

  @Override
  public void put(final TimerInstance timer) {
    timerKey.wrapLong(timer.getKey());
    elementInstanceKey.inner().wrapLong(timer.getElementInstanceKey());

    timerInstanceColumnFamily.put(elementAndTimerKey, timer);

    dueDateKey.wrapLong(timer.getDueDate());
    dueDateColumnFamily.put(dueDateCompositeKey, DbNil.INSTANCE);
  }

  @Override
  public void remove(final TimerInstance timer) {
    elementInstanceKey.inner().wrapLong(timer.getElementInstanceKey());
    timerKey.wrapLong(timer.getKey());
    timerInstanceColumnFamily.delete(elementAndTimerKey);

    dueDateKey.wrapLong(timer.getDueDate());
    dueDateColumnFamily.delete(dueDateCompositeKey);
  }

  @Override
  public long findTimersWithDueDateBefore(final long timestamp, final TimerVisitor consumer) {
    nextDueDate = -1L;

    dueDateColumnFamily.whileTrue(
        (key, nil) -> {
          final DbLong dueDate = key.first();

          boolean consumed = false;
          if (dueDate.getValue() <= timestamp) {
            final DbCompositeKey<DbForeignKey<DbLong>, DbLong> elementAndTimerKey = key.second();
            final TimerInstance timerInstance = timerInstanceColumnFamily.get(elementAndTimerKey);
            consumed = consumer.visit(timerInstance);
          }

          if (!consumed) {
            nextDueDate = dueDate.getValue();
          }
          return consumed;
        });

    return nextDueDate;
  }

  @Override
  public void forEachTimerForElementInstance(
      final long elementInstanceKey, final Consumer<TimerInstance> action) {
    this.elementInstanceKey.inner().wrapLong(elementInstanceKey);

    timerInstanceColumnFamily.whileEqualPrefix(
        this.elementInstanceKey,
        (key, value) -> {
          action.accept(value);
        });
  }

  @Override
  public TimerInstance get(final long elementInstanceKey, final long timerKey) {
    this.elementInstanceKey.inner().wrapLong(elementInstanceKey);
    this.timerKey.wrapLong(timerKey);

    return timerInstanceColumnFamily.get(elementAndTimerKey);
  }
}
