/*
 * Copyright 2026 Carl Stainton
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.instanto.mockatcha;

import org.junit.After;

/**
 * Releases what a test created outside a session, so it is not reported against the next test.
 *
 * <p>TeaVM starts each test class in a fresh page, so state could not carry between them there. A
 * JVM runs the whole suite in one process, where a mock left unchecked by one class is drained by
 * whichever class next asks for a check. Tests that own a session or use the rule do not need this;
 * the rest extend it.
 *
 * <p>Releasing twice is harmless, which matters because TeaVM's runner applies an inherited
 * {@code @After} once per class in the hierarchy.
 */
public abstract class MockatchaTestLifecycle {

  @After
  public void releaseMockatchaState() {
    Mockatcha.releaseMocks();
  }
}
