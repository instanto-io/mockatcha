package io.instanto.mockatcha.bdd.dom;

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

    public ElementExpectation toContainText(String expected) {
      return check(
          text().contains(expected), "text containing \"" + expected + "\"", "\"" + text() + "\"");
    }

    public ElementExpectation toHaveClass(String expected) {
      return check(
          classes().contains(" " + expected + " "),
          "the class \"" + expected + "\"",
          "\"" + Browser.className(element) + "\"");
    }

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

    public ElementExpectation toHaveStyle(String property, String expected) {
      String actual = Browser.style(element, property);
      return check(
          Objects.equals(expected, actual),
          property + " of \"" + expected + "\"",
          "\"" + actual + "\"");
    }

    public ElementExpectation toBeVisible() {
      return check(Browser.isVisible(element), "to be visible", "hidden");
    }

    public ElementExpectation toBeHidden() {
      return check(!Browser.isVisible(element), "to be hidden", "visible");
    }

    public ElementExpectation toBeDisabled() {
      return check(Browser.isDisabled(element), "to be disabled", "enabled");
    }

    public ElementExpectation toBeEnabled() {
      return check(!Browser.isDisabled(element), "to be enabled", "disabled");
    }

    public ElementExpectation toBeChecked() {
      return check(Browser.isChecked(element), "to be checked", "unchecked");
    }

    /** Whether the element is still attached to the document. */
    public ElementExpectation toBeInTheDocument() {
      return check(Browser.isInDocument(element), "to be in the document", "detached");
    }

    public ElementExpectation toHaveFocus() {
      return check(Browser.hasFocus(element), "to have focus", "not focused");
    }

    /** Whether the element holds nothing, ignoring surrounding whitespace. */
    public ElementExpectation toBeEmpty() {
      String markup = Browser.innerHtml(element).trim();
      return check(markup.isEmpty(), "to be empty", "\"" + markup + "\"");
    }

    public ElementExpectation toContainElement(HTMLElement descendant) {
      Objects.requireNonNull(descendant, "descendant");
      return check(
          Browser.contains(element, descendant),
          "to contain " + Browser.outerHtml(descendant),
          "no such descendant");
    }

    public ElementExpectation toContainHtml(String expected) {
      String markup = Browser.innerHtml(element);
      return check(
          markup.contains(expected), "markup containing \"" + expected + "\"", "\"" + markup + "\"");
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
  }
}
