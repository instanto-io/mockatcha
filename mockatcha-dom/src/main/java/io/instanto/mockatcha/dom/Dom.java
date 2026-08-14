package io.instanto.mockatcha.dom;

import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import org.teavm.jso.core.JSPromise;
import org.teavm.jso.dom.html.HTMLDocument;
import org.teavm.jso.dom.html.HTMLElement;

/** An isolated browser-test container with scoped queries, interactions, and waiting. */
public final class Dom {

  private static final int DEFAULT_WAIT_MILLISECONDS = 1_000;
  private static final int WAIT_INTERVAL_MILLISECONDS = 10;

  private static HTMLElement container;

  private Dom() {}

  /** The container for this test, created on first use and attached to the document. */
  public static HTMLElement container() {
    if (container == null) {
      HTMLDocument document = HTMLDocument.current();
      container = document.createElement("div");
      container.setAttribute("data-bdd-container", "");
      document.getBody().appendChild(container);
    }
    return container;
  }

  /** All queries scoped to the test container. */
  public static DomScope page() {
    return new DomScope(container());
  }

  /** Restricts queries to descendants of this element. */
  public static DomScope within(HTMLElement element) {
    return page().within(element);
  }

  /** Convenience for replacing the test container's contents with a small markup fixture. */
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

  public static HTMLElement find(String selector) {
    return page().find(selector);
  }

  public static HTMLElement findOrNull(String selector) {
    return page().findOrNull(selector);
  }

  public static List<HTMLElement> findAll(String selector) {
    return page().findAll(selector);
  }

  public static HTMLElement findByText(String text) {
    return page().findByText(text);
  }

  public static HTMLElement findByText(TextMatch text) {
    return page().findByText(text);
  }

  public static HTMLElement findByTextOrNull(String text) {
    return page().findByTextOrNull(text);
  }

  public static HTMLElement findByTextOrNull(TextMatch text) {
    return page().findByTextOrNull(text);
  }

  public static List<HTMLElement> findAllByText(String text) {
    return page().findAllByText(text);
  }

  public static List<HTMLElement> findAllByText(TextMatch text) {
    return page().findAllByText(text);
  }

  public static HTMLElement awaitByText(String text) {
    return page().awaitByText(text);
  }

  public static HTMLElement awaitByText(TextMatch text) {
    return page().awaitByText(text);
  }

  public static HTMLElement findByRole(String role) {
    return page().findByRole(role);
  }

  public static HTMLElement findByRole(String role, String accessibleName) {
    return page().findByRole(role, accessibleName);
  }

  public static HTMLElement findByRole(RoleQuery query) {
    return page().findByRole(query);
  }

  public static HTMLElement findByRoleOrNull(String role) {
    return page().findByRoleOrNull(role);
  }

  public static HTMLElement findByRoleOrNull(String role, String accessibleName) {
    return page().findByRoleOrNull(role, accessibleName);
  }

  public static HTMLElement findByRoleOrNull(RoleQuery query) {
    return page().findByRoleOrNull(query);
  }

  public static List<HTMLElement> findAllByRole(String role) {
    return page().findAllByRole(role);
  }

  public static List<HTMLElement> findAllByRole(String role, String accessibleName) {
    return page().findAllByRole(role, accessibleName);
  }

  public static List<HTMLElement> findAllByRole(RoleQuery query) {
    return page().findAllByRole(query);
  }

  public static HTMLElement awaitByRole(String role) {
    return page().awaitByRole(role);
  }

  public static HTMLElement awaitByRole(String role, String accessibleName) {
    return page().awaitByRole(role, accessibleName);
  }

  public static HTMLElement awaitByRole(RoleQuery query) {
    return page().awaitByRole(query);
  }

  public static HTMLElement findByLabelText(String text) {
    return page().findByLabelText(text);
  }

  public static HTMLElement findByLabelText(TextMatch text) {
    return page().findByLabelText(text);
  }

  public static HTMLElement findByLabelTextOrNull(String text) {
    return page().findByLabelTextOrNull(text);
  }

  public static HTMLElement findByLabelTextOrNull(TextMatch text) {
    return page().findByLabelTextOrNull(text);
  }

  public static List<HTMLElement> findAllByLabelText(String text) {
    return page().findAllByLabelText(text);
  }

