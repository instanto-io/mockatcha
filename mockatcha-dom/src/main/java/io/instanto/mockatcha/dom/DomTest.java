/*
 * Copyright 2026 Carl Stainton
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.instanto.mockatcha.dom;

import org.junit.After;

/**
 * Compatibility base class for a runner that cannot execute {@link DomRule}.
 */
public abstract class DomTest {

  @After
  public void removeTestContainer() {
    Dom.reset();
  }
}
