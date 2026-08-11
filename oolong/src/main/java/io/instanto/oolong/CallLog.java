package io.instanto.oolong;

import io.instanto.mockatcha.Invocation;
import java.util.List;

/** The calls recorded for a mock, or for one method of it. */
public final class CallLog {

  private final List<Invocation> invocations;
  private final String description;

  CallLog(List<Invocation> invocations, String description) {
    this.invocations = invocations;
    this.description = description;
  }

  /** How many matching calls were recorded. */
  public int count() {
    return invocations.size();
  }

  /** Whether any matching call was recorded. */
  public boolean any() {
    return !invocations.isEmpty();
  }

  /** The arguments of one matching call, counting from zero. */
  public List<Object> argsFor(int index) {
    return at(index).arguments();
  }

  /** The first matching call. */
  public Invocation first() {
    return at(0);
  }

  /** The most recent matching call. */
  public Invocation mostRecent() {
    return at(invocations.size() - 1);
  }

  /** Every matching call, oldest first. */
  public List<Invocation> all() {
    return invocations;
  }

  /** One matching call, counting from zero. */
  public Invocation at(int index) {
    if (index < 0 || index >= invocations.size()) {
      throw new IndexOutOfBoundsException(
          "Wanted call " + index + " of " + description + " but only " + invocations.size()
              + " were recorded");
    }
    return invocations.get(index);
  }

  @Override
  public String toString() {
    return description + " " + invocations;
  }
}
