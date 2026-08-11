package io.instanto.oolong.dom;

import java.util.Objects;
import org.teavm.jso.dom.html.HTMLElement;

/**
 * Assertions about a rendered element.
 *
 * <pre>{@code
 * assertThat(find("h1")).hasText("Hello Ada");
 * assertThat(find(".row")).hasClass("selected");
 * }</pre>
 *
 * <p>A failure names the element it was given, so it says which one was wrong.
 */
public final class DomAssert {

  private DomAssert() {}

  public static ElementAssert assertThat(HTMLElement element) {
    return new ElementAssert(element);
  }

  /** Assertions about one element. */
  public static final class ElementAssert {

    private final HTMLElement element;

    private ElementAssert(HTMLElement element) {
      this.element = Objects.requireNonNull(element, "element");
    }

    /** The element's text, ignoring surrounding whitespace. */
    public ElementAssert hasText(String expected) {
      String actual = text();
      if (!actual.equals(expected)) {
        throw failure("text \"" + expected + "\"", "\"" + actual + "\"");
      }
      return this;
    }

    public ElementAssert containsText(String expected) {
      String actual = text();
      if (!actual.contains(expected)) {
        throw failure("text containing \"" + expected + "\"", "\"" + actual + "\"");
      }
      return this;
    }

    public ElementAssert hasClass(String expected) {
      if (!classes().contains(" " + expected + " ")) {
        throw failure("the class \"" + expected + "\"", "\"" + Browser.className(element) + "\"");
      }
      return this;
    }

    public ElementAssert doesNotHaveClass(String unwanted) {
      if (classes().contains(" " + unwanted + " ")) {
        throw failure(
            "no class \"" + unwanted + "\"", "\"" + Browser.className(element) + "\"");
      }
      return this;
    }

    public ElementAssert hasAttribute(String name, String expected) {
      String actual = element.getAttribute(name);
      if (!Objects.equals(expected, actual)) {
        throw failure(name + "=\"" + expected + "\"", String.valueOf(actual));
      }
      return this;
    }

    /** The value of a field, as the browser reports it. */
    public ElementAssert hasValue(String expected) {
      String actual = Browser.value(element);
      if (!Objects.equals(expected, actual)) {
        throw failure("the value \"" + expected + "\"", "\"" + actual + "\"");
      }
      return this;
    }

    public ElementAssert isVisible() {
      if (!Browser.isVisible(element)) {
        throw failure("to be visible", "hidden");
      }
      return this;
    }

    public ElementAssert isHidden() {
      if (Browser.isVisible(element)) {
        throw failure("to be hidden", "visible");
      }
      return this;
    }

    public ElementAssert isDisabled() {
      if (!Browser.isDisabled(element)) {
        throw failure("to be disabled", "enabled");
      }
      return this;
    }

    public ElementAssert isEnabled() {
      if (Browser.isDisabled(element)) {
        throw failure("to be enabled", "disabled");
      }
      return this;
    }

    public ElementAssert isChecked() {
      if (!Browser.isChecked(element)) {
        throw failure("to be checked", "unchecked");
      }
      return this;
    }

    private String text() {
      String content = element.getTextContent();
      return content == null ? "" : content.trim();
    }

    private String classes() {
      return " " + Browser.className(element) + " ";
    }

    private AssertionError failure(String wanted, String actual) {
      return new AssertionError(
          "Wanted " + wanted + " but found " + actual + ".\nThe element is:\n"
              + Browser.outerHtml(element));
    }
  }
}
