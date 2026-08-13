package io.instanto.mockatcha.bdd.dom;

import org.teavm.jso.JSBody;
import org.teavm.jso.dom.html.HTMLElement;

/** The few browser operations TeaVM's DOM API does not expose directly. */
final class Browser {

  private Browser() {}

  @JSBody(params = {"element"}, script = "return element.innerHTML;")
  static native String innerHtml(HTMLElement element);

  @JSBody(params = {"element", "html"}, script = "element.innerHTML = html;")
  static native void setInnerHtml(HTMLElement element, String html);

  @JSBody(params = {"element"}, script = "return element.outerHTML;")
  static native String outerHtml(HTMLElement element);

  @JSBody(
      params = {"element", "type"},
      script = "element.dispatchEvent(new Event(type, { bubbles: true, cancelable: true }));")
  static native void dispatch(HTMLElement element, String type);

  @JSBody(
      params = {"element", "type", "key"},
      script =
          "element.dispatchEvent(new KeyboardEvent(type,"
              + " { key: key, bubbles: true, cancelable: true }));")
  static native void dispatchKey(HTMLElement element, String type, String key);

  @JSBody(params = {"element"}, script = "return element.className;")
  static native String className(HTMLElement element);

  @JSBody(params = {"element"}, script = "return element.value;")
  static native String value(HTMLElement element);

  @JSBody(params = {"element", "value"}, script = "element.value = value;")
  static native void setValue(HTMLElement element, String value);

  @JSBody(params = {"element"}, script = "return !!element.disabled;")
  static native boolean isDisabled(HTMLElement element);

  @JSBody(params = {"element"}, script = "return !!element.checked;")
  static native boolean isChecked(HTMLElement element);

  @JSBody(params = {"element"}, script = "return element.isConnected;")
  static native boolean isInDocument(HTMLElement element);

  @JSBody(params = {"element"}, script = "return document.activeElement === element;")
  static native boolean hasFocus(HTMLElement element);

  @JSBody(
      params = {"element", "descendant"},
      script = "return element !== descendant && element.contains(descendant);")
  static native boolean contains(HTMLElement element, HTMLElement descendant);

  @JSBody(
      params = {"element", "property"},
      script = "return window.getComputedStyle(element).getPropertyValue(property).trim();")
  static native String style(HTMLElement element, String property);

  /** Whether the element takes up space and is not hidden by styling. */
  @JSBody(
      params = {"element"},
      script =
          "if (element.hidden) { return false; }"
              + "var style = window.getComputedStyle(element);"
              + "if (style.display === 'none' || style.visibility === 'hidden') { return false; }"
              + "return element.offsetParent !== null"
              + " || element.getClientRects().length > 0;")
  static native boolean isVisible(HTMLElement element);
}