  public static List<HTMLElement> findAllByLabelText(TextMatch text) {
    return page().findAllByLabelText(text);
  }

  public static HTMLElement awaitByLabelText(String text) {
    return page().awaitByLabelText(text);
  }

  public static HTMLElement awaitByLabelText(TextMatch text) {
    return page().awaitByLabelText(text);
  }

  public static HTMLElement findByPlaceholderText(String text) {
    return page().findByPlaceholderText(text);
  }

  public static HTMLElement findByPlaceholderText(TextMatch text) {
    return page().findByPlaceholderText(text);
  }

  public static HTMLElement findByPlaceholderTextOrNull(String text) {
    return page().findByPlaceholderTextOrNull(text);
  }

  public static HTMLElement findByPlaceholderTextOrNull(TextMatch text) {
    return page().findByPlaceholderTextOrNull(text);
  }

  public static List<HTMLElement> findAllByPlaceholderText(String text) {
    return page().findAllByPlaceholderText(text);
  }

  public static List<HTMLElement> findAllByPlaceholderText(TextMatch text) {
    return page().findAllByPlaceholderText(text);
  }

  public static HTMLElement awaitByPlaceholderText(String text) {
    return page().awaitByPlaceholderText(text);
  }

  public static HTMLElement awaitByPlaceholderText(TextMatch text) {
    return page().awaitByPlaceholderText(text);
  }

  public static HTMLElement findByDisplayValue(String value) {
    return page().findByDisplayValue(value);
  }

  public static HTMLElement findByDisplayValue(TextMatch value) {
    return page().findByDisplayValue(value);
  }

  public static HTMLElement findByDisplayValueOrNull(String value) {
    return page().findByDisplayValueOrNull(value);
  }

  public static HTMLElement findByDisplayValueOrNull(TextMatch value) {
    return page().findByDisplayValueOrNull(value);
  }

  public static List<HTMLElement> findAllByDisplayValue(String value) {
    return page().findAllByDisplayValue(value);
  }

  public static List<HTMLElement> findAllByDisplayValue(TextMatch value) {
    return page().findAllByDisplayValue(value);
  }

  public static HTMLElement awaitByDisplayValue(String value) {
    return page().awaitByDisplayValue(value);
  }

  public static HTMLElement awaitByDisplayValue(TextMatch value) {
    return page().awaitByDisplayValue(value);
  }

  public static HTMLElement findByAltText(String text) {
    return page().findByAltText(text);
  }

  public static HTMLElement findByAltText(TextMatch text) {
    return page().findByAltText(text);
  }

  public static HTMLElement findByAltTextOrNull(String text) {
    return page().findByAltTextOrNull(text);
  }

  public static HTMLElement findByAltTextOrNull(TextMatch text) {
    return page().findByAltTextOrNull(text);
  }

  public static List<HTMLElement> findAllByAltText(String text) {
    return page().findAllByAltText(text);
  }

  public static List<HTMLElement> findAllByAltText(TextMatch text) {
    return page().findAllByAltText(text);
  }

  public static HTMLElement awaitByAltText(String text) {
    return page().awaitByAltText(text);
  }

  public static HTMLElement awaitByAltText(TextMatch text) {
    return page().awaitByAltText(text);
  }

  public static HTMLElement findByTitle(String text) {
    return page().findByTitle(text);
  }

  public static HTMLElement findByTitle(TextMatch text) {
    return page().findByTitle(text);
  }

  public static HTMLElement findByTitleOrNull(String text) {
    return page().findByTitleOrNull(text);
  }

  public static HTMLElement findByTitleOrNull(TextMatch text) {
    return page().findByTitleOrNull(text);
  }

  public static List<HTMLElement> findAllByTitle(String text) {
    return page().findAllByTitle(text);
  }

  public static List<HTMLElement> findAllByTitle(TextMatch text) {
    return page().findAllByTitle(text);
  }

  public static HTMLElement awaitByTitle(String text) {
    return page().awaitByTitle(text);
  }

  public static HTMLElement awaitByTitle(TextMatch text) {
    return page().awaitByTitle(text);
  }

  public static HTMLElement findByTestId(String testId) {
    return page().findByTestId(testId);
  }

  public static HTMLElement findByTestIdOrNull(String testId) {
    return page().findByTestIdOrNull(testId);
  }

