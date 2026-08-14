package io.instanto.mockatcha;

import org.junit.After;
import org.junit.Before;

/**
 * Compatibility base class for TeaVM runners that do not execute {@link MockatchaRule}.
 *
 * <pre>{@code
 * @RunWith(TeaVMTestRunner.class)
 * @SkipJVM
 * public class PricingTest extends StrictTest {
 *     ...
 * }
 * }</pre>
 *
 * <p>This is a Mockatcha fallback, not a TeaVM or JUnit API. Use it only when the runner cannot use
 * Mockatcha's reusable TeaVM rule support. It opens the same {@link MockatchaSession} through
 * inherited {@code @Before} and {@code @After} methods. Do not combine it with
 * {@link MockatchaRule}.
 */
public abstract class StrictTest {

  private MockatchaSession session;

  @Before
  public void failOnCallsThatMissTheirStub() {
    session = Mockatcha.session(true);
  }

  @After
  public void checkMockatchaUsage() {
    if (session != null) {
      session.close();
    }
  }
}
