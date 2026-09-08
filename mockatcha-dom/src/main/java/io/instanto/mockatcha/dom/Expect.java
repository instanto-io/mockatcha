/*
 * Copyright 2026 Carl Stainton
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.instanto.mockatcha.dom;

import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import org.teavm.jso.dom.html.HTMLElement;

/**
 * Expectations about a rendered element.
 *
 * <pre>{@code
 * expect(find("h1")).toHaveText("Hello Ada");
 * expect(find(".row")).not().toHaveClass("muted");
 * }</pre>
 *
 * <p>A failure prints the element it was given. {@link ElementExpectation#not()} applies to the
 * one expectation that follows it, as it does in Jasmine.
 */
public final class Expect {

  private Expect() {}

  /** Begins a chain of expectations about an element. */
  public static ElementExpectation expect(HTMLElement element) {
    return new ElementExpectation(element, false);
  }

  /** Expectations about one element. */
  public static final class ElementExpectation {

    private final HTMLElement element;
    private final boolean negated;

    private ElementExpectation(HTMLElement element, boolean negated) {
      this.element = Objects.requireNonNull(element, "element");
      this.negated = negated;
    }

    /** Inverts the expectation that follows. */
    public ElementExpectation not() {
      return new ElementExpectation(element, true);
    }

    /** The element's text, ignoring surrounding whitespace. */
    public ElementExpectation toHaveText(String expected) {
      return check(text().equals(expected), "text \"" + expected + "\"", "\"" + text() + "\"");
    }

    /** Expects the element's text to contain this value. */
    public ElementExpectation toContainText(String expected) {
      return check(
          text().contains(expected), "text containing \"" + expected + "\"", "\"" + text() + "\"");
    }

    /** Expects the element to have this CSS class. */
    public ElementExpectation toHaveClass(String expected) {
      return check(
          classes().contains(" " + expected + " "),
          "the class \"" + expected + "\"",
          "\"" + Browser.className(element) + "\"");
    }

    /** Expects an attribute to have this value. */
    public ElementExpectation toHaveAttribute(String name, String expected) {
      String actual = element.getAttribute(name);
      return check(
          Objects.equals(expected, actual), name + "=\"" + expected + "\"", String.valueOf(actual));
    }

    /** The value of a field, as the browser reports it. */
    public ElementExpectation toHaveValue(String expected) {
      String actual = Browser.value(element);
      return check(
          Objects.equals(expected, actual),
          "the value \"" + expected + "\"",
          "\"" + actual + "\"");
    }

    /** Expects the accessible name calculated from labels, ARIA, or element content. */
    public ElementExpectation toHaveAccessibleName(String expected) {
      return toHaveAccessibleName(TextMatch.exact(expected));
    }

    /** Expects the accessible name to satisfy this text matcher. */
    public ElementExpectation toHaveAccessibleName(TextMatch expected) {
      Objects.requireNonNull(expected, "expected");
      String actual = Browser.accessibleName(element);
      return check(
          expected.matches(actual),
          "an accessible name " + expected.description(),
          "\"" + TextMatch.normalize(actual) + "\"");
    }

    /** Expects the element's explicit or implicit accessible role. */
    public ElementExpectation toHaveRole(String expected) {
      String actual = Browser.role(element);
      return check(
          Objects.equals(expected, actual),
          "the role \"" + expected + "\"",
          actual == null ? "no role" : "\"" + actual + "\"");
    }

    /** Expects this computed CSS property value. */
    public ElementExpectation toHaveStyle(String property, String expected) {
      String actual = Browser.style(element, property);
      return check(
          Objects.equals(expected, actual),
          property + " of \"" + expected + "\"",
          "\"" + actual + "\"");
    }

    /** Expects the element to be visible. */
    public ElementExpectation toBeVisible() {
      return check(Browser.isVisible(element), "to be visible", "hidden");
    }

    /** Expects the element to be hidden. */
    public ElementExpectation toBeHidden() {
      return check(!Browser.isVisible(element), "to be hidden", "visible");
    }

    /** Expects the form control to be disabled. */
    public ElementExpectation toBeDisabled() {
      return check(Browser.isDisabled(element), "to be disabled", "enabled");
    }

    /** Expects the form control to be enabled. */
    public ElementExpectation toBeEnabled() {
      return check(!Browser.isDisabled(element), "to be enabled", "disabled");
    }

