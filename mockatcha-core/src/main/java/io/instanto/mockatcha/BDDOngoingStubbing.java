/*
 * Copyright 2026 Carl Stainton
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.instanto.mockatcha;

/** Configures the result of the call captured by {@link BDDMockatcha#given(Object)}. */
public interface BDDOngoingStubbing<T> {

  BDDOngoingStubbing<T> willReturn(T value);

  BDDOngoingStubbing<T> willThrow(Throwable throwable);

  BDDOngoingStubbing<T> willAnswer(Answer<? extends T> answer);
}
