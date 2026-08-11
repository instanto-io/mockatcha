package io.instanto.mockatcha.internal;

import io.instanto.mockatcha.Invocation;
import java.util.List;

/** Builds the part of a failure message that shows what actually happened. */
final class Failures {

  private Failures() {}

  static String listOf(List<Invocation> invocations) {
    if (invocations.isEmpty()) {
      return "\nNo calls were recorded on this mock.";
    }
    StringBuilder message = new StringBuilder("\nCalls recorded on this mock:");
    for (Invocation invocation : invocations) {
      message.append("\n  ").append(invocation);
    }
    return message.toString();
  }
}
