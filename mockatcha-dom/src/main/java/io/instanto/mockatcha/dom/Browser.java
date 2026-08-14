package io.instanto.mockatcha.dom;

import org.teavm.jso.JSBody;
import org.teavm.jso.JSFunctor;
import org.teavm.jso.JSObject;
import org.teavm.jso.dom.html.HTMLElement;

/** The few browser operations TeaVM's DOM API does not expose directly. */
final class Browser {

  private Browser() {}

  @JSFunctor
  interface TimerCallback extends JSObject {
    void run();
  }

  /** Schedules test-library polling without consuming or depending on the application's fake clock. */
  @JSBody(
      params = {"callback", "delay"},
      script =
          "var g = typeof globalThis !== 'undefined' ? globalThis : window;"
              + "var timer = g.__bddClock ? g.__bddClock.setTimeout : g.setTimeout;"
              + "timer.call(g, callback, delay);")
  static native void setPollingTimeout(TimerCallback callback, int delay);

  @JSBody(params = {"element"}, script = "return element.innerHTML;")
  static native String innerHtml(HTMLElement element);

  @JSBody(
      params = {"element"},
      script =
          "return Array.prototype.filter.call(element.childNodes,"
              + " function(node) { return node.nodeType === 3; })"
              + ".map(function(node) { return node.nodeValue; }).join('');")
  static native String ownText(HTMLElement element);

  /** Explicit or common implicit ARIA role for an element. */
  @JSBody(
      params = {"element"},
      script =
          "for (var node = element; node && node.nodeType === 1; node = node.parentElement) {"
              + " if (node.hidden || node.getAttribute('aria-hidden') === 'true') return null;"
              + " var style = window.getComputedStyle(node);"
              + " if (style.display === 'none' || style.visibility === 'hidden') return null;"
              + "}"
              + "var explicit = element.getAttribute('role');"
              + "if (explicit) { return explicit.trim().split(/\\s+/)[0]; }"
              + "var tag = element.tagName.toLowerCase();"
              + "if (tag === 'button') return 'button';"
              + "if (tag === 'a' && element.hasAttribute('href')) return 'link';"
              + "if (tag === 'textarea') return 'textbox';"
              + "if (tag === 'select') return element.multiple || element.size > 1"
              + " ? 'listbox' : 'combobox';"
              + "if (tag === 'img' && element.hasAttribute('alt')) return 'img';"
              + "if (tag === 'main') return 'main';"
              + "if (tag === 'nav') return 'navigation';"
              + "if (tag === 'article') return 'article';"
              + "if (tag === 'aside') return 'complementary';"
              + "if (tag === 'table') return 'table';"
              + "if (tag === 'thead' || tag === 'tbody' || tag === 'tfoot') return 'rowgroup';"
              + "if (tag === 'tr') return 'row';"
              + "if (tag === 'td') return 'cell';"
              + "if (tag === 'th') return element.scope === 'row' ? 'rowheader' : 'columnheader';"
              + "if (tag === 'ul' || tag === 'ol') return 'list';"
              + "if (tag === 'li') return 'listitem';"
              + "if (tag === 'option') return 'option';"
              + "if (tag === 'progress') return 'progressbar';"
              + "if (tag === 'output') return 'status';"
              + "if (tag === 'summary') return 'button';"
              + "if (/^h[1-6]$/.test(tag)) return 'heading';"
              + "if (tag === 'input') {"
              + " var type = (element.type || 'text').toLowerCase();"
              + " if (type === 'checkbox') return 'checkbox';"
              + " if (type === 'radio') return 'radio';"
              + " if (type === 'button' || type === 'submit' || type === 'reset') return 'button';"
              + " if (type === 'range') return 'slider';"
              + " if (type === 'number') return 'spinbutton';"
              + " if (type === 'search') return 'searchbox';"
              + " if (type !== 'hidden' && type !== 'password') return 'textbox';"
              + "}"
              + "return null;")
  static native String role(HTMLElement element);

