package io.instanto.mockatcha.bdd;

import static io.instanto.mockatcha.ArgumentMatchers.anyDouble;
import static io.instanto.mockatcha.ArgumentMatchers.anyLong;
import static io.instanto.mockatcha.ArgumentMatchers.argThat;
import static io.instanto.mockatcha.ArgumentMatchers.isNotNull;
import static io.instanto.mockatcha.ArgumentMatchers.isNull;
import static io.instanto.mockatcha.Mockatcha.mock;
import static io.instanto.mockatcha.Mockatcha.verify;
import static io.instanto.mockatcha.Mockatcha.when;
import static io.instanto.mockatcha.bdd.BddMatchers.containing;
import static io.instanto.mockatcha.bdd.BddMatchers.containingExactly;
import static io.instanto.mockatcha.bdd.BddMatchers.instanceOf;
import static io.instanto.mockatcha.bdd.BddMatchers.mapContaining;
import static io.instanto.mockatcha.bdd.BddMatchers.ofSize;
import static io.instanto.mockatcha.bdd.BddMatchers.stringContaining;
import static io.instanto.mockatcha.bdd.BddMatchers.stringMatching;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;

import java.util.List;
import java.util.Map;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.junit.SkipJVM;
import org.teavm.junit.TeaVMTestRunner;

/** Matchers that describe the shape of an argument rather than its exact value. */
@RunWith(TeaVMTestRunner.class)
@SkipJVM
public class BddMatchersTeaVmTest {

  @Test
  public void matchesASubstring() {
    Audit audit = mock(Audit.class);

    when(audit.record(stringContaining("timesheet"))).thenReturn("kept");

    assertEquals("kept", audit.record("submitted timesheet A-17"));
    assertNull(audit.record("submitted expenses"));
  }

  @Test
  public void matchesARegularExpression() {
    Audit audit = mock(Audit.class);

    when(audit.record(stringMatching("^audit:[A-Z]-\\d+$"))).thenReturn("kept");

    assertEquals("kept", audit.record("audit:A-17"));
    assertNull(audit.record("audit:lowercase"));
    verify(audit).record(stringMatching("A-17"));
  }

  @Test
  public void matchesACollectionThatContainsItems() {
    Audit audit = mock(Audit.class);

    when(audit.batch(containing("A-17"))).thenReturn(1);

    assertEquals(1, audit.batch(List.of("A-16", "A-17", "A-18")));
    assertEquals(0, audit.batch(List.of("A-16")));
  }

  @Test
  public void matchesACollectionOfAGivenSize() {
    Audit audit = mock(Audit.class);

    when(audit.batch(ofSize(2))).thenReturn(2);

    assertEquals(2, audit.batch(List.of("A-16", "A-17")));
    assertEquals(0, audit.batch(List.of("A-16")));
  }

  @Test
  public void matchesAMapEntry() {
    Audit audit = mock(Audit.class);

    when(audit.tag(mapContaining("employee", "A-17"))).thenReturn("kept");

    assertEquals("kept", audit.tag(Map.of("employee", "A-17", "hours", "38")));
    assertNull(audit.tag(Map.of("employee", "A-18")));
  }

  @Test
  public void matchesACollectionHoldingExactlyTheseItems() {
    Audit audit = mock(Audit.class);

    when(audit.batch(containingExactly("A-17", "A-16"))).thenReturn(3);

    assertEquals(3, audit.batch(List.of("A-16", "A-17")));
    assertEquals(0, audit.batch(List.of("A-16", "A-17", "A-18")));
    assertEquals(0, audit.batch(List.of("A-16")));
  }

  @Test
  public void exactCollectionMatchingAccountsForDuplicates() {
    Audit audit = mock(Audit.class);

    when(audit.batch(containingExactly("A-17", "A-17"))).thenReturn(3);

    assertEquals(3, audit.batch(List.of("A-17", "A-17")));
    assertEquals(0, audit.batch(List.of("A-17", "A-18")));
  }

  @Test
  public void exactCollectionMatchingSupportsPrimitiveArrays() {
    Audit audit = mock(Audit.class);

    when(audit.primitiveBatch(containingExactly(3, 1, 3))).thenReturn(7);

    assertEquals(7, audit.primitiveBatch(new int[] {3, 1, 3}));
    assertEquals(0, audit.primitiveBatch(new int[] {3, 1, 2}));
  }

  @Test
  public void refusesANegativeCollectionSize() {
    assertThrows(IllegalArgumentException.class, () -> ofSize(-1));
  }

  @Test
  public void matchesAValueOfAGivenType() {
    Audit audit = mock(Audit.class);

    when(audit.tagAny(instanceOf(String.class))).thenReturn("a string");
    when(audit.tagAny(instanceOf(Integer.class))).thenReturn("a number");

    assertEquals("a string", audit.tagAny("A-17"));
    assertEquals("a number", audit.tagAny(38));
    assertNull(audit.tagAny(1.5));
  }

  @Test
  public void matchesWithAConditionOfYourOwn() {
    Audit audit = mock(Audit.class);

    when(audit.batch(argThat(value -> ((List<?>) value).size() > 2))).thenReturn(9);

    assertEquals(9, audit.batch(List.of("a", "b", "c")));
    assertEquals(0, audit.batch(List.of("a")));
  }

  @Test
  public void matchesTheWiderPrimitiveTypes() {
    Audit audit = mock(Audit.class);

    when(audit.measure(anyLong(), anyDouble())).thenReturn("measured");

    assertEquals("measured", audit.measure(5L, 1.5D));
  }

  @Test
  public void matchesPresenceAndAbsence() {
    Audit audit = mock(Audit.class);

    when(audit.record(isNull())).thenReturn("missing");
    when(audit.record(isNotNull())).thenReturn("present");

    assertEquals("missing", audit.record(null));
    assertEquals("present", audit.record("anything"));
  }

  public interface Audit {

    String record(String note);

    int batch(List<String> accounts);

    int primitiveBatch(int[] accounts);

    String tag(Map<String, String> tags);

    String measure(long count, double rate);

    String tagAny(Object value);
  }
}
