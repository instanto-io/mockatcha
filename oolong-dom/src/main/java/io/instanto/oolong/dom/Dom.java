package io.instanto.oolong.dom;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.teavm.jso.dom.html.HTMLDocument;
import org.teavm.jso.dom.html.HTMLElement;
import org.teavm.jso.dom.xml.Element;
import org.teavm.jso.dom.xml.NodeList;

/**
 * A container for one test to render into, with queries and events scoped to it.
 *
 * <p>Queries search the container rather than the document, so markup left behind by another test
 * cannot be found. {@link #reset()} removes the container; {@link DomRule} and {@link DomTest} call
 * it after every test.
 */
public final class Dom {

  private static HTMLElement container;

  private Dom() {}

  /** The container for this test, created on first use and attached to the document. */
  public static HTMLElement container() {
    if (container == null) {
      HTMLDocument document = HTMLDocument.current();
      container = document.createElement("div");
      container.setAttribute("data-oolong-container", "");
      document.getBody().appendChild(container);
    }
    return container;
  }

  /** Replaces the container's contents with this markup. */
  public static HTMLElement render(String markup) {
    Browser.setInnerHtml(container(), Objects.requireNonNull(markup, "markup"));
    return container();
  }

  /** Removes the container and everything in it. */
  public static void reset() {
    if (container != null && container.getParentNode() != null) {
      container.getParentNode().removeChild(container);
    }
    container = null;
  }

  /** The one element matching a CSS selector, failing with the container's markup when there is none. */
  public static HTMLElement find(String selector) {
    HTMLElement found = findOrNull(selector);
    if (found == null) {
      throw new AssertionError(
          "No element matching \"" + selector + "\"." + markup());
    }
    return found;
  }

  /** The one element matching a CSS selector, or null. */
  public static HTMLElement findOrNull(String selector) {
    Objects.requireNonNull(selector, "selector");
    Element found = container().querySelector(selector);
    return found == null ? null : (HTMLElement) found;
  }

  /** Every element matching a CSS selector, in document order. */
  public static List<HTMLElement> findAll(String selector) {
    Objects.requireNonNull(selector, "selector");
    NodeList<? extends Element> found = container().querySelectorAll(selector);
    List<HTMLElement> elements = new ArrayList<>(found.getLength());
    for (int index = 0; index < found.getLength(); index++) {
      elements.add((HTMLElement) found.get(index));
    }
    return elements;
  }

  /** The first element whose own text is exactly this, ignoring surrounding whitespace. */
  public static HTMLElement findByText(String text) {
    Objects.requireNonNull(text, "text");
    for (HTMLElement candidate : findAll("*")) {
      String content = candidate.getTextContent();
      if (content != null && content.trim().equals(text)) {
        return candidate;
      }
    }
    throw new AssertionError("No element with the text \"" + text + "\"." + markup());
  }

  public static void click(HTMLElement element) {
    Objects.requireNonNull(element, "element").click();
  }

  /** Sets a field's value and reports it the way a browser does while someone types. */
  public static void type(HTMLElement element, String text) {
    Objects.requireNonNull(element, "element");
    Browser.setValue(element, Objects.requireNonNull(text, "text"));
    Browser.dispatch(element, "input");
    Browser.dispatch(element, "change");
  }

  /** Presses a key, using the names the browser uses, such as {@code Enter} or {@code Escape}. */
  public static void press(HTMLElement element, String key) {
    Objects.requireNonNull(element, "element");
    Objects.requireNonNull(key, "key");
    Browser.dispatchKey(element, "keydown", key);
    Browser.dispatchKey(element, "keyup", key);
  }

  /** Dispatches a bubbling event of this type. */
  public static void fire(HTMLElement element, String type) {
    Objects.requireNonNull(element, "element");
    Browser.dispatch(element, Objects.requireNonNull(type, "type"));
  }

  static String markup() {
    if (container == null) {
      return "\nNothing has been rendered.";
    }
    return "\nThe container holds:\n" + Browser.innerHtml(container);
  }
}
