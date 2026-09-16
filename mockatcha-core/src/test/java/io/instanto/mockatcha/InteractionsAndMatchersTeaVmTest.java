/*
 * Copyright 2026 Carl Stainton
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.instanto.mockatcha;

import static io.instanto.mockatcha.AdditionalMatchers.and;
import static io.instanto.mockatcha.AdditionalMatchers.aryEq;
import static io.instanto.mockatcha.AdditionalMatchers.cmpEq;
import static io.instanto.mockatcha.AdditionalMatchers.find;
import static io.instanto.mockatcha.AdditionalMatchers.geq;
import static io.instanto.mockatcha.AdditionalMatchers.gt;
import static io.instanto.mockatcha.AdditionalMatchers.leq;
import static io.instanto.mockatcha.AdditionalMatchers.lt;
import static io.instanto.mockatcha.AdditionalMatchers.not;
import static io.instanto.mockatcha.AdditionalMatchers.or;
import static io.instanto.mockatcha.ArgumentMatchers.any;
import static io.instanto.mockatcha.ArgumentMatchers.anyString;
import static io.instanto.mockatcha.ArgumentMatchers.endsWith;
import static io.instanto.mockatcha.ArgumentMatchers.eq;
import static io.instanto.mockatcha.ArgumentMatchers.nullable;
import static io.instanto.mockatcha.ArgumentMatchers.same;
import static io.instanto.mockatcha.ArgumentMatchers.startsWith;
import static io.instanto.mockatcha.Mockatcha.inOrder;
import static io.instanto.mockatcha.Mockatcha.doReturn;
import static io.instanto.mockatcha.Mockatcha.lenient;
import static io.instanto.mockatcha.Mockatcha.mock;
import static io.instanto.mockatcha.Mockatcha.times;
import static io.instanto.mockatcha.Mockatcha.validateUsage;
import static io.instanto.mockatcha.Mockatcha.verify;
import static io.instanto.mockatcha.Mockatcha.verifyNoInteractions;
import static io.instanto.mockatcha.Mockatcha.verifyNoMoreInteractions;
import static io.instanto.mockatcha.Mockatcha.when;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.junit.TeaVMTestRunner;

/** Accounting for every call, comparing and combining matchers, and reporting misuse. */
@RunWith(TeaVMTestRunner.class)
public class InteractionsAndMatchersTeaVmTest extends MockatchaTestLifecycle {

  @Test
  public void acceptsWhenEveryCallHasBeenVerified() {
    Repository repository = mock(Repository.class);

    repository.save("report");
    repository.audit("done");

    verify(repository).save("report");
    verify(repository).audit("done");
    verifyNoMoreInteractions(repository);
  }

  @Test
  public void reportsACallThatWasNeverVerified() {
    Repository repository = mock(Repository.class);

    repository.save("report");
    repository.audit("forgotten");

    verify(repository).save("report");

    AssertionError failure =
        assertThrows(AssertionError.class, () -> verifyNoMoreInteractions(repository));
    assertTrue(failure.getMessage(), failure.getMessage().contains("audit"));
  }

  @Test
  public void countsAVerificationThatCoveredSeveralCalls() {
    Repository repository = mock(Repository.class);

    repository.save("report");
    repository.save("report");

    verify(repository, times(2)).save("report");
    verifyNoMoreInteractions(repository);
  }

  @Test
  public void countsOrderedVerificationToo() {
    Repository repository = mock(Repository.class);

    repository.save("report");
    repository.audit("done");

    InOrder order = inOrder(repository);
    order.verify(repository).save("report");
    order.verify(repository).audit("done");
    verifyNoMoreInteractions(repository);
  }

  @Test
  public void expectsAMockToHaveBeenLeftAlone() {
    Repository repository = mock(Repository.class);
    Repository untouched = mock(Repository.class);

    repository.save("report");

    verifyNoInteractions(untouched);
    assertThrows(AssertionError.class, () -> verifyNoInteractions(repository));
  }

