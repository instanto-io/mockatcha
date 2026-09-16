/*
 * Copyright 2026 Carl Stainton
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.instanto.mockatcha;

import static io.instanto.mockatcha.Mockatcha.mock;
import static io.instanto.mockatcha.Mockatcha.spy;
import static io.instanto.mockatcha.Mockatcha.verify;
import static io.instanto.mockatcha.Mockatcha.when;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Proxy;
import org.junit.Test;

/** What mocking does on the JVM, where mocks are proxies rather than generated classes. */
public class JvmMocksTest extends MockatchaTestLifecycle {

  interface Store {
    String find(String reference);

    int count();

    void clear();
  }

  static class RealStore implements Store {
    @Override
    public String find(String reference) {
      return "real:" + reference;
    }

    @Override
    public int count() {
      return 7;
    }

    @Override
    public void clear() {}
  }

  @Test
  public void aMockIsAProxyThatStubsAndVerifies() {
    Store store = mock(Store.class);
    assertTrue(Proxy.isProxyClass(store.getClass()));

    when(store.find("A-17")).thenReturn("timesheet");

    assertEquals("timesheet", store.find("A-17"));
    verify(store).find("A-17");
  }

  @Test
  public void unstubbedCallsReturnTheTypesDefault() {
    Store store = mock(Store.class);

    assertEquals(null, store.find("absent"));
    assertEquals(0, store.count());
    store.clear();

    verify(store).clear();
  }

  @Test
  public void aSpyRunsTheRealMethodUntilItIsStubbed() {
    Store spy = spy(Store.class, new RealStore());

    assertEquals("real:A-17", spy.find("A-17"));
    when(spy.find("A-17")).thenReturn("stubbed");

    assertEquals("stubbed", spy.find("A-17"));
    assertEquals(7, spy.count());
  }

  @Test
  public void identityAndDescriptionDoNotReachTheStubs() {
    Store store = mock(Store.class, "store");
    Store other = mock(Store.class);

    assertEquals(store, store);
    assertNotEquals(store, other);
    assertEquals(store.hashCode(), store.hashCode());
    assertEquals(
        "store (Mockatcha mock of io.instanto.mockatcha.JvmMocksTest$Store)", store.toString());

    verify(store, Mockatcha.never()).find(ArgumentMatchers.any());
  }

  @Test
  public void aClassIsMockedWithAGeneratedSubclass() {
    RealStore store = mock(RealStore.class);

    assertTrue(store instanceof RealStore);
    assertEquals("an unstubbed call does not reach the real method", 0, store.count());

    when(store.find("A-17")).thenReturn("timesheet");

    assertEquals("timesheet", store.find("A-17"));
    verify(store).find("A-17");
  }

  @Test
  public void aTypeThatCannotBeSubclassedSaysWhy() {
    UnsupportedOperationException rejected =
        assertThrows(UnsupportedOperationException.class, () -> mock(FinalStore.class));

    assertTrue(rejected.getMessage(), rejected.getMessage().contains("cannot mock this type"));
    assertTrue(rejected.getMessage(), rejected.getMessage().contains("is final"));
  }

  static final class FinalStore {
    String find(String reference) {
      return reference;
    }
  }
}
