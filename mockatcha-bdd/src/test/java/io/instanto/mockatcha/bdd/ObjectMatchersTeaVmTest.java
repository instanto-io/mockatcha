/*
 * Copyright 2026 Carl Stainton
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.instanto.mockatcha.bdd;

import static io.instanto.mockatcha.Mockatcha.mock;
import static io.instanto.mockatcha.Mockatcha.never;
import static io.instanto.mockatcha.Mockatcha.verify;
import static io.instanto.mockatcha.Mockatcha.when;
import static io.instanto.mockatcha.bdd.ObjectMatchers.objectContaining;
import static io.instanto.mockatcha.bdd.ObjectMatchers.propertiesOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.junit.SkipJVM;
import org.teavm.junit.TeaVMTestRunner;

/**
 * Matching an object by naming some of its properties.
 *
 * <p>Jasmine's {@code objectContaining} relies on JavaScript property lookup. These tests prove the
 * generated equivalent reads real accessors and fields in the browser, with no reflection.
 */
@RunWith(TeaVMTestRunner.class)
@SkipJVM
public class ObjectMatchersTeaVmTest {

  @Test
  public void matchesOnOneRecordStyleAccessor() {
    Repository repository = mock(Repository.class);

    repository.save(new Timesheet("A-17", 38, true));

    verify(repository).save(objectContaining(Timesheet.class, "employeeId", "A-17"));
    verify(repository, never()).save(objectContaining(Timesheet.class, "employeeId", "A-18"));
  }

  @Test
  public void matchesOnSeveralPropertiesAtOnce() {
    Repository repository = mock(Repository.class);

    repository.save(new Timesheet("A-17", 38, true));
    repository.save(new Timesheet("A-17", 41, true));

    verify(repository)
        .save(objectContaining(Timesheet.class, Map.of("employeeId", "A-17", "hours", 38)));
    verify(repository, never())
        .save(objectContaining(Timesheet.class, Map.of("employeeId", "A-17", "hours", 99)));
  }

  @Test
  public void readsGetterAndIsAccessorNames() {
    Repository repository = mock(Repository.class);

    repository.save(new Timesheet("A-17", 38, true));

    verify(repository).save(objectContaining(Timesheet.class, "department", "Engineering"));
    verify(repository).save(objectContaining(Timesheet.class, "approved", true));
  }

  @Test
  public void readsAVisibleField() {
    Repository repository = mock(Repository.class);

    repository.save(new Timesheet("A-17", 38, true));

    verify(repository).save(objectContaining(Timesheet.class, "source", "web"));
  }

  @Test
  public void readsAnInheritedAccessor() {
    Repository repository = mock(Repository.class);

    repository.saveOvertime(new OvertimeTimesheet("A-17", 50, true, 10));

    verify(repository)
        .saveOvertime(
            objectContaining(OvertimeTimesheet.class, Map.of("employeeId", "A-17", "overtime", 10)));
  }

  @Test
  public void worksForStubbingAsWellAsVerification() {
    Repository repository = mock(Repository.class);

    when(repository.describe(objectContaining(Timesheet.class, "hours", 38))).thenReturn("normal");

    assertEquals("normal", repository.describe(new Timesheet("A-17", 38, true)));
    assertNull(repository.describe(new Timesheet("A-17", 41, true)));
  }

  @Test
  public void doesNotMatchNullOrAnUnrelatedType() {
    PropertyReader<Timesheet> reader = propertiesOf(Timesheet.class);

    assertFalse(reader.canRead(null));
    assertFalse(reader.canRead("not a timesheet"));
    assertTrue(reader.canRead(new Timesheet("A-17", 38, true)));
  }

  @Test
  public void readsPropertiesDirectlyWhenThatIsWhatATestNeeds() {
    PropertyReader<Timesheet> reader = propertiesOf(Timesheet.class);
    Timesheet timesheet = new Timesheet("A-17", 38, true);

    assertEquals("A-17", reader.read(timesheet, "employeeId"));
    assertEquals(38, reader.read(timesheet, "hours"));
    assertEquals(true, reader.read(timesheet, "approved"));
    assertEquals("web", reader.read(timesheet, "source"));
  }

  @Test
  public void listsThePropertiesItCanRead() {
    List<String> names = Arrays.asList(propertiesOf(Timesheet.class).names());

    assertTrue(names.toString(), names.contains("employeeId"));
    assertTrue(names.toString(), names.contains("hours"));
    assertTrue(names.toString(), names.contains("department"));
    assertTrue(names.toString(), names.contains("approved"));
    assertTrue(names.toString(), names.contains("source"));
    assertFalse(names.toString(), names.contains("toString"));
    assertFalse(names.toString(), names.contains("secret"));
  }

  @Test
  public void refusesAMisspelledPropertyImmediately() {
    IllegalArgumentException failure =
        assertThrows(
            IllegalArgumentException.class,
            () -> objectContaining(Timesheet.class, "employeeID", "A-17"));

    assertTrue(failure.getMessage(), failure.getMessage().contains("No property named"));
    assertTrue(failure.getMessage(), failure.getMessage().contains("employeeId"));
  }

  public interface Repository {

    void save(Timesheet timesheet);

    void saveOvertime(OvertimeTimesheet timesheet);

    String describe(Timesheet timesheet);
  }

  public static class Timesheet {

    /** A visible field is a property too, which is what a value-style class often exposes. */
    public final String source = "web";

    private final String employeeId;
    private final int hours;
    private final boolean approved;
    private final String secret = "not a property";

    public Timesheet(String employeeId, int hours, boolean approved) {
      this.employeeId = employeeId;
      this.hours = hours;
      this.approved = approved;
    }

    public String employeeId() {
      return employeeId;
    }

    public int hours() {
      return hours;
    }

    public boolean isApproved() {
      return approved;
    }

    public String getDepartment() {
      return "Engineering";
    }

    private String secret() {
      return secret;
    }

    @Override
    public String toString() {
      return "Timesheet " + employeeId;
    }
  }

  public static class OvertimeTimesheet extends Timesheet {

    private final int overtime;

    public OvertimeTimesheet(String employeeId, int hours, boolean approved, int overtime) {
      super(employeeId, hours, approved);
      this.overtime = overtime;
    }

    public int overtime() {
      return overtime;
    }
  }
}