  /** A practical accessible-name calculation covering ARIA, labels, images, and controls. */
  @JSBody(
      params = {"element"},
      script =
          "function clean(value) { return (value || '').replace(/\\s+/g, ' ').trim(); }"
              + "var labelledBy = element.getAttribute('aria-labelledby');"
              + "if (labelledBy) {"
              + " var names = labelledBy.trim().split(/\\s+/).map(function(id) {"
              + "  var label = document.getElementById(id); return label ? label.textContent : '';"
              + " }).join(' ');"
              + " if (clean(names)) return clean(names);"
              + "}"
              + "var aria = element.getAttribute('aria-label');"
              + "if (clean(aria)) return clean(aria);"
              + "if (element.labels && element.labels.length) {"
              + " var labels = Array.prototype.map.call(element.labels,"
              + "  function(label) { return label.textContent; }).join(' ');"
              + " if (clean(labels)) return clean(labels);"
              + "}"
              + "if (element.tagName === 'IMG' && element.hasAttribute('alt'))"
              + " return clean(element.getAttribute('alt'));"
              + "if (element.tagName === 'INPUT'"
              + " && /^(button|submit|reset)$/.test(element.type)) return clean(element.value);"
              + "var content = clean(element.textContent);"
              + "return content || clean(element.getAttribute('title'));")
  static native String accessibleName(HTMLElement element);

  @JSBody(
      params = {"element"},
      script =
          "var aria = element.getAttribute('aria-level');"
              + "if (aria) return parseInt(aria, 10) || 0;"
              + "var tag = element.tagName.toLowerCase();"
              + "return /^h[1-6]$/.test(tag) ? parseInt(tag.substring(1), 10) : 0;")
  static native int headingLevel(HTMLElement element);

  @JSBody(
      params = {"element"},
      script =
          "var aria = element.getAttribute('aria-checked');"
              + "if (aria === 'true') return 1; if (aria === 'false') return 0;"
              + "return ('checked' in element) ? (element.checked ? 1 : 0) : -1;")
  static native int checkedState(HTMLElement element);

  @JSBody(
      params = {"element"},
      script =
          "var aria = element.getAttribute('aria-selected');"
              + "if (aria === 'true') return 1; if (aria === 'false') return 0;"
              + "return ('selected' in element) ? (element.selected ? 1 : 0) : -1;")
  static native int selectedState(HTMLElement element);

  @JSBody(
      params = {"element"},
      script =
          "var aria = element.getAttribute('aria-expanded');"
              + "return aria === 'true' ? 1 : aria === 'false' ? 0 : -1;")
  static native int expandedState(HTMLElement element);

  @JSBody(
      params = {"element"},
      script =
          "var aria = element.getAttribute('aria-pressed');"
              + "return aria === 'true' ? 1 : aria === 'false' ? 0 : -1;")
  static native int pressedState(HTMLElement element);

  /** The visible or ARIA label associated with a labelable control, or null. */
  @JSBody(
      params = {"element"},
      script =
          "function clean(value) { return (value || '').replace(/\\s+/g, ' ').trim(); }"
              + "if (element.labels && element.labels.length) {"
              + " var labels = Array.prototype.map.call(element.labels,"
              + "  function(label) { return label.textContent; }).join(' ');"
              + " if (clean(labels)) return clean(labels);"
              + "}"
              + "var labelledBy = element.getAttribute('aria-labelledby');"
              + "if (labelledBy) {"
              + " var names = labelledBy.trim().split(/\\s+/).map(function(id) {"
              + "  var label = document.getElementById(id); return label ? label.textContent : '';"
              + " }).join(' ');"
              + " if (clean(names)) return clean(names);"
              + "}"
              + "var aria = clean(element.getAttribute('aria-label'));"
              + "return aria || null;")
  static native String labelText(HTMLElement element);

