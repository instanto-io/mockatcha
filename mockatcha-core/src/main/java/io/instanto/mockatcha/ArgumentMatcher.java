/*
 * Copyright 2026 Carl Stainton
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.instanto.mockatcha;

/** Tests whether one invocation argument satisfies a stub or verification. */
@FunctionalInterface
public interface ArgumentMatcher<T> {
  boolean matches(T argument);
}
