/*
 * Copyright 2026 Carl Stainton
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.instanto.mockatcha.dom;

import java.util.Locale;
import java.util.Objects;

/** Matches text after collapsing whitespace and trimming its ends. */
public final class TextMatch {

  private final String wanted;
  private final boolean containing;
  private final boolean ignoringCase;

  private TextMatch(String wanted, boolean containing, boolean ignoringCase) {
    this.wanted = normalize(Objects.requireNonNull(wanted, "wanted"));
    this.containing = containing;
    this.ignoringCase = ignoringCase;
  }

  /** Matches the complete normalized text. */
  public static TextMatch exact(String text) {
    return new TextMatch(text, false, false);
  }

  /** Matches normalized text that contains this value. */
  public static TextMatch containing(String text) {
    return new TextMatch(text, true, false);
  }

  /** Matches the complete normalized text without regard to case. */
  public static TextMatch exactIgnoringCase(String text) {
    return new TextMatch(text, false, true);
  }

  /** Matches normalized text containing this value without regard to case. */
  public static TextMatch containingIgnoringCase(String text) {
    return new TextMatch(text, true, true);
  }

  boolean matches(String actual) {
    String candidate = normalize(actual);
    String expected = wanted;
    if (ignoringCase) {
      candidate = candidate.toLowerCase(Locale.ROOT);
      expected = expected.toLowerCase(Locale.ROOT);
    }
    return containing ? candidate.contains(expected) : candidate.equals(expected);
  }

  String description() {
    return (containing ? "containing " : "equal to ") + quote(wanted)
        + (ignoringCase ? " ignoring case" : "");
  }

  static String normalize(String text) {
    if (text == null || text.isEmpty()) {
      return "";
    }
    return text.replaceAll("\\s+", " ").trim();
  }

  private static String quote(String text) {
    return "\"" + text + "\"";
  }
}
