/*
 * Copyright 2026 Carl Stainton
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.instanto.mockatcha;

import static io.instanto.mockatcha.AdditionalAnswers.answer;
import static io.instanto.mockatcha.AdditionalAnswers.returnsArgAt;
import static io.instanto.mockatcha.AdditionalAnswers.returnsElementsOf;
import static io.instanto.mockatcha.AdditionalAnswers.returnsFirstArg;
import static io.instanto.mockatcha.AdditionalAnswers.returnsLastArg;
import static io.instanto.mockatcha.AdditionalAnswers.returnsSecondArg;
import static io.instanto.mockatcha.ArgumentMatchers.any;
import static io.instanto.mockatcha.ArgumentMatchers.anyInt;
import static io.instanto.mockatcha.ArgumentMatchers.anyString;
import static io.instanto.mockatcha.BDDMockatcha.given;
import static io.instanto.mockatcha.BDDMockatcha.then;
import static io.instanto.mockatcha.BDDMockatcha.willReturn;
import static io.instanto.mockatcha.BDDMockatcha.willThrow;
import static io.instanto.mockatcha.Mockatcha.inOrder;
import static io.instanto.mockatcha.Mockatcha.mock;
import static io.instanto.mockatcha.Mockatcha.spy;
import static io.instanto.mockatcha.Mockatcha.times;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.junit.SkipJVM;
import org.teavm.junit.TeaVMTestRunner;

/** Answers built from the call, and the given-when-then spelling of the same API. */
@RunWith(TeaVMTestRunner.class)
@SkipJVM
public class AnswersAndBddTeaVmTest {

  @Test
  public void returnsAnArgumentBackToTheCaller() {
    Store store = mock(Store.class);

    Mockatcha.when(store.save(anyString())).thenAnswer(returnsFirstArg());

    assertEquals("A-17", store.save("A-17"));
  }

  @Test
  public void returnsAnyOfTheArguments() {
    Store store = mock(Store.class);

    Mockatcha.when(store.combine(anyString(), anyString(), anyString()))
        .thenAnswer(returnsSecondArg());

    assertEquals("b", store.combine("a", "b", "c"));

    Mockatcha.reset(store);
    Mockatcha.when(store.combine(anyString(), anyString(), anyString()))
        .thenAnswer(returnsLastArg());
    assertEquals("c", store.combine("a", "b", "c"));

    Mockatcha.reset(store);
    Mockatcha.when(store.combine(anyString(), anyString(), anyString()))
        .thenAnswer(returnsArgAt(0));
    assertEquals("a", store.combine("a", "b", "c"));
  }

  @Test
  public void returnsASequenceOfPreparedValues() {
    Store store = mock(Store.class);

    Mockatcha.when(store.next()).thenAnswer(returnsElementsOf(List.of(1, 2, 3)));

    assertEquals(1, store.next());
    assertEquals(2, store.next());
    assertEquals(3, store.next());
    assertEquals(3, store.next());
  }

  @Test
  public void computesAResultWithoutCastingTheArgument() {
    Store store = mock(Store.class);

    Mockatcha.when(store.save(anyString())).thenAnswer(answer((String id) -> id.toUpperCase()));
    Mockatcha.when(store.label(anyString(), anyInt()))
        .thenAnswer(answer((String id, Integer count) -> id + "/" + count));

    assertEquals("A-17", store.save("a-17"));
    assertEquals("A-17/3", store.label("A-17", 3));
  }

  @Test
  public void rejectsAnArgumentPositionThatIsNotThere() {
    Store store = mock(Store.class);
    Mockatcha.when(store.save(anyString())).thenAnswer(returnsArgAt(4));

    assertThrows(IllegalArgumentException.class, () -> store.save("A-17"));
  }

  @Test
  public void readsAsGivenWhenThen() {
    Store store = mock(Store.class);
    Notifications notifications = mock(Notifications.class);

    given(store.save("A-17")).willReturn("saved");

    String result = store.save("A-17");

    assertEquals("saved", result);
    then(store).should().save("A-17");
    then(notifications).shouldHaveNoInteractions();
  }

  @Test
  public void countsAndOrdersInTheSameSpelling() {
    Store store = mock(Store.class);
    Notifications notifications = mock(Notifications.class);

    store.save("A-17");
    store.save("A-17");
    notifications.send("done");

    then(store).should(times(2)).save("A-17");

    InOrder order = inOrder(store, notifications);
    then(store).should(order, times(2)).save("A-17");
    then(notifications).should(order).send("done");
  }

  @Test
  public void reportsAFailureInTheSameSpelling() {
    Store store = mock(Store.class);

    given(store.save("A-17")).willThrow(new IllegalStateException("offline"));

    assertThrows(IllegalStateException.class, () -> store.save("A-17"));
  }

  @Test
  public void arrangesWithoutCallingInTheSameSpelling() {
    Counter real = new Counter();
    Counter counter = spy(Counter.class, real);

    willReturn(99).given(counter).count();

    assertEquals(99, counter.count());
    assertEquals(0, real.count());
  }

  @Test
  public void arrangesAFailureWithoutCalling() {
    Counter real = new Counter();
    Counter counter = spy(Counter.class, real);

    willThrow(new IllegalStateException("offline")).given(counter).increment();

    assertThrows(IllegalStateException.class, counter::increment);
    assertEquals(0, real.count());
  }

  @Test
  public void accountsForEveryCallInTheSameSpelling() {
    Store store = mock(Store.class);

    store.save("A-17");

    then(store).should().save("A-17");
    then(store).shouldHaveNoMoreInteractions();
  }

  @Test
  public void mixesWithTheMockitoShapedSpelling() {
    Store store = mock(Store.class);

    given(store.save(any())).willAnswer(returnsFirstArg());
    store.save("A-17");

    Mockatcha.verify(store).save("A-17");
    then(store).shouldHaveNoMoreInteractions();
  }

  public interface Store {

    String save(String report);

    String combine(String first, String second, String third);

    String label(String id, int count);

    int next();
  }

  public interface Notifications {

    void send(String message);
  }

  public static class Counter {

    private int count;

    public void increment() {
      count++;
    }

    public int count() {
      return count;
    }
  }
}
