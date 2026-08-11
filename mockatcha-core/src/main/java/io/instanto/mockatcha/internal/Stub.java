package io.instanto.mockatcha.internal;

import io.instanto.mockatcha.Answer;
import io.instanto.mockatcha.Invocation;
import java.util.ArrayList;
import java.util.List;

final class Stub {

  private final InvocationPattern pattern;
  private final List<Answer<?>> answers = new ArrayList<>();
  private int answerIndex;
  private boolean used;
  private boolean lenient;

  Stub(InvocationPattern pattern) {
    this.pattern = pattern;
  }

  void markUsed() {
    used = true;
  }

  boolean isUsed() {
    return used;
  }

  void makeLenient() {
    lenient = true;
  }

  boolean isLenient() {
    return lenient;
  }

  InvocationPattern pattern() {
    return pattern;
  }

  void add(Answer<?> answer) {
    answers.add(answer);
  }

  Object answer(Invocation invocation) {
    if (answers.isEmpty()) {
      return null;
    }
    int index = Math.min(answerIndex, answers.size() - 1);
    if (answerIndex < answers.size() - 1) {
      answerIndex++;
    }
    try {
      return answers.get(index).answer(invocation);
    } catch (Throwable throwable) {
      return MockRuntime.sneakyThrow(throwable);
    }
  }
}
