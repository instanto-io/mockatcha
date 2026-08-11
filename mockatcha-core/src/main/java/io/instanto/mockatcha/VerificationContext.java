package io.instanto.mockatcha;

import java.util.Objects;

/**
 * What a {@link VerificationMode} is told about the call it is checking.
 *
 * <p>Most modes only need the matching count. {@code only()} also needs to know whether anything
 * else was called on the mock, which is why this exists rather than a bare integer.
 */
public final class VerificationContext {

  private final int matchingCount;
  private final int totalCount;
  private final String description;

  public VerificationContext(int matchingCount, int totalCount, String description) {
    this.matchingCount = matchingCount;
    this.totalCount = totalCount;
    this.description = Objects.requireNonNull(description, "description");
  }

  /** How many recorded calls match the call being verified. */
  public int matchingCount() {
    return matchingCount;
  }

  /** How many calls were recorded on the mock in total, matching or not. */
  public int totalCount() {
    return totalCount;
  }

  /** The call being verified, as it should appear in a failure message. */
  public String description() {
    return description;
  }
}
