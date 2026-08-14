package io.instanto.mockatcha.dom;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.jso.core.JSPromise;
import org.teavm.junit.SkipJVM;
import org.teavm.junit.TeaVMTestRunner;

/** Promise callbacks and TeaVM continuations in a browser test. */
@RunWith(TeaVMTestRunner.class)
@SkipJVM
public class PromiseTimingTeaVmTest {

  @Test
  public void aResolvedPromiseDoesNotCallBackWithinTheTest() {
    List<String> seen = new ArrayList<>();

    JSPromise.resolve("value").then(
        value -> {
          seen.add("resolved");
          return value;
        });

    // Spinning does not yield, so the microtask queue cannot drain here.
    for (int spin = 0; spin < 100_000; spin++) {
      if (!seen.isEmpty()) {
        break;
      }
    }

    assertEquals(List.of(), seen);
  }

  @Test
  public void awaitYieldsAndResumesTheTest() {
    assertEquals("value", JSPromise.resolve("value").await());
  }
}
