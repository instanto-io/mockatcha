/*
 * Copyright 2026 Carl Stainton
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.instanto.mockatcha;

import static io.instanto.mockatcha.ArgumentMatchers.anyString;
import static io.instanto.mockatcha.Mockatcha.clearInvocations;
import static io.instanto.mockatcha.Mockatcha.doAnswer;
import static io.instanto.mockatcha.Mockatcha.doNothing;
import static io.instanto.mockatcha.Mockatcha.doReturn;
import static io.instanto.mockatcha.Mockatcha.doThrow;
import static io.instanto.mockatcha.Mockatcha.mockingDetails;
import static io.instanto.mockatcha.Mockatcha.never;
import static io.instanto.mockatcha.Mockatcha.reset;
import static io.instanto.mockatcha.Mockatcha.spy;
import static io.instanto.mockatcha.Mockatcha.times;
import static io.instanto.mockatcha.Mockatcha.verify;
import static io.instanto.mockatcha.Mockatcha.when;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.junit.TeaVMTestRunner;

/** Spies, which record calls and hand the unstubbed ones to a real object. */
@RunWith(TeaVMTestRunner.class)
public class MockatchaSpyTeaVmTest extends MockatchaTestLifecycle {

  @Test
  public void unstubbedMethodsReachTheRealObject() {
    PricingService real = new PricingService("GBP");
    PricingService pricing = spy(PricingService.class, real);

    assertEquals("GBP", pricing.currency());
    assertEquals(200, pricing.total("A-17"));
    verify(pricing).currency();
    verify(pricing).total("A-17");
  }

  @Test
  public void stubbedMethodsTakePrecedenceOverTheRealObject() {
    PricingService real = new PricingService("GBP");
    PricingService pricing = spy(PricingService.class, real);

    when(pricing.currency()).thenReturn("EUR");

    assertEquals("EUR", pricing.currency());
    assertEquals("EUR", pricing.currency());
    assertEquals("GBP", real.currency());
  }

  @Test
  public void theSetupCallOfWhenRunsTheRealMethodOnceAndIsNotRecorded() {
    PricingService real = new PricingService("GBP");
    PricingService pricing = spy(PricingService.class, real);

    when(pricing.currency()).thenReturn("EUR");

    assertEquals(1, real.currencyCalls());
    assertEquals(0, mockingDetails(pricing).getInvocations().size());
  }

  @Test
  public void doReturnArrangesAnAnswerWithoutRunningTheRealMethod() {
    PricingService real = new PricingService("GBP");
    PricingService pricing = spy(PricingService.class, real);

    doReturn("EUR").when(pricing).currency();

    assertEquals(0, real.currencyCalls());
    assertEquals("EUR", pricing.currency());
    assertEquals(0, real.currencyCalls());
    verify(pricing).currency();
  }

  @Test
  public void doThrowArrangesAFailureWithoutRunningTheRealMethod() {
    PricingService real = new PricingService("GBP");
    PricingService pricing = spy(PricingService.class, real);

    doThrow(new IllegalStateException("offline")).when(pricing).currency();

    IllegalStateException failure = assertThrows(IllegalStateException.class, pricing::currency);
    assertEquals("offline", failure.getMessage());
    assertEquals(0, real.currencyCalls());
  }

  @Test
  public void doNothingSuppressesARealVoidMethod() {
    PricingService real = new PricingService("GBP");
    PricingService pricing = spy(PricingService.class, real);

    doNothing().when(pricing).record("audited");
    pricing.record("audited");
    pricing.record("other");

    assertEquals(List.of("other"), real.notes());
    verify(pricing).record("audited");
  }

  @Test
  public void doAnswerCalculatesAResultWithoutRunningTheRealMethod() {
    PricingService real = new PricingService("GBP");
    PricingService pricing = spy(PricingService.class, real);

    doAnswer(call -> call.argument(0) + "!").when(pricing).describe(anyString());

    assertEquals("A-17!", pricing.describe("A-17"));
    assertEquals(0, real.describeCalls());
  }

  @Test
  public void aFailingRealMethodFailsThroughTheSpy() {
    PricingService real = new PricingService(null);
    PricingService pricing = spy(PricingService.class, real);

    assertThrows(IllegalStateException.class, pricing::currency);
    verify(pricing).currency();
  }