  public static List<HTMLElement> findAllByTestId(String testId) {
    return page().findAllByTestId(testId);
  }

  public static HTMLElement awaitByTestId(String testId) {
    return page().awaitByTestId(testId);
  }

  /** Clicks an element. Wrap this call in a framework-specific flush boundary when required. */
  public static HTMLElement click(HTMLElement element) {
    Objects.requireNonNull(element, "element").click();
    return element;
  }

  public static HTMLElement click(String selector) {
    return click(find(selector));
  }

  /** Replaces a field value one character at a time using keyboard and input events. */
  public static HTMLElement type(HTMLElement element, String text) {
    Objects.requireNonNull(element, "element");
    Objects.requireNonNull(text, "text");
    requireValueControl(element, "type");
    Browser.focus(element);
    boolean hadValue = !Browser.value(element).isEmpty();
    Browser.setNativeValue(element, "");
    if (hadValue) Browser.dispatch(element, "input");
    for (int index = 0; index < text.length(); ) {
      int codePoint = text.codePointAt(index);
      String character = new String(Character.toChars(codePoint));
      index += Character.charCount(codePoint);
      if (Browser.beginTypingCharacter(element, character)) {
        Browser.setNativeValue(element, Browser.value(element) + character);
        Browser.finishTypingCharacter(element, character);
      }
    }
    Browser.dispatch(element, "change");
    return element;
  }

  /** Clears a field and dispatches input and change events. */
  public static HTMLElement clear(HTMLElement element) {
    Objects.requireNonNull(element, "element");
    requireValueControl(element, "clear");
    Browser.focus(element);
    Browser.setNativeValue(element, "");
    Browser.dispatch(element, "input");
    Browser.dispatch(element, "change");
    return element;
  }

  public static HTMLElement clear(String selector) {
    return clear(find(selector));
  }

  /** Checks a checkbox or radio input if it is not already checked. */
  public static HTMLElement check(HTMLElement element) {
    requireCheckable(element, "check");
    if (!Browser.isChecked(element)) element.click();
    return element;
  }

  public static HTMLElement check(String selector) {
    return check(find(selector));
  }

  /** Unchecks a checkbox if it is checked. */
  public static HTMLElement uncheck(HTMLElement element) {
    requireCheckable(element, "uncheck");
    if ("radio".equals(Browser.inputType(element))) {
      throw new IllegalArgumentException("uncheck() does not accept radio inputs");
    }
    if (Browser.isChecked(element)) element.click();
    return element;
  }

  public static HTMLElement uncheck(String selector) {
    return uncheck(find(selector));
  }

  public static HTMLElement focus(HTMLElement element) {
    Objects.requireNonNull(element, "element");
    Browser.focus(element);
    return element;
  }

  public static HTMLElement focus(String selector) {
    return focus(find(selector));
  }

  public static HTMLElement blur(HTMLElement element) {
    Objects.requireNonNull(element, "element");
    Browser.blur(element);
    return element;
  }

  public static HTMLElement blur(String selector) {
    return blur(find(selector));
  }

  /** Moves the pointer over an element using the usual mouse event sequence. */
  public static HTMLElement hover(HTMLElement element) {
    Objects.requireNonNull(element, "element");
    Browser.dispatchMouse(element, "mouseover");
    Browser.dispatchMouse(element, "mouseenter");
    Browser.dispatchMouse(element, "mousemove");
    return element;
  }

  public static HTMLElement hover(String selector) {
    return hover(find(selector));
  }

  /** Moves the pointer away from an element. */
  public static HTMLElement unhover(HTMLElement element) {
    Objects.requireNonNull(element, "element");
    Browser.dispatchMouse(element, "mouseout");
    Browser.dispatchMouse(element, "mouseleave");
    return element;
  }

  public static HTMLElement unhover(String selector) {
    return unhover(find(selector));
  }

  public static HTMLElement select(HTMLElement element, String value) {
    return selectOptions(element, value);
  }

  public static HTMLElement select(String selector, String value) {
    return select(find(selector), value);
  }

