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
