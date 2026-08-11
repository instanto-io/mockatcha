package io.instanto.oolong.dom;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.jso.core.JSPromise;
import org.teavm.junit.SkipJVM;
import org.teavm.junit.TeaVMTestRunner;

/**
 * A promise callback does not run while the test that started it is still running.
 *
 * <p>This is why there is no {@code waitFor}. The browser drains promise callbacks once the call
 * stack empties, which is after the test method returns, so no amount of waiting inside the test
 * reaches them. Code written against a result type that completes on the spot, such as Sarto
 * Async's {@code PortableResult}, is testable without waiting at all.
 */
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
}
