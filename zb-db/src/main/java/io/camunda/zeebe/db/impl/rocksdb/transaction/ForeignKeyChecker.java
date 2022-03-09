/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Zeebe Community License 1.1. You may not use this file
 * except in compliance with the Zeebe Community License 1.1.
 */
package io.camunda.zeebe.db.impl.rocksdb.transaction;

import io.camunda.zeebe.db.DbKey;
import io.camunda.zeebe.db.impl.DbForeignKey;
import io.camunda.zeebe.db.impl.ZeebeDbConstants;
import org.agrona.ExpandableArrayBuffer;

/**
 * Similar to {@link TransactionalColumnFamily} but supports lookups on arbitrary column families.
 * Can be used to check that a foreign key is valid.
 */
public final class ForeignKeyChecker {
  private final ZeebeTransactionDb<?> transactionDb;
  private final ExpandableArrayBuffer keyBuffer = new ExpandableArrayBuffer();

  public ForeignKeyChecker(final ZeebeTransactionDb<?> transactionDb) {
    this.transactionDb = transactionDb;
  }

  public void assertExists(
      final ZeebeTransaction transaction, final DbForeignKey<? extends DbKey> foreignKey)
      throws Exception {
    keyBuffer.putLong(0, foreignKey.columnFamily().ordinal(), ZeebeDbConstants.ZB_DB_BYTE_ORDER);
    foreignKey.write(keyBuffer, Long.BYTES);
    final var value =
        transaction.get(
            transactionDb.getDefaultNativeHandle(),
            transactionDb.getReadOptionsNativeHandle(),
            keyBuffer.byteArray(),
            keyBuffer.capacity());
    if (value == null) {
      throw new IllegalStateException(
          "Foreign key " + foreignKey.inner() + " does not exist in " + foreignKey.columnFamily());
    }
  }
}
