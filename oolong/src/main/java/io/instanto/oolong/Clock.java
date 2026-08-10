package io.instanto.oolong;

import java.util.ArrayList;
import java.util.List;
import org.teavm.jso.JSObject;

/**
 * A clock a test advances by hand.
 *
 * <p>Browser code that polls, debounces, retries, or animates is hard to test against real time: a
 * test either sleeps, which makes it slow and flaky, or reaches inside the code under test to
 * expose its timers. Installing this clock replaces the browser's timer functions, so work
 * scheduled through {@code setTimeout}, {@code setInterval}, or
 * {@code requestAnimationFrame} waits until the test says otherwise.
 *
 * <pre>{@code
 * clock().install();
 * try {
 *     component.startPolling();
 *     clock().tick(1000);
 *     verify(feed, times(2)).refresh();
 * } finally {
 *     clock().uninstall();
 * }
 * }</pre>
 *
 * <p>Two constraints follow from replacing global functions. Install and uninstall within one
 * synchronous test, because anything else scheduling work in the meantime — including TeaVM's own
 * asynchronous support — is queued rather than run. And uninstall in a {@code finally} block, so a
 * failing assertion does not leave the browser's timers replaced for the tests that follow.
 */
public final class Clock {

  /** Stops a callback that keeps rescheduling itself from hanging the browser silently. */
  private static final int MAXIMUM_CALLBACKS_PER_TICK = 100_000;

  private final List<ScheduledCall> scheduled = new ArrayList<>();
  private long baseTime = 1_600_000_000_000L;
  private long elapsed;
  private int nextHandle = 1;
  private boolean installed;

  Clock() {}

  /**
   * Replaces the browser's timer functions until {@link #uninstall} is called.
   *
   * <p>The clock starts at a fixed instant rather than the real one, so a test that formats or
   * compares times is reproducible.
   */
  public Clock install() {
    if (installed) {
      throw new IllegalStateException("The Oolong clock is already installed");
    }
    scheduled.clear();
    elapsed = 0;
    BrowserTimers.install(this::schedule, this::cancel, () -> now());
    installed = true;
    return this;
  }

  /** Restores the browser's timer functions and discards anything still queued. */
  public void uninstall() {
    if (!installed) {
      return;
    }
    installed = false;
    scheduled.clear();
    BrowserTimers.uninstall();
  }

  public boolean isInstalled() {
    return installed;
  }

  /** Sets the instant the clock reports, without running anything that is due. */
  public Clock setTime(long epochMilliseconds) {
    baseTime = epochMilliseconds - elapsed;
    return this;
  }

  /** The instant this clock reports, which is what {@code Date.now()} returns while installed. */
  public long now() {
    return baseTime + elapsed;
  }

  /** How many callbacks are waiting, which is useful when asserting that nothing was scheduled. */
  public int pending() {
    return scheduled.size();
  }

  /**
   * Advances the clock, running everything that falls due along the way.
   *
   * <p>Callbacks run in due order and see the time they were scheduled for, so a callback that
   * schedules more work still observes a consistent clock. When everything due has run, the clock
   * sits at exactly the requested instant.
   */
  public void tick(long milliseconds) {
    if (!installed) {
      throw new IllegalStateException("Install the Oolong clock before advancing it");
    }
    if (milliseconds < 0) {
      throw new IllegalArgumentException("Time does not run backwards: " + milliseconds);
    }

    long target = elapsed + milliseconds;
    for (int callbacks = 0; ; callbacks++) {
      if (callbacks > MAXIMUM_CALLBACKS_PER_TICK) {
        throw new IllegalStateException(
            "Stopped after " + MAXIMUM_CALLBACKS_PER_TICK + " callbacks in one tick, because"
                + " something is rescheduling itself faster than the clock advances");
      }
      ScheduledCall next = nextDueBy(target);
      if (next == null) {
        break;
      }

      elapsed = Math.max(elapsed, next.due);
      if (next.repeating) {
        next.due = elapsed + Math.max(next.delay, 1);
      } else {
        scheduled.remove(next);
      }
      BrowserTimers.run(next.callback);
    }
    elapsed = target;
  }

  private ScheduledCall nextDueBy(long target) {
    ScheduledCall earliest = null;
    for (ScheduledCall candidate : scheduled) {
      if (candidate.due > target) {
        continue;
      }
      if (earliest == null
          || candidate.due < earliest.due
          || (candidate.due == earliest.due && candidate.sequence < earliest.sequence)) {
        earliest = candidate;
      }
    }
    return earliest;
  }

  private int schedule(JSObject callback, int delay, boolean repeating) {
    int handle = nextHandle++;
    scheduled.add(
        new ScheduledCall(handle, callback, elapsed + Math.max(delay, 0), delay, repeating));
    return handle;
  }

  private void cancel(int handle) {
    scheduled.removeIf(call -> call.handle == handle);
  }

  private static final class ScheduledCall {

    private static int sequenceGenerator;

    private final int handle;
    private final JSObject callback;
    private final int delay;
    private final boolean repeating;
    private final int sequence = sequenceGenerator++;
    private long due;

    private ScheduledCall(
        int handle, JSObject callback, long due, int delay, boolean repeating) {
      this.handle = handle;
      this.callback = callback;
      this.due = due;
      this.delay = delay;
      this.repeating = repeating;
    }
  }
}