  @Test
  public void aFailureListsTheCallsThatDidHappen() {
    Repository repository = mock(Repository.class);

    repository.save("report");
    repository.audit("done");

    AssertionError failure =
        assertThrows(AssertionError.class, () -> verify(repository).save("something else"));

    assertTrue(failure.getMessage(), failure.getMessage().contains("Calls recorded on this mock"));
    assertTrue(failure.getMessage(), failure.getMessage().contains("save"));
    assertTrue(failure.getMessage(), failure.getMessage().contains("audit"));
  }

  @Test
  public void aFailureSaysSoWhenNothingWasCalled() {
    Repository repository = mock(Repository.class);

    AssertionError failure =
        assertThrows(AssertionError.class, () -> verify(repository).save("report"));

    assertTrue(failure.getMessage(), failure.getMessage().contains("No calls were recorded"));
  }

  @Test
  public void aFailedVerificationDoesNotConsumeTheCall() {
    Repository repository = mock(Repository.class);
    repository.save("report");

    assertThrows(AssertionError.class, () -> verify(repository, times(2)).save("report"));
    assertThrows(AssertionError.class, () -> verifyNoMoreInteractions(repository));

    verify(repository).save("report");
    verifyNoMoreInteractions(repository);
  }

  @Test
  public void combinesTwoMatchers() {
    Repository repository = mock(Repository.class);

    when(repository.grade(and(gt(10), lt(100)))).thenReturn("middling");

    assertEquals("middling", repository.grade(50));
    assertNull(repository.grade(5));
    assertNull(repository.grade(500));
  }

  @Test
  public void acceptsEitherMatcher() {
    Repository repository = mock(Repository.class);

    when(repository.grade(or(lt(10), gt(100)))).thenReturn("extreme");

    assertEquals("extreme", repository.grade(5));
    assertEquals("extreme", repository.grade(500));
    assertNull(repository.grade(50));
  }

  @Test
  public void invertsAMatcher() {
    Repository repository = mock(Repository.class);

    when(repository.describe(not(eq("ignored")))).thenReturn("kept");

    assertEquals("kept", repository.describe("anything"));
    assertNull(repository.describe("ignored"));
  }

  @Test
  public void comparesNumbers() {
    Repository repository = mock(Repository.class);

    when(repository.grade(geq(10))).thenReturn("at least ten");
    when(repository.grade(leq(5))).thenReturn("at most five");

    assertEquals("at most five", repository.grade(5));
    assertEquals("at least ten", repository.grade(10));
    assertNull(repository.grade(7));
  }

  @Test
  public void comparesLargeLongsWithoutLosingPrecision() {
    Repository repository = mock(Repository.class);
    long boundary = 9_007_199_254_740_992L;

    when(repository.gradeLong(AdditionalMatchers.gt(boundary))).thenReturn("greater");

    assertNull(repository.gradeLong(boundary));
    assertEquals("greater", repository.gradeLong(boundary + 1));
  }

  @Test
  public void comparesAnythingWithAnOrdering() {
    Repository repository = mock(Repository.class);

    when(repository.describe(AdditionalMatchers.gt("m"))).thenReturn("late in the alphabet");

    assertEquals("late in the alphabet", repository.describe("ز"));
    assertEquals("late in the alphabet", repository.describe("zebra"));
    assertNull(repository.describe("apple"));
  }

  @Test
  public void comparesForEqualityByOrdering() {
    Repository repository = mock(Repository.class);

    when(repository.describe(cmpEq("exact"))).thenReturn("found");

    assertEquals("found", repository.describe("exact"));
    assertNull(repository.describe("other"));
  }

  @Test
  public void matchesArraysByContent() {
    Repository repository = mock(Repository.class);

    repository.write(new byte[] {1, 2, 3});

    verify(repository).write(aryEq(new byte[] {1, 2, 3}));
    verify(repository, times(0)).write(aryEq(new byte[] {9}));
  }

  @Test
  public void findsARegularExpressionInAString() {
    Repository repository = mock(Repository.class);

    when(repository.describe(find("A-\\d+"))).thenReturn("an account");

    assertEquals("an account", repository.describe("timesheet for A-17"));
    assertEquals("an account", repository.describe("first line\nA-18\nlast line"));
    assertNull(repository.describe("timesheet"));
  }

