package io.instanto.mockatcha;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

/**
 * Turns on strict stubbing for a test, then checks its stubs and matchers afterwards.
 *
 * <pre>{@code
 * @Rule
 * public MockatchaRule mockatcha = new MockatchaRule();
 * }</pre>
 *
 * <p>The same checks {@link StrictTest} performs, for a test that would rather not inherit from
 * anything. Under TeaVM this needs a runner that runs rules; {@link StrictTest} works either way.
 */
public final class MockatchaRule implements TestRule {

  private boolean strict = true;

  /** Leaves a call that misses its stub to fail later, rather than at the call. */
  public MockatchaRule withoutStrictStubs() {
    strict = false;
    return this;
  }

  @Override
  public Statement apply(Statement base, Description description) {
    return new Statement() {
      @Override
      public void evaluate() throws Throwable {
        Mockatcha.strictStubs(strict);
        try {
          base.evaluate();
        } finally {
          Mockatcha.strictStubs(false);
        }
        Mockatcha.validateStubbing();
        Mockatcha.validateUsage();
      }
    };
  }
}
