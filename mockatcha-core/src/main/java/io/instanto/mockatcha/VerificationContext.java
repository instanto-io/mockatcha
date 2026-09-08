/*
 * Copyright 2026 Carl Stainton
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.instanto.mockatcha;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/** What a {@link VerificationMode} is told about the call it is checking. */
public final class VerificationContext {

  private final int matchingCount;
  private final int totalCount;
  private final String description;
  private final List<Invocation> recorded;

  public VerificationContext(int matchingCount, int totalCount, String description) {
    this(matchingCount, totalCount, description, Collections.emptyList());
  }

  public VerificationContext(
      int matchingCount, int totalCount, String description, List<Invocation> recorded) {
    this.matchingCount = matchingCount;
    this.totalCount = totalCount;
    this.description = Objects.requireNonNull(description, "description");
    this.recorded = Objects.requireNonNull(recorded, "recorded");
  }

  /** How many recorded calls match the call being verified. */
  public int matchingCount() {
    return matchingCount;
  }

  /** How many calls were recorded on the mock in total, matching or not. */
  public int totalCount() {
    return totalCount;
  }

  /** The call being verified, as it appears in a failure message. */
  public String description() {
    return description;
  }

  /** The calls a failure message should list, oldest first. */
  public List<Invocation> recorded() {
    return recorded;
  }
}
