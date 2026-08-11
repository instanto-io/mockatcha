package io.instanto.mockatcha;

import org.junit.After;
import org.junit.Before;

/**
 * A base class for tests that should fail on unused stubs and misplaced matchers.
 *
 * <pre>{@code
 * @RunWith(TeaVMTestRunner.class)
 * @SkipJVM
 * public class PricingTest extends StrictTest {
 *     ...
 * }
 * }</pre>
 *
 * <p>Extending this is the way to get the checks automatically: TeaVM's test runner collects
 * {@code @Before} and {@code @After} methods from superclasses, and does not run JUnit rules.
 * Calling {@link Mockatcha#validateStubbing} and {@link Mockatcha#validateUsage} directly does the
 * same for one test.
 */
public abstract class StrictTest {

  @Before
  public void failOnCallsThatMissTheirStub() {
    Mockatcha.strictStubs(true);
  }

  @After
  public void checkMockatchaUsage() {
    Mockatcha.strictStubs(false);
    AssertionError unusedStubs = null;
    try {
      Mockatcha.validateStubbing();
    } catch (AssertionError failure) {
      unusedStubs = failure;
    }

    try {
      Mockatcha.validateUsage();
    } catch (IllegalStateException misplacedMatchers) {
      if (unusedStubs == null) {
        throw misplacedMatchers;
      }
    }

    if (unusedStubs != null) {
      throw unusedStubs;
    }
  }
}
