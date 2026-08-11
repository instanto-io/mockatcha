package io.instanto.mockatcha.internal;

import io.instanto.mockatcha.Invocation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Runtime state captured by a generated mock implementation. */
public final class MockState {

  private final String description;
  private Object mock;
  private final List<Invocation> invocations = new ArrayList<>();
  private final List<Stub> stubs = new ArrayList<>();
  private final Set<Integer> verified = new HashSet<>();

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
    return matching(pattern).size();
  }

  List<Invocation> matching(InvocationPattern pattern) {
    List<Invocation> matched = new ArrayList<>();
    for (Invocation invocation : invocations) {
      if (pattern.matches(invocation.method(), invocation.arguments().toArray())) {
        matched.add(invocation);
      }
    }
    return matched;
  }

  void markVerified(List<Invocation> matched) {
    for (Invocation invocation : matched) {
      verified.add(invocation.sequence());
    }
  }

  /** The calls no verification has accounted for, oldest first. */
  List<Invocation> unverified() {
    List<Invocation> remaining = new ArrayList<>();
    for (Invocation invocation : invocations) {
      if (!verified.contains(invocation.sequence())) {
        remaining.add(invocation);
      }
    }
    return remaining;
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
        stub.markUsed();
        return stub;
      }
    }
    return null;
  }

  /** The stubs no call ever matched, leaving out those a test marked lenient. */
  List<Stub> unusedStubs() {
    List<Stub> unused = new ArrayList<>();
    for (Stub stub : stubs) {
      if (!stub.isUsed() && !stub.isLenient()) {
        unused.add(stub);
      }
    }
    return unused;
  }

  /** The stubs arranged for a method name, for explaining why a call found none. */
  List<Stub> stubsOf(String methodName) {
    List<Stub> matching = new ArrayList<>();
    for (Stub stub : stubs) {
      if (stub.pattern().methodName().equals(methodName) && !stub.isLenient()) {
        matching.add(stub);
      }
    }
    return matching;
  }

  /** The calls recorded for a method name, for explaining why a stub went unused. */
  List<Invocation> invocationsOf(String methodName) {
    List<Invocation> matching = new ArrayList<>();
    for (Invocation invocation : invocations) {
      if (invocation.methodName().equals(methodName)) {
        matching.add(invocation);
      }
    }
    return matching;
  }

  /** Drops every stub arranged for this method name. */
  void removeStubs(String methodName) {
    stubs.removeIf(stub -> stub.pattern().methodName().equals(methodName));
  }

  void clearInvocations() {
    invocations.clear();
    verified.clear();
  }

  void clearInvocations(String methodName) {
    invocations.removeIf(
        invocation -> {
          if (!invocation.methodName().equals(methodName)) {
            return false;
          }
          verified.remove(invocation.sequence());
          return true;
        });
  }

  void reset() {
    invocations.clear();
    verified.clear();
    stubs.clear();
  }
}
