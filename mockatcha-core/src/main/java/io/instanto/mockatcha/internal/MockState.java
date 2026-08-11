package io.instanto.mockatcha.internal;

import io.instanto.mockatcha.Invocation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Runtime state captured by a generated mock implementation. */
public final class MockState {

  private final String description;
  private Object mock;
  private final List<Invocation> invocations = new ArrayList<>();
  private final List<Stub> stubs = new ArrayList<>();

  MockState(String description) {
    this.description = description;
  }

  void attach(Object mock) {
    this.mock = mock;
  }

  Object mock() {
    return mock;
  }

  String description() {
    return description;
  }

  void record(Invocation invocation) {
    invocations.add(invocation);
  }

  void remove(Invocation invocation) {
    invocations.remove(invocation);
  }

  List<Invocation> invocations() {
    return Collections.unmodifiableList(new ArrayList<>(invocations));
  }

  /** How many calls were recorded on this mock. */
  int invocationCount() {
    return invocations.size();
  }

  int count(InvocationPattern pattern) {
    int count = 0;
    for (var invocation : invocations) {
      Object[] arguments = invocation.arguments().toArray();
      if (pattern.matches(invocation.method(), arguments)) {
        count++;
      }
    }
    return count;
  }

  Stub addStub(InvocationPattern pattern) {
    Stub stub = new Stub(pattern);
    stubs.add(stub);
    return stub;
  }

  Stub findStub(String method, Object[] arguments) {
    for (int index = stubs.size() - 1; index >= 0; index--) {
      Stub stub = stubs.get(index);
      if (stub.pattern().matches(method, arguments)) {
        return stub;
      }
    }
    return null;
  }

  /** Drops every stub arranged for this method name. */
  void removeStubs(String methodName) {
    stubs.removeIf(stub -> stub.pattern().methodName().equals(methodName));
  }

  void clearInvocations() {
    invocations.clear();
  }

  void clearInvocations(String methodName) {
    invocations.removeIf(invocation -> invocation.methodName().equals(methodName));
  }

  void reset() {
    invocations.clear();
    stubs.clear();
  }
}
