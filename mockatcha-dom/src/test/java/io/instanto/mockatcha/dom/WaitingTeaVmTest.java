package io.instanto.mockatcha.dom;

import static io.instanto.mockatcha.dom.Dom.awaitByRole;
import static io.instanto.mockatcha.dom.Dom.findByText;
import static io.instanto.mockatcha.dom.Dom.render;
import static io.instanto.mockatcha.dom.Dom.waitFor;
import static io.instanto.mockatcha.dom.Expect.expect;
import static io.instanto.mockatcha.bdd.Bdd.clock;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.jso.browser.Window;
import org.teavm.jso.core.JSPromise;
import org.teavm.junit.SkipJVM;
import org.teavm.junit.TeaVMTestRunner;

@RunWith(TeaVMTestRunner.class)
@SkipJVM
public class WaitingTeaVmTest {

  @Rule public DomRule dom = new DomRule();

  @Test
  public void waitsForAnElementRenderedByLaterBrowserWork() {
    render("<main></main>");
    Window.setTimeout(() -> Browser.setInnerHtml(Dom.find("main"), "<h1>Ready</h1>"), 20);

    expect(awaitByRole("heading", "Ready")).toHaveText("Ready");
  }

  @Test
  public void waitsForAnAssertionAboutAnExistingElement() {
    render("<p>Loading</p>");
    Window.setTimeout(() -> findByText("Loading").setInnerText("Complete"), 20);

    waitFor(() -> expect(Dom.find("p")).toHaveText("Complete"));
  }

  @Test
  public void timeoutRetainsTheLastQueryDiagnostics() {
    render("<p>Still loading</p>");

    AssertionError failure = assertThrows(AssertionError.class,
        () -> waitFor(() -> Dom.findByRole("heading"), 20));

    assertTrue(failure.getMessage(), failure.getMessage().contains("Still loading"));
  }

  @Test
  public void waitingStillResumesWhileTheApplicationClockIsInstalled() {
    render("<p>Loading</p>");
    clock().install();
    try {
      Window.setTimeout(
          () -> JSPromise.resolve("Ready").then(value -> {
            Dom.find("p").setInnerText(value);
            return value;
          }),
          100);

      clock().tick(100);
      long fakeTimeAfterTick = clock().now();

      expect(Dom.awaitByText("Ready")).toHaveText("Ready");
      assertTrue(clock().isInstalled());
      assertEquals(fakeTimeAfterTick, clock().now());
    } finally {
      clock().uninstall();
    }
  }
}
