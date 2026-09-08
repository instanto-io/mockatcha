package io.instanto.mockatcha.dom;

import java.util.Objects;
import java.util.function.Predicate;
import org.teavm.jso.JSBody;
import org.teavm.jso.dom.html.HTMLDocument;
import org.teavm.jso.dom.html.HTMLElement;

/** A same-origin, independently bootstrapped application served by the test host. */
public final class FramedApplication implements AutoCloseable {
  private final HTMLElement frame;
  private final String url;
  private boolean closed;

  private FramedApplication(String url, int width, int height) {
    this.url = Objects.requireNonNull(url, "url");
    if (!isSameOriginUrl(url)) {
      throw new IllegalArgumentException("Application must use a same-origin HTTP URL: " + url);
    }
    frame = HTMLDocument.current().createElement("iframe");
    frame.setAttribute("title", "Application under test");
    resize(width, height);
    frame.setAttribute("src", url);
    Dom.container().appendChild(frame);
  }

  /** Opens an application staged by ApplicationFiles; it runs its own normal startup. */
  public static FramedApplication open(String url) {
    return new FramedApplication(url, 1024, 768);
  }

  public static FramedApplication open(String url, int width, int height) {
    return new FramedApplication(url, width, height);
  }

  /** Keeps the frame rendered so layout, focus and visibility assertions remain meaningful. */
  public FramedApplication resize(int width, int height) {
    requireOpen();
    if (width <= 0 || height <= 0) throw new IllegalArgumentException("Frame size must be positive");
    frame.setAttribute("width", Integer.toString(width));
    frame.setAttribute("height", Integer.toString(height));
    frame.setAttribute("style", "border:0;display:block");
    return this;
  }

  /** Waits for application readiness, independently of the frame's load event. */
  public FramedApplication awaitReady(Predicate<DomScope> ready, int timeoutMilliseconds) {
    Objects.requireNonNull(ready, "ready");
    try {
      Dom.waitFor(() -> {
        DomScope scope = page();
        if (!ready.test(scope)) throw new AssertionError("Application readiness condition is false");
      }, timeoutMilliseconds);
    } catch (AssertionError failure) {
      throw new AssertionError("Application " + url + " did not become ready: " + failure.getMessage(), failure);
    }
    return this;
  }

  public FramedApplication awaitReady(Predicate<DomScope> ready) {
    return awaitReady(ready, 10_000);
  }

  /** Returns queries scoped to the child document, including overlays attached to its body. */
  public DomScope page() {
    assertHealthy();
    HTMLElement body = body(frame);
    if (body == null || !hasDiagnostics(frame)) {
      throw new AssertionError("Waiting for " + url
          + "; stage the application with ApplicationFiles to capture startup errors");
    }
    return new DomScope(body);
  }

  /** Fails on captured startup errors, resource errors and unhandled promise rejections. */
  public void assertHealthy() {
    requireOpen();
    String failure = failure(frame);
    if (failure != null) throw new IllegalStateException("Application " + url + ": " + failure);
  }

  /** Checks child errors before removing the frame, even when a scenario assertion failed. */
  @Override
  public void close() {
    if (closed) return;
    try {
      assertHealthy();
    } finally {
      closed = true;
      if (frame.getParentNode() != null) frame.getParentNode().removeChild(frame);
    }
  }

  private void requireOpen() {
    if (closed) throw new IllegalStateException("Application frame is closed: " + url);
  }

  @JSBody(params = "url", script = "try { var target = new URL(url, document.baseURI);"
      + "return /^https?:$/.test(target.protocol) && target.origin === location.origin;"
      + "} catch (error) { return false; }")
  private static native boolean isSameOriginUrl(String url);

  @JSBody(params = "frame", script = "return frame.contentDocument && frame.contentDocument.body;")
  private static native HTMLElement body(HTMLElement frame);

  @JSBody(params = "frame", script = "return !!frame.contentWindow.__mockatchaFrameDiagnostics;")
  private static native boolean hasDiagnostics(HTMLElement frame);

  @JSBody(params = "frame", script = "try {"
      + " var doc = frame.contentWindow.document;"
      + " var diagnostics = frame.contentWindow.__mockatchaFrameDiagnostics;"
      + " return diagnostics && diagnostics.errors.length ? diagnostics.errors.join('\\n') : null;"
      + "} catch (error) { return 'Cannot inspect application: cross-origin navigation or inaccessible document'; }")
  private static native String failure(HTMLElement frame);
}