  @Test
  public void verificationReadsRecordedCallsWithoutTouchingTheRealObject() {
    PricingService real = new PricingService("GBP");
    PricingService pricing = spy(PricingService.class, real);

    pricing.currency();

    verify(pricing).currency();
    verify(pricing, never()).describe("A-17");
    assertEquals(1, real.currencyCalls());
  }

  @Test
  public void resetRemovesStubsAndHistoryButKeepsDelegation() {
    PricingService real = new PricingService("GBP");
    PricingService pricing = spy(PricingService.class, real);
    doReturn("EUR").when(pricing).currency();
    pricing.currency();

    reset(pricing);

    assertEquals("GBP", pricing.currency());
    verify(pricing, times(1)).currency();
  }

  @Test
  public void clearInvocationsKeepsStubsAndDelegation() {
    PricingService real = new PricingService("GBP");
    PricingService pricing = spy(PricingService.class, real);
    doReturn("EUR").when(pricing).currency();
    pricing.currency();

    clearInvocations(pricing);

    assertEquals("EUR", pricing.currency());
    verify(pricing, times(1)).currency();
  }

  @Test
  public void aSpyIsNotTheObjectItDelegatesTo() {
    PricingService real = new PricingService("GBP");
    PricingService pricing = spy(PricingService.class, real);

    assertNotSame(real, pricing);
    assertTrue(pricing instanceof PricingService);
    assertEquals("Mockatcha spy of io.instanto.mockatcha.MockatchaSpyTeaVmTest$PricingService",
        pricing.toString());
  }

  @Test
  public void aRealMethodCallingAnotherOneIsNotIntercepted() {
    PricingService real = new PricingService("GBP");
    PricingService pricing = spy(PricingService.class, real);
    doReturn("EUR").when(pricing).currency();

    // summary() calls currency() on the delegate, where `this` is the real object, so the stub
    // above does not apply. This is the documented cost of delegation.
    assertEquals("summary GBP", pricing.summary());
    verify(pricing, never()).currency();
  }

  @Test
  public void requiresARealObjectToDelegateTo() {
    assertThrows(
        IllegalArgumentException.class, () -> spy(PricingService.class, (PricingService) null));
  }

  @Test
  public void spiesOnAnInterfaceThroughItsImplementation() {
    Notifier notifier = spy(Notifier.class, new RecordingNotifier());

    notifier.send("first");
    doNothing().when(notifier).send("second");
    notifier.send("second");

    verify(notifier).send("first");
    verify(notifier).send("second");
    assertEquals(List.of("first"), ((RecordingNotifier) notifier.self()).sent);
  }

  @Test
  public void spiesOnPrimitiveAndVoidMethods() {
    Counter real = new Counter();
    Counter counter = spy(Counter.class, real);

    counter.increment();
    counter.increment();

    assertEquals(2, counter.count());
    assertEquals(2L, counter.total());
    assertTrue(counter.positive());
    verify(counter, times(2)).increment();

    doReturn(99).when(counter).count();
    assertEquals(99, counter.count());
    assertEquals(2, real.count());
  }

  public static class PricingService {

    private final String currency;
    private final List<String> notes = new ArrayList<>();
    private int currencyCalls;
    private int describeCalls;

    public PricingService() {
      this("GBP");
    }

    public PricingService(String currency) {
      this.currency = currency;
    }

    public String currency() {
      currencyCalls++;
      if (currency == null) {
        throw new IllegalStateException("currency unavailable");
      }
      return currency;
    }

    public int total(String account) {
      return 200;
    }

    public String describe(String account) {
      describeCalls++;
      return account;
    }

    public String summary() {
      return "summary " + currency();
    }

    public void record(String note) {
      notes.add(note);
    }

    public List<String> notes() {
      return notes;
    }

    public int currencyCalls() {
      return currencyCalls;
    }

    public int describeCalls() {
      return describeCalls;
    }
  }

  public static class Counter {

    private int count;

    public void increment() {
      count++;
    }

    public int count() {
      return count;
    }

    public long total() {
      return count;
    }

    public boolean positive() {
      return count > 0;
    }
  }

  public interface Notifier {

    void send(String message);

    Object self();
  }

  public static class RecordingNotifier implements Notifier {

    final List<String> sent = new ArrayList<>();

    @Override
    public void send(String message) {
      sent.add(message);
    }

    @Override
    public Object self() {
      return this;
    }
  }
}
