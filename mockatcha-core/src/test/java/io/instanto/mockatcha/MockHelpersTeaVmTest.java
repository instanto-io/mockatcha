/*
 * Copyright 2026 Carl Stainton
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.instanto.mockatcha;

import static io.instanto.mockatcha.Mockatcha.verify;
import static io.instanto.mockatcha.Mockatcha.when;
import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.junit.TeaVMTestRunner;

/**
 * Mocks made through a test's own helpers. TeaVM follows the class a helper hands on back to the
 * literal its caller passes, so the helper's mocks are generated like any other.
 */
@RunWith(TeaVMTestRunner.class)
public class MockHelpersTeaVmTest extends MockatchaTestLifecycle {

  @Test
  public void mocksThroughAStaticHelper() {
    Clock clock = mockOf(Clock.class);
    when(clock.now()).thenReturn(42L);

    assertEquals(42L, clock.now());
    verify(clock).now();
  }

  @Test
  public void mocksThroughAHelperInAnotherClass() {
    Clock clock = Helpers.namedMock(Clock.class, "clock");
    when(clock.now()).thenReturn(7L);

    assertEquals(7L, clock.now());
  }

  @Test
  public void spiesThroughAHelperThatCallsAnother() {
    Greeter greeter = Helpers.spyOn(Greeter.class, name -> "Hello " + name);
    when(greeter.greet("TeaVM")).thenReturn("Hi TeaVM");

    assertEquals("Hi TeaVM", greeter.greet("TeaVM"));
    assertEquals("Hello Carl", greeter.greet("Carl"));
  }

  private static <T> T mockOf(Class<T> type) {
    return Mockatcha.mock(type);
  }

  interface Clock {
    long now();
  }

  interface Greeter {
    String greet(String name);
  }

  static final class Helpers {
    private Helpers() {}

    static <T> T namedMock(Class<T> type, String name) {
      return Mockatcha.mock(type, name);
    }

    static <T> T spyOn(Class<T> type, T delegate) {
      return spy(type, delegate);
    }

    private static <T> T spy(Class<T> type, T delegate) {
      return Mockatcha.spy(type, delegate);
    }
  }
}
