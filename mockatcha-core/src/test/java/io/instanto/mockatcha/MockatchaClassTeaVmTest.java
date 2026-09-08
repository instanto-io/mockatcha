/*
 * Copyright 2026 Carl Stainton
 *
 * SPDX-License-Identifier: Apache-2.0
 */
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
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.junit.SkipJVM;
import org.teavm.junit.TeaVMTestRunner;

/** Mocking of ordinary classes, which Mockatcha supports through a generated subclass. */
@RunWith(TeaVMTestRunner.class)
@SkipJVM
public class MockatchaClassTeaVmTest {

  @Test
  public void stubsAndVerifiesAClassWithTheSameCallsAsAnInterface() {
    PricingService pricing = mock(PricingService.class);

    when(pricing.total("A-17")).thenReturn(42);

    assertEquals(42, pricing.total("A-17"));
    assertEquals(0, pricing.total("A-18"));
    verify(pricing).total("A-17");
    verify(pricing, never()).total("A-99");
  }

  @Test
  public void unstubbedMethodsReturnJavaEmptyValuesRatherThanRealOnes() {
    PricingService pricing = mock(PricingService.class);

    assertNull(pricing.currency());
    assertEquals(0, pricing.total("A-17"));
    assertFalse(pricing.enabled());
  }

  @Test
  public void supportsEveryPrimitiveResult() {
    Primitives primitives = mock(Primitives.class);

    when(primitives.asBoolean()).thenReturn(true);
    when(primitives.asByte()).thenReturn((byte) 7);
    when(primitives.asShort()).thenReturn((short) 8);
    when(primitives.asChar()).thenReturn('x');
    when(primitives.asInt()).thenReturn(9);
    when(primitives.asLong()).thenReturn(10L);
    when(primitives.asFloat()).thenReturn(1.5F);
    when(primitives.asDouble()).thenReturn(2.5D);

    assertTrue(primitives.asBoolean());
    assertEquals((byte) 7, primitives.asByte());
    assertEquals((short) 8, primitives.asShort());
    assertEquals('x', primitives.asChar());
    assertEquals(9, primitives.asInt());
    assertEquals(10L, primitives.asLong());
    assertEquals(1.5F, primitives.asFloat(), 0F);
    assertEquals(2.5D, primitives.asDouble(), 0D);
  }

  @Test
  public void recordsWideArgumentsInTheirDeclaredSlots() {
    Primitives primitives = mock(Primitives.class);

    when(primitives.combine(1L, 2.5D, 3)).thenReturn("wide");

    assertEquals("wide", primitives.combine(1L, 2.5D, 3));
    assertNull(primitives.combine(1L, 2.5D, 4));
    verify(primitives).combine(1L, 2.5D, 3);
  }

  @Test
  public void recordsVoidCallsWithoutRunningRealBehaviour() {
    PricingService pricing = mock(PricingService.class);

    pricing.record("audited");

    verify(pricing).record("audited");
    assertEquals(0, pricing.recordCount());
  }

  @Test
  public void distinguishesAStubbedNullFromNoStubbing() {
    PricingService pricing = mock(PricingService.class);

    when(pricing.currency()).thenReturn(null);

    assertNull(pricing.currency());
    verify(pricing).currency();
  }

  @Test
  public void reportsAStubbedFailure() {
    PricingService pricing = mock(PricingService.class);
    when(pricing.total("A-17")).thenThrow(new IllegalStateException("rates unavailable"));

    IllegalStateException failure =
        assertThrows(IllegalStateException.class, () -> pricing.total("A-17"));

    assertEquals("rates unavailable", failure.getMessage());
  }

  @Test
  public void supportsMatchersAnswersAndConsecutiveResults() {
    PricingService pricing = mock(PricingService.class);

    when(pricing.describe(anyString(), eq(2))).thenAnswer(call -> call.argument(0) + "/2");
    when(pricing.total(anyString())).thenReturn(1).thenReturn(2);

    assertEquals("A-17/2", pricing.describe("A-17", 2));
    assertNull(pricing.describe("A-17", 3));
    assertEquals(1, pricing.total("A-17"));
    assertEquals(2, pricing.total("A-18"));
    assertEquals(2, pricing.total("A-19"));
    verify(pricing, times(3)).total(anyString());
  }

  @Test
  public void configuresOverloadsIndependently() {
    PricingService pricing = mock(PricingService.class);

    when(pricing.total("A-17")).thenReturn(1);
    when(pricing.total(7)).thenReturn(2);

    assertEquals(1, pricing.total("A-17"));
    assertEquals(2, pricing.total(7));
    verify(pricing).total("A-17");
    verify(pricing).total(7);
  }

  @Test
  public void mocksMethodsInheritedFromASuperclass() {
    PremiumPricingService pricing = mock(PremiumPricingService.class);

    when(pricing.currency()).thenReturn("EUR");
    when(pricing.discount()).thenReturn(15);

    assertEquals("EUR", pricing.currency());
    assertEquals(15, pricing.discount());
  }

  @Test
  public void mocksAbstractClassesIncludingTheirAbstractMethods() {
    Report report = mock(Report.class);

    when(report.title()).thenReturn("Quarterly");
    when(report.rows()).thenReturn(3);

    assertEquals("Quarterly", report.title());
    assertEquals(3, report.rows());
  }

