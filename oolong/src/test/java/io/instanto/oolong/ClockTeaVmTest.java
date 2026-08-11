package io.instanto.oolong;

import static io.instanto.mockatcha.Mockatcha.mock;
import static io.instanto.mockatcha.Mockatcha.never;
import static io.instanto.mockatcha.Mockatcha.times;
import static io.instanto.mockatcha.Mockatcha.verify;
import static io.instanto.oolong.Oolong.clock;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.jso.browser.Window;
import org.teavm.junit.SkipJVM;
import org.teavm.junit.TeaVMTestRunner;

/** The fake clock, which replaces the browser's timer functions while it is installed. */
@RunWith(TeaVMTestRunner.class)
@SkipJVM
public class ClockTeaVmTest {

  @After
  public void restoreTheBrowsersTimers() {
    clock().uninstall();
  }

  @Test
  public void runsATimeoutOnlyWhenTheClockReachesIt() {
    clock().install();
    List<String> ran = new ArrayList<>();

    Window.setTimeout(() -> ran.add("fired"), 500);

    clock().tick(499);
    assertEquals(List.of(), ran);
    clock().tick(1);
    assertEquals(List.of("fired"), ran);
  }

  @Test
  public void runsTimeoutsInDueOrderRatherThanScheduledOrder() {
    clock().install();
    List<String> ran = new ArrayList<>();

    Window.setTimeout(() -> ran.add("late"), 200);
    Window.setTimeout(() -> ran.add("early"), 100);

    clock().tick(1000);

    assertEquals(List.of("early", "late"), ran);
  }

  @Test
  public void repeatsAnIntervalOncePerPeriod() {
    clock().install();
    List<String> ran = new ArrayList<>();

    Window.setInterval(() -> ran.add("tick"), 100);

    clock().tick(350);

    assertEquals(3, ran.size());
  }

  @Test
  public void cancelsScheduledWork() {
    clock().install();
    List<String> ran = new ArrayList<>();

    int handle = Window.setTimeout(() -> ran.add("fired"), 100);
    Window.clearTimeout(handle);
    clock().tick(1000);

    assertEquals(List.of(), ran);
    assertEquals(0, clock().pending());
  }

  @Test
  public void runsWorkScheduledByAnotherCallback() {
    clock().install();
    List<String> ran = new ArrayList<>();

    Window.setTimeout(
        () -> {
          ran.add("first");
          Window.setTimeout(() -> ran.add("second"), 100);
        },
        100);

    clock().tick(200);

    assertEquals(List.of("first", "second"), ran);
  }

  @Test
  public void reportsATimeThatOnlyMovesWhenTheTestSaysSo() {
    clock().install().setTime(1_000_000L);

    assertEquals(1_000_000L, clock().now());
    assertEquals(1_000_000L, currentTimeFromTheBrowser());

    clock().tick(250);

    assertEquals(1_000_250L, clock().now());
    assertEquals(1_000_250L, currentTimeFromTheBrowser());
  }

  @Test
  public void javaTimeApisFollowTheClockToo() {
    clock().install().setTime(5_000_000L);

    assertEquals(5_000_000L, System.currentTimeMillis());
    assertEquals(5_000_000L, new java.util.Date().getTime());

    clock().tick(1_000);

    assertEquals(5_001_000L, System.currentTimeMillis());
    assertEquals(5_001_000L, new java.util.Date().getTime());
  }

  @Test
  public void anExplicitDateStillMeansWhatItSays() {
    clock().install().setTime(5_000_000L);

    assertEquals(1_234L, new java.util.Date(1_234L).getTime());
  }

  @Test
  public void countsWorkThatIsStillWaiting() {
    clock().install();

    Window.setTimeout(() -> { }, 100);
    Window.setTimeout(() -> { }, 200);

    assertEquals(2, clock().pending());
    clock().tick(150);
    assertEquals(1, clock().pending());
  }

  @Test
  public void drivesPollingCodeThroughItsCollaborator() {
    clock().install();
    Feed feed = mock(Feed.class);

    startPolling(feed, 250);

    verify(feed, never()).refresh();
    clock().tick(1000);
    verify(feed, times(4)).refresh();
  }

  @Test
  public void restoresTheBrowsersTimersOnUninstall() {
    clock().install();
    assertTrue(clock().isInstalled());

    clock().uninstall();

    assertEquals(false, clock().isInstalled());
    // A real timer can be scheduled and cancelled again without the clock intercepting it.
    Window.clearTimeout(Window.setTimeout(() -> { }, 100_000));
  }

  @Test
  public void refusesToAdvanceBeforeItIsInstalled() {
    IllegalStateException failure =
        assertThrows(IllegalStateException.class, () -> clock().tick(100));

    assertTrue(failure.getMessage(), failure.getMessage().contains("Install"));
  }

  @Test
  public void refusesTwoInstallations() {
    clock().install();

    assertThrows(IllegalStateException.class, () -> clock().install());
  }

  @Test
  public void discardsWaitingWorkWhenUninstalled() {
    clock().install();
    List<String> ran = new ArrayList<>();
    Window.setTimeout(() -> ran.add("fired"), 100);

    clock().uninstall();
    clock().install();
    clock().tick(1000);

    assertEquals(List.of(), ran);
  }

  private static void startPolling(Feed feed, int period) {
    Window.setInterval(feed::refresh, period);
  }

  private static long currentTimeFromTheBrowser() {
    return (long) org.teavm.jso.core.JSDate.now();
  }

  public interface Feed {
    void refresh();
  }
}
