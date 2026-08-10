package io.instanto.mockatcha;

import io.instanto.mockatcha.internal.MockRuntime;
import java.util.List;

/** Read-only call history for diagnostics and specialised assertions. */
public final class MockingDetails {

  private final Object mock;

  MockingDetails(Object mock) {
    this.mock = mock;
  }

  public boolean isMock() {
    return MockRuntime.isMock(mock);
  }

  public List<Invocation> getInvocations() {
    return MockRuntime.invocations(mock);
  }
}