  /** Finds a form control by explicit/implicit label or ARIA labelling. */
  @JSBody(
      params = {"container", "text"},
      script =
          "function clean(value) { return (value || '').replace(/\\s+/g, ' ').trim(); }"
              + "var labels = container.querySelectorAll('label');"
              + "for (var i = 0; i < labels.length; i++) {"
              + " if (clean(labels[i].textContent) === text) {"
              + "  var control = labels[i].control || labels[i].querySelector("
              + "   'button,input,meter,output,progress,select,textarea');"
              + "  if (control) return control;"
              + " }"
              + "}"
              + "var candidates = container.querySelectorAll("
              + " 'button,input,meter,output,progress,select,textarea,[aria-label],[aria-labelledby]');"
              + "for (var j = 0; j < candidates.length; j++) {"
              + " var candidate = candidates[j];"
              + " if (clean(candidate.getAttribute('aria-label')) === text) return candidate;"
              + " var ids = candidate.getAttribute('aria-labelledby');"
              + " if (ids) {"
              + "  var name = ids.trim().split(/\\s+/).map(function(id) {"
              + "   var labelled = document.getElementById(id);"
              + "   if (labelled && !container.contains(labelled)) labelled = null;"
              + "   return labelled ? labelled.textContent : '';"
              + "  }).join(' ');"
              + "  if (clean(name) === text) return candidate;"
              + " }"
              + "}"
              + "return null;")
  static native HTMLElement findByLabel(HTMLElement container, String text);

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

  @JSBody(
      params = {"element"},
      script = "return typeof element.value === 'string' ? element.value : null;")
  static native String value(HTMLElement element);

  @JSBody(params = {"element", "value"}, script = "element.value = value;")
  static native void setValue(HTMLElement element, String value);

  /** Uses the browser's native setter so frameworks that track values observe the change. */
  @JSBody(
      params = {"element", "value"},
      script =
          "var prototype = Object.getPrototypeOf(element);"
              + "var descriptor = prototype && Object.getOwnPropertyDescriptor(prototype, 'value');"
              + "if (descriptor && descriptor.set) descriptor.set.call(element, value);"
              + "else element.value = value;")
  static native void setNativeValue(HTMLElement element, String value);

  @JSBody(
      params = {"element", "key"},
      script =
          "element.focus();"
              + "var down = new KeyboardEvent('keydown', {key:key,bubbles:true,cancelable:true});"
              + "if (!element.dispatchEvent(down)) {"
              + " element.dispatchEvent(new KeyboardEvent('keyup',"
              + "  {key:key,bubbles:true,cancelable:true})); return false;"
              + "}"
              + "element.dispatchEvent(new KeyboardEvent('keypress',"
              + " {key:key,bubbles:true,cancelable:true}));"
              + "var before = new InputEvent('beforeinput',"
              + " {data:key,inputType:'insertText',bubbles:true,cancelable:true});"
              + "if (element.dispatchEvent(before)) return true;"
              + "element.dispatchEvent(new KeyboardEvent('keyup',"
              + " {key:key,bubbles:true,cancelable:true})); return false;")
  static native boolean beginTypingCharacter(HTMLElement element, String key);

  @JSBody(
      params = {"element", "key"},
      script =
          "element.dispatchEvent(new InputEvent('input',"
              + " {data:key,inputType:'insertText',bubbles:true}));"
              + "element.dispatchEvent(new KeyboardEvent('keyup',"
              + " {key:key,bubbles:true,cancelable:true}));")
  static native void finishTypingCharacter(HTMLElement element, String key);

  @JSBody(params = {"element", "checked"}, script = "element.checked = checked;")
  static native void setChecked(HTMLElement element, boolean checked);

  @JSBody(
      params = {"element"},
      script =
          "var tag = element.tagName.toLowerCase();"
              + "return tag === 'input' ? (element.type || 'text').toLowerCase() : '';")
  static native String inputType(HTMLElement element);

  @JSBody(
      params = {"element", "type"},
      script =
          "element.dispatchEvent(new MouseEvent(type,"
              + " {bubbles:type !== 'mouseenter' && type !== 'mouseleave',cancelable:true}));")
  static native void dispatchMouse(HTMLElement element, String type);

