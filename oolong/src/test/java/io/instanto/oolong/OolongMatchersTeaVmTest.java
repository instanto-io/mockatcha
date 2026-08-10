package io.instanto.oolong;

import static io.instanto.mockatcha.ArgumentMatchers.anyDouble;
import static io.instanto.mockatcha.ArgumentMatchers.anyLong;
import static io.instanto.mockatcha.ArgumentMatchers.argThat;
import static io.instanto.mockatcha.ArgumentMatchers.isNotNull;
import static io.instanto.mockatcha.ArgumentMatchers.isNull;
import static io.instanto.mockatcha.Mockatcha.mock;
import static io.instanto.mockatcha.Mockatcha.verify;
import static io.instanto.mockatcha.Mockatcha.when;
import static io.instanto.oolong.OolongMatchers.containing;
import static io.instanto.oolong.OolongMatchers.mapContaining;
import static io.instanto.oolong.OolongMatchers.ofSize;
import static io.instanto.oolong.OolongMatchers.stringContaining;
import static io.instanto.oolong.OolongMatchers.stringMatching;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.List;
import java.util.Map;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.junit.SkipJVM;
import org.teavm.junit.TeaVMTestRunner;

/** Matchers that describe the shape of an argument rather than its exact value. */
@RunWith(TeaVMTestRunner.class)
@SkipJVM
public class OolongMatchersTeaVmTest {

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

    String tag(Map<String, String> tags);

    String measure(long count, double rate);
  }
}
