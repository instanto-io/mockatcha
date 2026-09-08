/*
 * Copyright 2026 Carl Stainton
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.instanto.mockatcha;

/** Computes a result from a call's first two arguments. */
@FunctionalInterface
public interface Answer2<T, A, B> {
  T answer(A first, B second) throws Throwable;
}
