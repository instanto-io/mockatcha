package io.instanto.mockatcha.dom;

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
 * <p>TeaVM tests need the reusable {@code teavm-rule-support} module. {@link DomTest} is the
 * fallback when rule support cannot be installed.
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
