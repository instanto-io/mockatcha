package io.instanto.oolong.dom;

import org.junit.After;

/**
 * A base class that removes the test's container afterwards.
 *
 * <p>The same cleanup {@link DomRule} performs, for a test whose runner does not run rules.
 */
public abstract class DomTest {

  @After
  public void removeTestContainer() {
    Dom.reset();
  }
}
