package io.instanto.mockatcha.examples;

import java.util.List;

/** Where a sparkline gets its numbers. */
public interface ReadingSource {

  /** The most recent readings, oldest first, each between 0 and 100. */
  List<Integer> recent(int count);
}