  @Test
  public void mocksProtectedMethods() {
    PricingService pricing = mock(PricingService.class);

    when(pricing.baseRate()).thenReturn(11);

    assertEquals(11, pricing.baseRate());
    verify(pricing).baseRate();
  }

  @Test
  public void leavesFinalAndStaticMethodsAlone() {
    PricingService pricing = mock(PricingService.class);

    assertEquals("final", pricing.stamp());
    assertEquals("static", PricingService.version());
  }

  @Test
  public void routesACallThroughAGenericBridgeMethodToTheOverride() {
    StringBox box = mock(StringBox.class);
    when(box.get()).thenReturn("stubbed");

    Box<String> asBase = box;

    assertEquals("stubbed", box.get());
    assertEquals("stubbed", asBase.get());
  }

  @Test
  public void answersObjectMethodsWithoutRecordingOrRunningRealCode() {
    PricingService first = mock(PricingService.class);
    PricingService second = mock(PricingService.class);

    assertEquals(first, first);
    assertNotEquals(first, second);
    assertEquals(first.hashCode(), first.hashCode());
    assertEquals("Mockatcha mock of io.instanto.mockatcha.MockatchaClassTeaVmTest$PricingService",
        first.toString());
    assertEquals(0, mockingDetails(first).getInvocations().size());
  }

  @Test
  public void keepsTwoInstancesOfOneGeneratedClassIndependent() {
    PricingService first = mock(PricingService.class);
    PricingService second = mock(PricingService.class);

    when(first.total("A-17")).thenReturn(1);
    when(second.total("A-17")).thenReturn(2);
    first.total("A-17");

    assertEquals(1, first.total("A-17"));
    assertEquals(2, second.total("A-17"));
    verify(first, times(2)).total("A-17");
    verify(second, times(1)).total("A-17");
  }

  @Test
  public void reportsCallHistoryAndClearsOrResetsIt() {
    PricingService pricing = mock(PricingService.class);
    when(pricing.total("A-17")).thenReturn(42);
    pricing.total("A-17");

    assertTrue(mockingDetails(pricing).isMock());
    assertEquals(1, mockingDetails(pricing).getInvocations().size());
    assertEquals("total", mockingDetails(pricing).getInvocations().get(0).methodName());

    clearInvocations(pricing);
    assertEquals(42, pricing.total("A-17"));

    reset(pricing);
    assertEquals(0, pricing.total("A-17"));
    verify(pricing).total("A-17");
  }

  @Test
  public void runsTheNoArgumentConstructorOfTheMockedClass() {
    PricingService pricing = mock(PricingService.class);

    assertTrue(pricing.constructorRan());
  }

  @Test
  public void preservesVirtualCallsMadeByAConstructor() {
    ConstructorHook service = mock(ConstructorHook.class);

    assertEquals("real during construction", service.valueSeenByConstructor());
    assertNull(service.value());
  }

  @Test
  public void givesAbstractConstructorHooksAnEmptyValue() {
    AbstractConstructorHook service = mock(AbstractConstructorHook.class);

    assertEquals(0, service.valueSeenByConstructor());
    assertEquals(0, service.value());
  }

  public static class PricingService {

    private final boolean constructed;
    private int recorded;

    public PricingService() {
      this.constructed = true;
    }

    public int total(String account) {
      return 100;
    }

    public int total(int quantity) {
      return quantity * 100;
    }

    public String currency() {
      return "GBP";
    }

    public String describe(String account, int precision) {
      return account + "/" + precision;
    }

    public boolean enabled() {
      return true;
    }

    public void record(String note) {
      recorded++;
    }

    public int recordCount() {
      return recorded;
    }

    protected int baseRate() {
      return 3;
    }

    public final String stamp() {
      return "final";
    }

    public static String version() {
      return "static";
    }

    // A final method cannot be overridden, so this reads the generated subclass's own state only
    // through methods the subclass does not replace.
    public final boolean constructorRan() {
      return constructed;
    }
  }

  public static class PremiumPricingService extends PricingService {

    public int discount() {
      return 5;
    }
  }

  public static class ConstructorHook {

    private final String constructedWith = value();

    public String value() {
      return "real during construction";
    }

    public final String valueSeenByConstructor() {
      return constructedWith;
    }
  }

  public abstract static class AbstractConstructorHook {

    private final int constructedWith = value();

    public abstract int value();

    public final int valueSeenByConstructor() {
      return constructedWith;
    }
  }

  public abstract static class Report {

    public abstract String title();

    public int rows() {
      return 1;
    }
  }

  public static class Box<T> {

    public T get() {
      return null;
    }
  }

  public static class StringBox extends Box<String> {

    @Override
    public String get() {
      return "real";
    }
  }

  public static class Primitives {

    public boolean asBoolean() {
      return false;
    }

    public byte asByte() {
      return 1;
    }

    public short asShort() {
      return 2;
    }

    public char asChar() {
      return 'a';
    }

    public int asInt() {
      return 3;
    }

    public long asLong() {
      return 4L;
    }

    public float asFloat() {
      return 5F;
    }

    public double asDouble() {
      return 6D;
    }

    public String combine(long first, double second, int third) {
      return "real";
    }
  }
}
