/*
 * Copyright 2026 Carl Stainton
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.instanto.mockatcha.bdd;

import io.instanto.mockatcha.Invocation;
import io.instanto.mockatcha.spi.MockAccess;
import java.util.ArrayList;
import java.util.List;

/** The calls recorded for a mock, or for one method of it. */
public final class CallLog {

  private final Object mock;
  private final String methodName;
  private final List<Invocation> invocations;
  private final String description;

  CallLog(Object mock, String methodName, List<Invocation> invocations, String description) {
    this.mock = mock;
    this.methodName = methodName;
    this.invocations = invocations;
    this.description = description;
  }

  /** How many matching calls were recorded. */
  public int count() {
    return invocations.size();
  }

  /** Whether any matching call was recorded. */
  public boolean any() {
    return !invocations.isEmpty();
  }

  /** The arguments of one matching call, counting from zero. */
  public List<Object> argsFor(int index) {
    return at(index).arguments();
  }

  /** The arguments of every matching call, oldest first. */
  public List<List<Object>> allArgs() {
    List<List<Object>> arguments = new ArrayList<>(invocations.size());
    for (Invocation invocation : invocations) {
      arguments.add(invocation.arguments());
    }
    return arguments;
  }

  /** The first matching call. */
  public Invocation first() {
    return at(0);
  }

  /** The most recent matching call. */
  public Invocation mostRecent() {
    return at(invocations.size() - 1);
  }

  /** Every matching call, oldest first. */
  public List<Invocation> all() {
    return invocations;
  }

  /** One matching call, counting from zero. */
  public Invocation at(int index) {
    if (index < 0 || index >= invocations.size()) {
      throw new IndexOutOfBoundsException(
          "Wanted call " + index + " of " + description + " but only " + invocations.size()
              + " were recorded");
    }
    return invocations.get(index);
  }

  /**
   * Forgets the calls this log covers, keeping any arranged behaviour.
   *
   * <p>Reads taken before this still hold the calls they found.
   */
  public void reset() {
    if (methodName == null) {
      MockAccess.clearInvocations(mock);
    } else {
      MockAccess.clearInvocations(mock, methodName);
    }
  }

  @Override
  public String toString() {
    return description + " " + invocations;
  }
}