  @Test
  public void matchesByRuntimeTypeWithOptionalNullability() {
    Repository repository = mock(Repository.class);

    when(repository.classify(any(String.class))).thenReturn("string");
    when(repository.optional(nullable(Integer.class))).thenReturn("number or absent");

    assertEquals("string", repository.classify("A-17"));
    assertNull(repository.classify(null));
    assertNull(repository.classify(17));
    assertEquals("number or absent", repository.optional(17));
    assertEquals("number or absent", repository.optional(null));
    assertNull(repository.optional("17"));
  }

  @Test
  public void matchesTheSameInstanceRatherThanAnEqualObject() {
    Repository repository = mock(Repository.class);
    EqualValue wanted = new EqualValue("A-17");

    when(repository.classify(same(wanted))).thenReturn("identical");

    assertEquals("identical", repository.classify(wanted));
    assertNull(repository.classify(new EqualValue("A-17")));
  }

  @Test
  public void matchesStringPrefixesAndSuffixes() {
    Repository repository = mock(Repository.class);

    when(repository.describe(startsWith("account:"))).thenReturn("account");
    when(repository.describe(endsWith(".csv"))).thenReturn("csv");

    assertEquals("account", repository.describe("account:A-17"));
    assertEquals("csv", repository.describe("timesheet.csv"));
    assertNull(repository.describe("timesheet.txt"));
  }

  @Test
  public void reportsAndClearsAnUnfinishedVerification() {
    Repository repository = mock(Repository.class);

    verify(repository);

    IllegalStateException failure = assertThrows(IllegalStateException.class, Mockatcha::validateUsage);
    assertTrue(failure.getMessage(), failure.getMessage().contains("verify(mock)"));
    validateUsage();
  }

  @Test
  public void reportsAndClearsAnUnfinishedStubber() {
    Repository repository = mock(Repository.class);

    doReturn("unused").when(repository);

    IllegalStateException failure = assertThrows(IllegalStateException.class, Mockatcha::validateUsage);
    assertTrue(failure.getMessage(), failure.getMessage().contains("when(mock)"));
    validateUsage();
  }

  @Test
  public void reportsAndClearsUnfinishedLeniency() {
    lenient();

    IllegalStateException failure = assertThrows(IllegalStateException.class, Mockatcha::validateUsage);
    assertTrue(failure.getMessage(), failure.getMessage().contains("lenient()"));
    validateUsage();
  }

  @Test
  public void reportsAMatcherThatWasNeverUsedByAMock() {
    Repository repository = mock(Repository.class);
    NotAMock plain = new NotAMock();

    plain.describe(anyString());

    IllegalStateException failure =
        assertThrows(IllegalStateException.class, () -> verify(repository).describe("anything"));
    assertTrue(failure.getMessage(), failure.getMessage().contains("left over"));
    assertTrue(failure.getMessage(), failure.getMessage().contains("anyString()"));
  }

  @Test
  public void reportsALeftOverMatcherWhenAskedDirectly() {
    NotAMock plain = new NotAMock();

    plain.describe(anyString());

    assertThrows(IllegalStateException.class, Mockatcha::validateUsage);
    validateUsage();
  }

  @Test
  public void reportsALeftOverMatcherBeforeANewMockIsMade() {
    NotAMock plain = new NotAMock();

    plain.describe(anyString());

    assertThrows(IllegalStateException.class, () -> mock(Repository.class));
    validateUsage();
  }

  public interface Repository {

    void save(String report);

    void audit(String note);

    String describe(String note);

    String grade(int score);

    String gradeLong(long score);

    String classify(Object value);

    String optional(Object value);

    void write(byte[] bytes);
  }

  public static final class NotAMock {

    public String describe(String note) {
      return note;
    }
  }

  public static final class EqualValue {

    private final String value;

    EqualValue(String value) {
      this.value = value;
    }

    @Override
    public boolean equals(Object other) {
      return other instanceof EqualValue && value.equals(((EqualValue) other).value);
    }

    @Override
    public int hashCode() {
      return value.hashCode();
    }
  }
}