  @JSBody(params = {"element"}, script = "element.focus();")
  static native void focus(HTMLElement element);

  @JSBody(params = {"element"}, script = "element.blur();")
  static native void blur(HTMLElement element);

  /** Returns 0 for a non-select, 1 for a missing option, and 2 after selecting the value. */
  @JSBody(
      params = {"element", "value"},
      script =
          "if (element.tagName !== 'SELECT') return 0;"
              + "for (var i = 0; i < element.options.length; i++) {"
              + " if (element.options[i].value === value) { element.value = value; return 2; }"
              + "}"
              + "return 1;")
  static native int select(HTMLElement element, String value);

  /** Returns 0 for a non-select and otherwise selects every requested value. */
  @JSBody(
      params = {"element", "values"},
      script =
          "if (element.tagName !== 'SELECT') return 0;"
              + "var wanted = Array.prototype.slice.call(values);"
              + "if (!element.multiple && wanted.length > 1) return 3;"
              + "var complete = wanted.every(function(value) {"
              + " return Array.prototype.some.call(element.options,"
              + "  function(option) { return option.value === value; });"
              + "});"
              + "if (!complete) return 1;"
              + "for (var i = 0; i < element.options.length; i++) {"
              + " element.options[i].selected = wanted.indexOf(element.options[i].value) >= 0;"
              + "}"
              + "return 2;")
  static native int selectOptions(HTMLElement element, String[] values);

  /** Moves focus to the next or previous focusable descendant. */
  @JSBody(
      params = {"root", "backwards"},
      script =
          "var selector = 'a[href],button,input,select,textarea,[tabindex]';"
              + "var items = Array.prototype.filter.call(root.querySelectorAll(selector),"
              + " function(item) {"
              + "  var tab = item.getAttribute('tabindex');"
              + "  return !item.disabled && tab !== '-1'"
              + "   && getComputedStyle(item).display !== 'none'"
              + "   && getComputedStyle(item).visibility !== 'hidden';"
              + " });"
              + "if (!items.length) return null;"
              + "var current = items.indexOf(document.activeElement);"
              + "var next = backwards ? current - 1 : current + 1;"
              + "if (current < 0) next = backwards ? items.length - 1 : 0;"
              + "if (next < 0) next = items.length - 1; if (next >= items.length) next = 0;"
              + "items[next].focus(); return items[next];")
  static native HTMLElement tab(HTMLElement root, boolean backwards);

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

  @JSBody(
      params = {"element"},
      script = "return !!element.required || element.getAttribute('aria-required') === 'true';")
  static native boolean isRequired(HTMLElement element);

  @JSBody(
      params = {"element"},
      script =
          "var aria = element.getAttribute('aria-invalid');"
              + "if (aria && aria !== 'false') return true;"
              + "return element.validity ? !element.validity.valid : false;")
  static native boolean isInvalid(HTMLElement element);

  @JSBody(
      params = {"root", "name"},
      script =
          "var controls = Array.prototype.filter.call(root.querySelectorAll('[name]'),"
              + " function(control) { return control.name === name; });"
              + "if (!controls.length) return null;"
              + "var first = controls[0]; var tag = first.tagName.toLowerCase();"
              + "var type = (first.type || '').toLowerCase();"
              + "if (type === 'radio') {"
              + " var radio = controls.find(function(control) { return control.checked; });"
              + " return radio ? radio.value : '';"
              + "}"
              + "if (type === 'checkbox') {"
              + " if (controls.length === 1) return String(!!first.checked);"
              + " return controls.filter(function(control) { return control.checked; })"
              + "  .map(function(control) { return control.value; }).join('\\u001f');"
              + "}"
              + "if (tag === 'select' && first.multiple) {"
              + " return Array.prototype.filter.call(first.options, function(option) {"
              + "  return option.selected; }).map(function(option) { return option.value; })"
              + "  .join('\\u001f');"
              + "}"
              + "return first.value;")
  static native String formValue(HTMLElement root, String name);
}
