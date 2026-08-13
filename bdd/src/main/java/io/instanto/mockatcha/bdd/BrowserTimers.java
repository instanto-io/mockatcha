package io.instanto.mockatcha.bdd;

import org.teavm.jso.JSBody;
import org.teavm.jso.JSFunctor;
import org.teavm.jso.JSObject;

/** Replaces the browser's timer and time functions with ones that call back into {@link Clock}. */
final class BrowserTimers {

  private BrowserTimers() {}

  @JSFunctor
  interface ScheduleFunction extends JSObject {
    int schedule(JSObject callback, int delay, boolean repeating);
  }

  @JSFunctor
  interface CancelFunction extends JSObject {
    void cancel(int handle);
  }

  @JSFunctor
  interface NowFunction extends JSObject {
    double now();
  }

  /*
   * Date is replaced wholesale rather than having its `now` overwritten, because TeaVM compiles
   * System.currentTimeMillis() to `new Date().getTime()`. The replacement shares Date's prototype
   * so `instanceof` and every instance method keep working.
   */
  @JSBody(
      params = {"schedule", "cancel", "now", "elapsed"},
      script =
          "var g = typeof globalThis !== 'undefined' ? globalThis : window;"
              + "if (g.__bddClock) {"
              + "  throw new Error('The BDD clock is already installed');"
              + "}"
              + "var realDate = g.Date;"
              + "g.__bddClock = {"
              + "  setTimeout: g.setTimeout, clearTimeout: g.clearTimeout,"
              + "  setInterval: g.setInterval, clearInterval: g.clearInterval,"
              + "  requestAnimationFrame: g.requestAnimationFrame,"
              + "  cancelAnimationFrame: g.cancelAnimationFrame,"
              + "  Date: realDate,"
              + "  performanceNow: g.performance ? g.performance.now : null"
              + "};"
              + "g.setTimeout = function(fn, delay) {"
              + "  var extra = Array.prototype.slice.call(arguments, 2);"
              + "  return schedule(function() { fn.apply(null, extra); }, delay | 0, false);"
              + "};"
              + "g.setInterval = function(fn, delay) {"
              + "  var extra = Array.prototype.slice.call(arguments, 2);"
              + "  return schedule(function() { fn.apply(null, extra); }, delay | 0, true);"
              + "};"
              + "g.clearTimeout = function(handle) { cancel(handle | 0); };"
              + "g.clearInterval = function(handle) { cancel(handle | 0); };"
              + "g.requestAnimationFrame = function(fn) {"
              + "  return schedule(function() { fn(now()); }, 16, false);"
              + "};"
              + "g.cancelAnimationFrame = function(handle) { cancel(handle | 0); };"
              + "function FakeDate(a, b, c, d, e, f, h) {"
              + "  if (!(this instanceof FakeDate)) { return new realDate(now()).toString(); }"
              + "  switch (arguments.length) {"
              + "    case 0: return new realDate(now());"
              + "    case 1: return new realDate(a);"
              + "    case 2: return new realDate(a, b);"
              + "    case 3: return new realDate(a, b, c);"
              + "    case 4: return new realDate(a, b, c, d);"
              + "    case 5: return new realDate(a, b, c, d, e);"
              + "    case 6: return new realDate(a, b, c, d, e, f);"
              + "    default: return new realDate(a, b, c, d, e, f, h);"
              + "  }"
              + "}"
              + "FakeDate.prototype = realDate.prototype;"
              + "FakeDate.now = function() { return now(); };"
              + "FakeDate.parse = realDate.parse;"
              + "FakeDate.UTC = realDate.UTC;"
              + "g.Date = FakeDate;"
              + "if (g.performance) { g.performance.now = function() { return elapsed(); }; }")
  static native void install(
      ScheduleFunction schedule, CancelFunction cancel, NowFunction now, NowFunction elapsed);

  @JSBody(
      params = {},
      script =
          "var g = typeof globalThis !== 'undefined' ? globalThis : window;"
              + "var saved = g.__bddClock;"
              + "if (!saved) { return; }"
              + "g.setTimeout = saved.setTimeout;"
              + "g.clearTimeout = saved.clearTimeout;"
              + "g.setInterval = saved.setInterval;"
              + "g.clearInterval = saved.clearInterval;"
              + "g.requestAnimationFrame = saved.requestAnimationFrame;"
              + "g.cancelAnimationFrame = saved.cancelAnimationFrame;"
              + "g.Date = saved.Date;"
              + "if (g.performance && saved.performanceNow) {"
              + "  g.performance.now = saved.performanceNow;"
              + "}"
              + "delete g.__bddClock;")
  static native void uninstall();

  @JSBody(params = {"callback"}, script = "callback();")
  static native void run(JSObject callback);
}
