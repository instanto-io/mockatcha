package io.instanto.mockatcha.bdd;

import static io.instanto.mockatcha.Mockatcha.mock;
import static io.instanto.mockatcha.Mockatcha.spy;
import static io.instanto.mockatcha.Mockatcha.times;
import static io.instanto.mockatcha.Mockatcha.verify;
import static io.instanto.mockatcha.bdd.Bdd.calls;
import static io.instanto.mockatcha.bdd.Bdd.spyOn;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.junit.SkipJVM;
import org.teavm.junit.TeaVMTestRunner;

/** Naming a method instead of calling it, and reading a call record fluently. */
@RunWith(TeaVMTestRunner.class)
@SkipJVM
public class BddSpyTeaVmTest {

  @Test
  public void arrangesAMethodByNameWithoutCallingIt() {
    PricingService real = new PricingService();
    PricingService pricing = spy(PricingService.class, real);

    spyOn(pricing, "currency").andReturn("EUR");

    assertEquals("EUR", pricing.currency());
    assertEquals(0, real.currencyCalls());
  }

  @Test
  public void coversEveryOverloadOfAName() {
    PricingService pricing = mock(PricingService.class);

    spyOn(pricing, "total").andReturn(7);

    assertEquals(7, pricing.total("A-17"));
    assertEquals(7, pricing.total(3));
    assertEquals(2, calls(pricing, "total").count());
  }

  @Test
  public void narrowsToOneSetOfArguments() {
    PricingService pricing = mock(PricingService.class);

    spyOn(pricing, "total").withArgs("A-17").andReturn(42);

    assertEquals(42, pricing.total("A-17"));
    assertEquals(0, pricing.total("A-18"));
    assertEquals(1, spyOn(pricing, "total").withArgs("A-17").calls().count());
    assertEquals(2, calls(pricing, "total").count());
  }

  @Test
  public void answersWithEachValueInTurn() {
    PricingService pricing = mock(PricingService.class);

    spyOn(pricing, "currency").andReturnValues("first", "second");

    assertEquals("first", pricing.currency());
    assertEquals("second", pricing.currency());
    assertEquals("second", pricing.currency());
  }

  @Test
  public void refusesAnEmptyReturnSequence() {
    PricingService pricing = mock(PricingService.class);

    assertThrows(
        IllegalArgumentException.class,
        () -> spyOn(pricing, "currency").andReturnValues());
  }

  @Test
  public void reportsAFailure() {
    PricingService pricing = mock(PricingService.class);
    spyOn(pricing, "currency").andThrow(new IllegalStateException("offline"));

    IllegalStateException failure = assertThrows(IllegalStateException.class, pricing::currency);

    assertEquals("offline", failure.getMessage());
  }

  @Test
  public void calculatesAResultFromTheArguments() {
    PricingService pricing = mock(PricingService.class);

    spyOn(pricing, "describe").andCallFake(call -> call.argument(0) + "!");

    assertEquals("A-17!", pricing.describe("A-17"));
  }

  @Test
  public void andCallThroughRestoresTheRealObject() {
    PricingService real = new PricingService();
    PricingService pricing = spy(PricingService.class, real);
    spyOn(pricing, "currency").andReturn("EUR");
    assertEquals("EUR", pricing.currency());

    spyOn(pricing, "currency").andCallThrough();

    assertEquals("GBP", pricing.currency());
  }

  @Test
  public void andStubAnswersWithJavasEmptyValue() {
    PricingService real = new PricingService();
    PricingService pricing = spy(PricingService.class, real);

    spyOn(pricing, "currency").andStub();

    assertNull(pricing.currency());
    assertEquals(0, real.currencyCalls());
  }

  @Test
  public void readsTheCallRecord() {
    PricingService pricing = mock(PricingService.class);

    pricing.total("A-17");
    pricing.total("A-18");
    pricing.currency();

    CallLog totals = calls(pricing, "total");
    assertTrue(totals.any());
    assertEquals(2, totals.count());
    assertEquals(List.of("A-17"), totals.first().arguments());
    assertEquals(List.of("A-18"), totals.mostRecent().arguments());
    assertEquals(List.of("A-18"), totals.argsFor(1));
    assertEquals(3, calls(pricing).count());
    assertFalse(calls(pricing, "baseRate").any());
  }

  @Test
  public void reportsWhichCallWasWantedWhenThereIsNoSuchCall() {
    PricingService pricing = mock(PricingService.class);
    pricing.total("A-17");

    IndexOutOfBoundsException failure =
        assertThrows(IndexOutOfBoundsException.class, () -> calls(pricing, "total").argsFor(4));

    assertTrue(failure.getMessage(), failure.getMessage().contains("only 1"));
  }

  @Test
  public void readsTheArgumentsOfEveryCall() {
    PricingService pricing = mock(PricingService.class);

    pricing.total("A-17");
    pricing.total("A-18");

    assertEquals(
        List.of(List.of("A-17"), List.of("A-18")), calls(pricing, "total").allArgs());
  }

  @Test
  public void forgetsTheCallsOfOneMethod() {
    PricingService pricing = mock(PricingService.class);
    spyOn(pricing, "currency").andReturn("EUR");
    pricing.total("A-17");
    pricing.currency();

    calls(pricing, "total").reset();

    assertEquals(0, calls(pricing, "total").count());
    assertEquals(1, calls(pricing, "currency").count());
    assertEquals("EUR", pricing.currency());
  }

  @Test
  public void forgetsEveryCallOnTheMock() {
    PricingService pricing = mock(PricingService.class);
    spyOn(pricing, "currency").andReturn("EUR");
    pricing.total("A-17");
    pricing.currency();

    calls(pricing).reset();

    assertEquals(0, calls(pricing).count());
    assertEquals("EUR", pricing.currency());
  }

  @Test
  public void worksAlongsideTheMockitoShapedApi() {
    PricingService pricing = mock(PricingService.class);

    spyOn(pricing, "currency").andReturn("EUR");
    pricing.currency();

    verify(pricing, times(1)).currency();
    assertEquals(1, calls(pricing, "currency").count());
  }

  @Test
  public void refusesAnObjectThatIsNotAMock() {
    IllegalArgumentException failure =
        assertThrows(
            IllegalArgumentException.class, () -> spyOn(new PricingService(), "currency"));

    assertTrue(
        failure.getMessage(),
        failure.getMessage().contains("Mockatcha.mock or Mockatcha.spy"));
  }

  public static class PricingService {

    private int currencyCalls;

    public String currency() {
      currencyCalls++;
      return "GBP";
    }

    public int total(String account) {
      return 100;
    }

    public int total(int quantity) {
      return quantity;
    }

    public String describe(String account) {
      return account;
    }

    public int baseRate() {
      return 3;
    }

    public int currencyCalls() {
      return currencyCalls;
    }
  }
}
