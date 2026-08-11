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
import static io.instanto.mockatcha.ArgumentMatchers.anyString;
import static io.instanto.mockatcha.ArgumentMatchers.eq;
import static io.instanto.mockatcha.Mockatcha.inOrder;
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
import org.teavm.junit.SkipJVM;
import org.teavm.junit.TeaVMTestRunner;

/** Accounting for every call, comparing and combining matchers, and reporting misuse. */
@RunWith(TeaVMTestRunner.class)
@SkipJVM
public class InteractionsAndMatchersTeaVmTest {

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
    assertNull(repository.describe("timesheet"));
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

    void write(byte[] bytes);
  }

  public static final class NotAMock {

    public String describe(String note) {
      return note;
    }
  }
}