  /** Selects one or more options by value and dispatches input and change events. */
  public static HTMLElement selectOptions(HTMLElement element, String... values) {
    Objects.requireNonNull(element, "element");
    Objects.requireNonNull(values, "values");
    int result = Browser.selectOptions(element, values);
    if (result == 0) throw new IllegalArgumentException("selectOptions() requires a <select> element");
    if (result == 1) {
      throw new AssertionError("Not every requested option exists." + markup());
    }
    if (result == 3) {
      throw new IllegalArgumentException(
          "selectOptions() cannot select several values in a single-select element");
    }
    Browser.dispatch(element, "input");
    Browser.dispatch(element, "change");
    return element;
  }

  public static HTMLElement selectOptions(String selector, String... values) {
    return selectOptions(find(selector), values);
  }

  /** Moves focus to the next focusable element in the test container. */
  public static HTMLElement tab() {
    return Browser.tab(container(), false);
  }

  /** Moves focus to the previous focusable element in the test container. */
  public static HTMLElement tabBack() {
    return Browser.tab(container(), true);
  }

  public static HTMLElement press(HTMLElement element, String key) {
    Objects.requireNonNull(element, "element");
    Objects.requireNonNull(key, "key");
    Browser.dispatchKey(element, "keydown", key);
    Browser.dispatchKey(element, "keyup", key);
    return element;
  }

  public static HTMLElement fire(HTMLElement element, String type) {
    Objects.requireNonNull(element, "element");
    Browser.dispatch(element, Objects.requireNonNull(type, "type"));
    return element;
  }

  /** Retries an assertion until it passes or one second elapses. */
  public static void waitFor(Runnable assertion) {
    waitFor(assertion, DEFAULT_WAIT_MILLISECONDS);
  }

  /** Retries an assertion until it passes or this timeout elapses. */
  public static void waitFor(Runnable assertion, int timeoutMilliseconds) {
    Objects.requireNonNull(assertion, "assertion");
    requireTimeout(timeoutMilliseconds);
    Throwable[] terminal = new Throwable[1];
    boolean succeeded = new JSPromise<Boolean>((resolve, reject) ->
        attempt(assertion, timeoutMilliseconds, terminal, resolve)).await();
    if (succeeded) return;
    if (terminal[0] instanceof RuntimeException runtime) throw runtime;
    if (terminal[0] instanceof Error error) throw error;
    assertion.run();
  }

  /** Retries a query until it returns without an assertion failure. */
  public static <T> T waitFor(Supplier<T> query) {
    return waitFor(query, DEFAULT_WAIT_MILLISECONDS);
  }

  /** Retries a query until it returns or this timeout elapses. */
  public static <T> T waitFor(Supplier<T> query, int timeoutMilliseconds) {
    Objects.requireNonNull(query, "query");
    Holder<T> result = new Holder<>();
    Runnable attempt = () -> result.value = query.get();
    waitFor(attempt, timeoutMilliseconds);
    return result.value;
  }

  static String markup() {
    if (container == null) return "\nNothing has been rendered.";
    return "\nThe container holds:\n" + Browser.innerHtml(container);
  }

  private static void attempt(
      Runnable assertion,
      int remaining,
      Throwable[] terminal,
      org.teavm.jso.function.JSConsumer<Boolean> resolve) {
    try {
      assertion.run();
      resolve.accept(true);
    } catch (AssertionError failure) {
      if (remaining <= 0) {
        resolve.accept(false);
      } else {
        int delay = Math.min(WAIT_INTERVAL_MILLISECONDS, remaining);
        Browser.setPollingTimeout(
            () -> attempt(assertion, remaining - delay, terminal, resolve), delay);
      }
    } catch (RuntimeException | Error failure) {
      terminal[0] = failure;
      resolve.accept(false);
    }
  }

  private static void requireTimeout(int timeoutMilliseconds) {
    if (timeoutMilliseconds < 0) {
      throw new IllegalArgumentException("timeoutMilliseconds must not be negative");
    }
  }

  private static void requireValueControl(HTMLElement element, String operation) {
    if (Browser.value(element) == null) {
      throw new IllegalArgumentException(operation + "() requires an input, textarea, or select");
    }
  }

  private static void requireCheckable(HTMLElement element, String operation) {
    Objects.requireNonNull(element, "element");
    String type = Browser.inputType(element);
    if (!"checkbox".equals(type) && !"radio".equals(type)) {
      throw new IllegalArgumentException(operation + "() requires a checkbox or radio input");
    }
  }

  private static final class Holder<T> {
    private T value;
  }
}
