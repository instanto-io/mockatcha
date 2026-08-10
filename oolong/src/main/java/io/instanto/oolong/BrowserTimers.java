package io.instanto.oolong;

import org.teavm.jso.JSBody;
import org.teavm.jso.JSFunctor;
import org.teavm.jso.JSObject;

/**
 * The browser side of the fake clock.
 *
 * <p>Jasmine replaces the global timer functions so that scheduled work can be driven by hand.
 * TeaVM compiles {@code Window.setTimeout} and friends to those same globals, so the same trick
 * works here: replace them with functions that call back into Java, and keep the originals so they
 * can be put back.
 */
final class BrowserTimers {

  private BrowserTimers() {}

  /** Called from the browser when application code schedules work. Returns a cancellation handle. */
  @JSFunctor
  interface ScheduleFunction extends JSObject {
    int schedule(JSObject callback, int delay, boolean repeating);
  }

  /** Called from the browser when application code cancels scheduled work. */
  @JSFunctor
  interface CancelFunction extends JSObject {
    void cancel(int handle);
  }

  /** Called from the browser whenever it asks for the current time. */
  @JSFunctor
  interface NowFunction extends JSObject {
    double now();
  }

  @JSBody(
      params = {"schedule", "cancel", "now"},
      script =
          "var g = typeof globalThis !== 'undefined' ? globalThis : window;"
              + "if (g.__oolongClock) {"
              + "  throw new Error('The Oolong clock is already installed');"
              + "}"
              + "g.__oolongClock = {"
              + "  setTimeout: g.setTimeout, clearTimeout: g.clearTimeout,"
              + "  setInterval: g.setInterval, clearInterval: g.clearInterval,"
              + "  requestAnimationFrame: g.requestAnimationFrame,"
              + "  cancelAnimationFrame: g.cancelAnimationFrame,"
              + "  now: Date.now"
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
              + "  return schedule(function() { fn(Date.now()); }, 16, false);"
              + "};"
              + "g.cancelAnimationFrame = function(handle) { cancel(handle | 0); };"
              + "Date.now = function() { return now(); };")
  static native void install(ScheduleFunction schedule, CancelFunction cancel, NowFunction now);

  @JSBody(
      params = {},
      script =
          "var g = typeof globalThis !== 'undefined' ? globalThis : window;"
              + "var saved = g.__oolongClock;"
              + "if (!saved) { return; }"
              + "g.setTimeout = saved.setTimeout;"
              + "g.clearTimeout = saved.clearTimeout;"
              + "g.setInterval = saved.setInterval;"
              + "g.clearInterval = saved.clearInterval;"
              + "g.requestAnimationFrame = saved.requestAnimationFrame;"
              + "g.cancelAnimationFrame = saved.cancelAnimationFrame;"
              + "Date.now = saved.now;"
              + "delete g.__oolongClock;")
  static native void uninstall();

  @JSBody(
      params = {},
      script =
          "var g = typeof globalThis !== 'undefined' ? globalThis : window;"
              + "return !!g.__oolongClock;")
  static native boolean installed();

  @JSBody(params = {"callback"}, script = "callback();")
  static native void run(JSObject callback);
}
