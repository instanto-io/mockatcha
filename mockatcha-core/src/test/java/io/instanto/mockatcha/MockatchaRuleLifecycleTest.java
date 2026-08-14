package io.instanto.mockatcha;

import static io.instanto.mockatcha.ArgumentMatchers.anyString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;

import org.junit.Test;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

public class MockatchaRuleLifecycleTest {

  @Test
  public void validatesAndClearsUsageWhenTheTestBodyFails() {
    AssertionError bodyFailure = new AssertionError("body failed");
    Statement body =
        new Statement() {
          @Override
          public void evaluate() {
            anyString();
            throw bodyFailure;
          }
        };

    Throwable observed =
        assertThrows(
            Throwable.class,
            () -> new MockatchaRule().apply(body, Description.EMPTY).evaluate());

    assertSame(bodyFailure, observed);
    assertEquals(1, observed.getSuppressed().length);
    Mockatcha.validateUsage();
  }
}
