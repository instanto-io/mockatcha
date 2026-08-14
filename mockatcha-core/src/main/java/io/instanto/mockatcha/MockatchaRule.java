package io.instanto.mockatcha;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

/**
 * JUnit rule for Mockatcha's strict, isolated per-test lifecycle.
 *
 * <pre>{@code
 * @Rule
 * public MockatchaRule mockatcha = new MockatchaRule();
 * }</pre>
 *
 * <p>The rule opens a {@link MockatchaSession}, enables strict stubbing, validates the session, and
 * releases its mocks. TeaVM tests need the reusable {@code teavm-rule-support} module.
 * {@link StrictTest} is the fallback when that support cannot be installed; do not use both.
 */
public final class MockatchaRule implements TestRule {

  private boolean strict = true;

  /** Disables fail-fast stub mismatch checks; unused stubs are still validated afterwards. */
  public MockatchaRule withoutStrictStubs() {
    strict = false;
    return this;
  }

  @Override
  public Statement apply(Statement base, Description description) {
    return new Statement() {
      @Override
      public void evaluate() throws Throwable {
        Throwable failure = null;
        MockatchaSession session = Mockatcha.session(strict);
        try {
          base.evaluate();
        } catch (Throwable thrown) {
          failure = thrown;
        }

        try {
          session.close();
        } catch (Throwable thrown) {
          failure = combine(failure, thrown);
        }
        if (failure != null) {
          throw failure;
        }
      }
    };
  }

  private static Throwable combine(Throwable primary, Throwable additional) {
    if (primary == null) {
      return additional;
    }
    primary.addSuppressed(additional);
    return primary;
  }
}
