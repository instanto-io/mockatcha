package io.instanto.mockatcha;

import static io.instanto.mockatcha.ArgumentMatchers.anyInt;
import static io.instanto.mockatcha.ArgumentMatchers.anyString;
import static io.instanto.mockatcha.ArgumentMatchers.eq;
import static io.instanto.mockatcha.Mockatcha.clearInvocations;
import static io.instanto.mockatcha.Mockatcha.mock;
import static io.instanto.mockatcha.Mockatcha.mockingDetails;
import static io.instanto.mockatcha.Mockatcha.never;
import static io.instanto.mockatcha.Mockatcha.reset;
import static io.instanto.mockatcha.Mockatcha.times;
import static io.instanto.mockatcha.Mockatcha.verify;
import static io.instanto.mockatcha.Mockatcha.when;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.junit.SkipJVM;
import org.teavm.junit.TeaVMTestRunner;

@RunWith(TeaVMTestRunner.class)
@SkipJVM
public class MockatchaTeaVmTest {

  @Test
  public void stubsAndVerifiesAnInterfaceWithMockitoShapedCalls() {
    GreetingService greetings = mock(GreetingService.class);

    when(greetings.hello("Carl")).thenReturn("Hello Carl");

    assertEquals("Hello Carl", greetings.hello("Carl"));
    assertEquals("Hello Carl", greetings.hello("Carl"));
    assertEquals(null, greetings.hello("Someone else"));
    verify(greetings, times(2)).hello("Carl");
    verify(greetings, never()).hello("Nobody");
  }

  @Test
  public void supportsMatchersAnswersAndPrimitiveMethods() {
    GreetingService greetings = mock(GreetingService.class);

    when(greetings.hello(anyString()))
        .thenAnswer(invocation -> "Hello " + invocation.argument(0));
    when(greetings.add(anyInt(), eq(2))).thenReturn(42);

    assertEquals("Hello TeaVM", greetings.hello("TeaVM"));
    assertEquals(42, greetings.add(10, 2));
    assertEquals(0, greetings.add(10, 3));
    verify(greetings).add(10, 2);
  }

  @Test
  public void supportsConsecutiveAnswersAndThrows() {
    GreetingService greetings = mock(GreetingService.class);
    when(greetings.hello("sequence")).thenReturn("first").thenReturn("second");
    when(greetings.hello("failure")).thenThrow(new IllegalStateException("broken"));

    assertEquals("first", greetings.hello("sequence"));
    assertEquals("second", greetings.hello("sequence"));
    assertEquals("second", greetings.hello("sequence"));
    IllegalStateException failure =
        assertThrows(IllegalStateException.class, () -> greetings.hello("failure"));
    assertEquals("broken", failure.getMessage());
  }

  @Test
  public void recordsVoidAndOverloadedCalls() {
    GreetingService greetings = mock(GreetingService.class);

    greetings.record("ready");
    greetings.record(7);

    verify(greetings).record("ready");
    verify(greetings).record(7);
    assertTrue(mockingDetails(greetings).isMock());
    assertEquals(2, mockingDetails(greetings).getInvocations().size());
    assertEquals("record", mockingDetails(greetings).getInvocations().get(0).methodName());
    assertFalse(mockingDetails(new Object()).isMock());
  }

  @Test
  public void clearInvocationsKeepsConfiguredBehaviour() {
    GreetingService greetings = mock(GreetingService.class);
    when(greetings.hello("Carl")).thenReturn("Hello Carl");
    greetings.hello("Carl");

    clearInvocations(greetings);

    assertEquals("Hello Carl", greetings.hello("Carl"));
    verify(greetings).hello("Carl");
  }

  @Test
  public void resetRemovesConfiguredBehaviourAndCallHistory() {
    GreetingService greetings = mock(GreetingService.class);
    when(greetings.hello("Carl")).thenReturn("Hello Carl");
    greetings.hello("Carl");

    reset(greetings);

    assertEquals(null, greetings.hello("Carl"));
    verify(greetings).hello("Carl");
  }

  interface GreetingService {
    String hello(String name);

    int add(int left, int right);

    void record(String value);

    void record(int value);
  }
}
