/*
 * Copyright 2026 Carl Stainton
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.instanto.mockatcha;

import io.instanto.mockatcha.internal.MockRuntime;

/**
 * Owns, validates, and releases the mocks created during one test.
 *
 * <p>Use this with try-with-resources in a custom or non-JUnit harness. JUnit tests should use
 * {@link MockatchaRule}, or {@link StrictTest} when their runner cannot execute rules. Both
 * adapters open a session already. Sessions cannot be nested.
 */
public final class MockatchaSession implements AutoCloseable {

  private final Object token;
  private boolean closed;

  MockatchaSession(boolean strictStubs) {
    token = MockRuntime.openSession(strictStubs);
  }

  /** Validates this session and releases all mocks it owns. Calling this twice is harmless. */
  @Override
  public void close() {
    if (closed) {
      return;
    }
    closed = true;
    MockRuntime.closeSession(token);
  }
}
