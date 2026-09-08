/*
 * Copyright 2026 Carl Stainton
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.instanto.mockatcha;

import static io.instanto.mockatcha.ArgumentMatchers.anyString;
import static io.instanto.mockatcha.Mockatcha.atLeast;
import static io.instanto.mockatcha.Mockatcha.atLeastOnce;
import static io.instanto.mockatcha.Mockatcha.atMost;
import static io.instanto.mockatcha.Mockatcha.inOrder;
import static io.instanto.mockatcha.Mockatcha.mock;
import static io.instanto.mockatcha.Mockatcha.never;
import static io.instanto.mockatcha.Mockatcha.only;
import static io.instanto.mockatcha.Mockatcha.times;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.junit.SkipJVM;
import org.teavm.junit.TeaVMTestRunner;

/** Ordered verification, following Mockito's rules. */
@RunWith(TeaVMTestRunner.class)
@SkipJVM
public class InOrderTeaVmTest {

  @Test
  public void acceptsCallsMadeInTheExpectedOrder() {
    Repository repository = mock(Repository.class);
    Notifications notifications = mock(Notifications.class);

    repository.save("report");
    notifications.send("saved");

    InOrder order = inOrder(repository, notifications);
    order.verify(repository).save("report");
    order.verify(notifications).send("saved");
  }

  @Test
  public void rejectsCallsMadeInTheWrongOrder() {
    Repository repository = mock(Repository.class);
    Notifications notifications = mock(Notifications.class);

    notifications.send("saved");
    repository.save("report");

    InOrder order = inOrder(repository, notifications);
    order.verify(repository).save("report");

    assertThrows(AssertionError.class, () -> order.verify(notifications).send("saved"));
  }

  @Test
  public void allowsOtherCallsInBetween() {
    Repository repository = mock(Repository.class);
    Notifications notifications = mock(Notifications.class);

    repository.save("report");
    repository.audit("something else");
    notifications.send("saved");

    InOrder order = inOrder(repository, notifications);
    order.verify(repository).save("report");
    order.verify(notifications).send("saved");
  }

  @Test
  public void ordersCallsOnOneMock() {
    Repository repository = mock(Repository.class);

    repository.save("first");
    repository.save("second");

    InOrder order = inOrder(repository);
    order.verify(repository).save("first");
    order.verify(repository).save("second");
  }

  @Test
  public void consumesTheCallsItVerifies() {
    Repository repository = mock(Repository.class);

    repository.save("report");

    InOrder order = inOrder(repository);
    order.verify(repository).save("report");

    assertThrows(AssertionError.class, () -> order.verify(repository).save("report"));
  }

  @Test
  public void countsRepeatedCalls() {
    Repository repository = mock(Repository.class);

    repository.save("report");
    repository.save("report");
    repository.audit("done");

    InOrder order = inOrder(repository);
    order.verify(repository, times(2)).save("report");
    order.verify(repository).audit("done");
  }

  @Test
  public void anExactCountLooksAtTheRunOfCallsThatStartsNext() {
    Repository repository = mock(Repository.class);

    repository.save("report");
    repository.audit("interrupting");
    repository.save("report");

    // The run that starts next is one call long, so times(1) is satisfied by it.
    InOrder order = inOrder(repository);
    order.verify(repository, times(1)).save("report");
    order.verify(repository).audit("interrupting");
    order.verify(repository, times(1)).save("report");
  }

  @Test
  public void anExactCountFallsBackToEveryMatchingCall() {
    Repository repository = mock(Repository.class);

    repository.save("report");
    repository.audit("interrupting");
    repository.save("report");

    InOrder order = inOrder(repository);
    order.verify(repository, times(2)).save("report");
  }

  @Test
  public void supportsNeverForACallThatMustNotFollow() {
    Repository repository = mock(Repository.class);
    Notifications notifications = mock(Notifications.class);

    repository.save("report");

    InOrder order = inOrder(repository, notifications);
    order.verify(repository).save("report");
    order.verify(notifications, never()).send(anyString());
  }

  @Test
  public void supportsAtLeast() {
    Repository repository = mock(Repository.class);

    repository.save("report");
    repository.save("report");
    repository.save("report");

    InOrder order = inOrder(repository);
    order.verify(repository, atLeast(2)).save("report");
    assertThrows(AssertionError.class, () -> order.verify(repository, atLeastOnce()).save("report"));
  }

  @Test
  public void worksWithMatchers() {
    Repository repository = mock(Repository.class);

    repository.save("report");
    repository.audit("done");

    InOrder order = inOrder(repository);
    order.verify(repository).save(anyString());
    order.verify(repository).audit(anyString());
  }

  @Test
  public void capturesAnOrderedInvocationOnlyOnce() {
    Repository repository = mock(Repository.class);
    ArgumentCaptor<String> report = ArgumentCaptor.forClass(String.class);
    repository.save("report");

    inOrder(repository).verify(repository).save(report.capture());

    assertEquals(List.of("report"), report.getAllValues());
  }

  @Test
  public void reportsCallsLeftOverAtTheEnd() {
    Repository repository = mock(Repository.class);

    repository.save("report");
    repository.audit("extra");

    InOrder order = inOrder(repository);
    order.verify(repository).save("report");

    AssertionError failure = assertThrows(AssertionError.class, order::verifyNoMoreInteractions);
    assertTrue(failure.getMessage(), failure.getMessage().contains("audit"));
  }

  @Test
  public void acceptsWhenEverythingHasBeenVerified() {
    Repository repository = mock(Repository.class);

    repository.save("report");

    InOrder order = inOrder(repository);
    order.verify(repository).save("report");
    order.verifyNoMoreInteractions();
  }

  @Test
  public void refusesCountingRulesThatHaveNoOrderedMeaning() {
    Repository repository = mock(Repository.class);
    repository.save("report");
    InOrder order = inOrder(repository);

    assertThrows(
        IllegalArgumentException.class, () -> order.verify(repository, atMost(1)).save("report"));
    assertThrows(
        IllegalArgumentException.class, () -> order.verify(repository, only()).save("report"));
  }

  @Test
  public void refusesMocksItWasNotGiven() {
    Repository repository = mock(Repository.class);
    Notifications notifications = mock(Notifications.class);
    InOrder order = inOrder(repository);

    assertThrows(
        IllegalArgumentException.class, () -> order.verify(notifications).send("saved"));
  }

  @Test
  public void refusesObjectsThatAreNotMocks() {
    assertThrows(IllegalArgumentException.class, () -> inOrder(new Object()));
    assertThrows(IllegalArgumentException.class, Mockatcha::inOrder);
  }

  @Test
  public void leavesOrdinaryVerificationAlone() {
    Repository repository = mock(Repository.class);

    repository.save("report");
    repository.save("report");

    Mockatcha.verify(repository, times(2)).save("report");
    Mockatcha.verify(repository, never()).audit(anyString());
  }

  public interface Repository {

    void save(String report);

    void audit(String note);
  }

  public interface Notifications {

    void send(String message);
  }
}
