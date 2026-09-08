/*
 * Copyright 2026 Carl Stainton
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.instanto.mockatcha;

import io.instanto.mockatcha.internal.MockRuntime;
import java.util.List;

/** Read-only call history for diagnostics and specialised assertions. */
public final class MockingDetails {

  private final Object mock;

  MockingDetails(Object mock) {
    this.mock = mock;
  }

  /** Whether the inspected object is a Mockatcha mock or spy. */
  public boolean isMock() {
    return MockRuntime.isMock(mock);
  }

  /** A read-only snapshot of its calls, oldest first. */
  public List<Invocation> getInvocations() {
    return MockRuntime.invocations(mock);
  }
}
