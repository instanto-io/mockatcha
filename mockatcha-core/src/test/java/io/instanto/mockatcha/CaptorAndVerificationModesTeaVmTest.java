/*
 * Copyright 2026 Carl Stainton
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.instanto.mockatcha;

import static io.instanto.mockatcha.ArgumentMatchers.anyString;
import static io.instanto.mockatcha.ArgumentMatchers.eq;
import static io.instanto.mockatcha.Mockatcha.atLeast;
import static io.instanto.mockatcha.Mockatcha.atLeastOnce;
import static io.instanto.mockatcha.Mockatcha.atMost;
import static io.instanto.mockatcha.Mockatcha.mock;
import static io.instanto.mockatcha.Mockatcha.never;
import static io.instanto.mockatcha.Mockatcha.only;
import static io.instanto.mockatcha.Mockatcha.times;
import static io.instanto.mockatcha.Mockatcha.verify;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.junit.TeaVMTestRunner;

/** Capturing the arguments a call was made with, and the counting rules verification can apply. */
@RunWith(TeaVMTestRunner.class)
public class CaptorAndVerificationModesTeaVmTest extends MockatchaTestLifecycle {

  @Test
  public void capturesAnArgumentTheTestCouldNotHavePredicted() {
    Repository repository = mock(Repository.class);
    ArgumentCaptor<Timesheet> saved = ArgumentCaptor.forClass(Timesheet.class);

    new SubmissionService(repository).submit("A-17", 38);

    verify(repository).save(saved.capture());
    assertEquals("A-17", saved.getValue().employeeId());
    assertEquals(38, saved.getValue().hours());
  }

  @Test
  public void capturesEveryMatchingCallInOrder() {
    Repository repository = mock(Repository.class);
    ArgumentCaptor<Timesheet> saved = ArgumentCaptor.captor();
    SubmissionService service = new SubmissionService(repository);

    service.submit("A-17", 38);
    service.submit("A-18", 41);

    verify(repository, times(2)).save(saved.capture());
    assertEquals(2, saved.getAllValues().size());
    assertEquals("A-17", saved.getAllValues().get(0).employeeId());
    assertEquals("A-18", saved.getAllValues().get(1).employeeId());
    assertEquals("A-18", saved.getValue().employeeId());
  }

  @Test
  public void capturesAlongsideOtherMatchers() {
    Repository repository = mock(Repository.class);
    ArgumentCaptor<String> note = ArgumentCaptor.forClass(String.class);

    repository.audit("A-17", "submitted 38 hours");
    repository.audit("A-18", "submitted 41 hours");

    verify(repository).audit(eq("A-17"), note.capture());
    assertEquals("submitted 38 hours", note.getValue());
  }

  @Test
  public void doesNotCaptureACallRejectedByALaterMatcher() {
    Repository repository = mock(Repository.class);
    ArgumentCaptor<String> employee = ArgumentCaptor.forClass(String.class);
    repository.audit("not wanted", "draft");
    repository.audit("A-17", "submitted");

    verify(repository).audit(employee.capture(), eq("submitted"));

    assertEquals(List.of("A-17"), employee.getAllValues());
  }

  @Test
  public void capturesAnArgumentOfAPrimitiveParameter() {
    Repository repository = mock(Repository.class);
    ArgumentCaptor<Integer> hours = ArgumentCaptor.forClass(int.class);

    repository.record(38);

    verify(repository).record(hours.capture());
    assertEquals(Integer.valueOf(38), hours.getValue());
  }

  @Test
  public void capturesWhileStubbingToo() {
    Repository repository = mock(Repository.class);
    ArgumentCaptor<String> asked = ArgumentCaptor.forClass(String.class);
    Mockatcha.when(repository.find(asked.capture())).thenReturn(new Timesheet("A-17", 38));

    repository.find("A-17");

    assertEquals("A-17", asked.getValue());
  }

  @Test
  public void clearsCapturedValuesBetweenVerifications() {
    Repository repository = mock(Repository.class);
    ArgumentCaptor<Integer> hours = ArgumentCaptor.forClass(Integer.class);
    repository.record(38);

    verify(repository).record(hours.capture());
    hours.clear();
    repository.record(41);
    verify(repository).record(41);

    assertEquals(List.of(), hours.getAllValues());
  }

