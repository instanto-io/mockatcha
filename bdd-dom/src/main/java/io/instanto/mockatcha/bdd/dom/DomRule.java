package io.instanto.mockatcha.bdd.dom;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

/**
 * Removes the test's container afterwards, so the next test starts with an empty document.
 *
 * <pre>{@code
 * @Rule
 * public DomRule dom = new DomRule();
 * }</pre>
 *
 * <p>Under TeaVM this needs a runner that runs rules. {@link DomTest} does the same for a test that
 * cannot rely on one.
 */
public final class DomRule implements TestRule {

  @Override
  public Statement apply(Statement base, Description description) {
    return new Statement() {
      @Override
      public void evaluate() throws Throwable {
        try {
          base.evaluate();
        } finally {
          Dom.reset();
        }
      }
    };
  }
}
