/*
 * Copyright 2026 Carl Stainton
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.instanto.mockatcha.bdd;

/**
 * An action that may throw any checked or unchecked exception.
 *
 * <p>Used with {@link Bdd#expect(ThrowingRunnable)} to assert on exceptions.
 */
@FunctionalInterface
@SuppressWarnings("java:S112")
public interface ThrowingRunnable {

  /** Executes this action. */
  void run() throws Throwable;
}