  @Test
  public void explainsWhyNothingWasCaptured() {
    ArgumentCaptor<String> never = ArgumentCaptor.forClass(String.class);

    IllegalStateException failure = assertThrows(IllegalStateException.class, never::getValue);

    assertTrue(failure.getMessage(), failure.getMessage().contains("No argument was captured"));
  }

  @Test
  public void atLeastAcceptsThatManyCallsOrMore() {
    Repository repository = mock(Repository.class);
    repository.record(1);
    repository.record(2);
    repository.record(3);

    verify(repository, atLeast(2)).record(ArgumentMatchers.anyInt());
    verify(repository, atLeast(3)).record(ArgumentMatchers.anyInt());
    verify(repository, atLeastOnce()).record(ArgumentMatchers.anyInt());

    AssertionError failure =
        assertThrows(
            AssertionError.class,
            () -> verify(repository, atLeast(4)).record(ArgumentMatchers.anyInt()));
    assertTrue(failure.getMessage(), failure.getMessage().contains("at least 4"));
  }

  @Test
  public void atMostAcceptsThatManyCallsOrFewer() {
    Repository repository = mock(Repository.class);
    repository.record(1);
    repository.record(2);

    verify(repository, atMost(2)).record(ArgumentMatchers.anyInt());
    verify(repository, atMost(5)).record(ArgumentMatchers.anyInt());
    verify(repository, atMost(0)).audit(anyString(), anyString());

    AssertionError failure =
        assertThrows(
            AssertionError.class,
            () -> verify(repository, atMost(1)).record(ArgumentMatchers.anyInt()));
    assertTrue(failure.getMessage(), failure.getMessage().contains("at most 1"));
  }

  @Test
  public void onlyRequiresOneCallAndNothingElseOnTheMock() {
    Repository repository = mock(Repository.class);
    repository.record(38);

    verify(repository, only()).record(38);
  }

  @Test
  public void onlyRejectsAnotherCallOnTheSameMock() {
    Repository repository = mock(Repository.class);
    repository.record(38);
    repository.audit("A-17", "submitted");

    AssertionError failure =
        assertThrows(AssertionError.class, () -> verify(repository, only()).record(38));

    assertTrue(failure.getMessage(), failure.getMessage().contains("only call"));
    assertTrue(failure.getMessage(), failure.getMessage().contains("1 other call"));
  }

  @Test
  public void onlyRejectsARepeatedCall() {
    Repository repository = mock(Repository.class);
    repository.record(38);
    repository.record(38);

    assertThrows(AssertionError.class, () -> verify(repository, only()).record(38));
  }

  @Test
  public void countsStillRejectNegativeExpectations() {
    assertThrows(IllegalArgumentException.class, () -> times(-1));
    assertThrows(IllegalArgumentException.class, () -> atLeast(-1));
    assertThrows(IllegalArgumentException.class, () -> atMost(-1));
  }

  @Test
  public void existingModesAreUnchanged() {
    Repository repository = mock(Repository.class);
    repository.record(38);

    verify(repository).record(38);
    verify(repository, times(1)).record(38);
    verify(repository, never()).record(39);
  }

  public interface Repository {

    void save(Timesheet timesheet);

    void audit(String employeeId, String note);

    void record(int hours);

    Timesheet find(String employeeId);
  }

  public static final class Timesheet {

    private final String employeeId;
    private final int hours;

    public Timesheet(String employeeId, int hours) {
      this.employeeId = employeeId;
      this.hours = hours;
    }

    public String employeeId() {
      return employeeId;
    }

    public int hours() {
      return hours;
    }
  }

  /** Builds the argument itself, which is what makes a captor the right tool. */
  public static final class SubmissionService {

    private final Repository repository;

    public SubmissionService(Repository repository) {
      this.repository = repository;
    }

    public void submit(String employeeId, int hours) {
      repository.save(new Timesheet(employeeId, hours));
    }
  }
}