    /** Expects the checkbox or radio control to be checked. */
    public ElementExpectation toBeChecked() {
      return check(Browser.isChecked(element), "to be checked", "unchecked");
    }

    /** Expects an option or ARIA widget to be selected. */
    public ElementExpectation toBeSelected() {
      return check(Browser.selectedState(element) == 1, "to be selected", "not selected");
    }

    /** Expects a form control to be required through HTML or ARIA. */
    public ElementExpectation toBeRequired() {
      return check(Browser.isRequired(element), "to be required", "not required");
    }

    /** Expects a form control to fail native or ARIA validity. */
    public ElementExpectation toBeInvalid() {
      return check(Browser.isInvalid(element), "to be invalid", "valid");
    }

    /** Expects a form control to pass native and ARIA validity. */
    public ElementExpectation toBeValid() {
      return check(!Browser.isInvalid(element), "to be valid", "invalid");
    }

    /** Expects {@code aria-expanded="true"}. */
    public ElementExpectation toBeExpanded() {
      return check(Browser.expandedState(element) == 1, "to be expanded", "not expanded");
    }

    /** Expects {@code aria-expanded="false"}. */
    public ElementExpectation toBeCollapsed() {
      return check(Browser.expandedState(element) == 0, "to be collapsed", "not collapsed");
    }

    /** Whether the element is still attached to the document. */
    public ElementExpectation toBeInTheDocument() {
      return check(Browser.isInDocument(element), "to be in the document", "detached");
    }

    /** Expects the element to have browser focus. */
    public ElementExpectation toHaveFocus() {
      return check(Browser.hasFocus(element), "to have focus", "not focused");
    }

    /** Whether the element holds nothing, ignoring surrounding whitespace. */
    public ElementExpectation toBeEmpty() {
      String markup = Browser.innerHtml(element).trim();
      return check(markup.isEmpty(), "to be empty", "\"" + markup + "\"");
    }

    /** Expects this element to contain the supplied descendant. */
    public ElementExpectation toContainElement(HTMLElement descendant) {
      Objects.requireNonNull(descendant, "descendant");
      return check(
          Browser.contains(element, descendant),
          "to contain " + Browser.outerHtml(descendant),
          "no such descendant");
    }

    /** Expects the element's markup to contain this fragment. */
    public ElementExpectation toContainHtml(String expected) {
      String markup = Browser.innerHtml(element);
      return check(
          markup.contains(expected), "markup containing \"" + expected + "\"", "\"" + markup + "\"");
    }

    /** Expects named controls inside this form or container to have these values. */
    public ElementExpectation toHaveFormValues(Map<String, ?> expected) {
      Objects.requireNonNull(expected, "expected");
      for (Map.Entry<String, ?> entry : expected.entrySet()) {
        String actual = Browser.formValue(element, entry.getKey());
        String wanted = formValue(entry.getValue());
        if (actual == null) {
          return check(false, "a control named \"" + entry.getKey() + "\"", "no such control");
        }
        if (!Objects.equals(wanted, actual)) {
          return check(
              false,
              "the form value \"" + entry.getKey() + "\"=\"" + displayFormValue(wanted) + "\"",
              "\"" + displayFormValue(actual) + "\"");
        }
      }
      return check(true, "the supplied form values", "the supplied form values");
    }

    private ElementExpectation check(boolean held, String wanted, String actual) {
      if (held == negated) {
        throw new AssertionError(
            "Wanted " + (negated ? "no " : "") + wanted + " but found " + actual
                + ".\nThe element is:\n" + Browser.outerHtml(element));
      }
      return negated ? new ElementExpectation(element, false) : this;
    }

    private String text() {
      String content = element.getTextContent();
      return content == null ? "" : content.trim();
    }

    private String classes() {
      return " " + Browser.className(element) + " ";
    }

    private static String formValue(Object value) {
      if (value instanceof Iterable<?> values) {
        StringBuilder joined = new StringBuilder();
        Iterator<?> iterator = values.iterator();
        while (iterator.hasNext()) {
          if (!joined.isEmpty()) joined.append('\u001f');
          joined.append(String.valueOf(iterator.next()));
        }
        return joined.toString();
      }
      return String.valueOf(value);
    }

    private static String displayFormValue(String value) {
      return value.replace("\u001f", ", ");
    }
  }
}
