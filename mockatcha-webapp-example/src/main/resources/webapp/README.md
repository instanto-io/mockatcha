# Plain JavaScript sample application

This small single-page application uses HTML, CSS, and JavaScript without
TeaVM. Mockatcha stages it on TeaVM's test resource server, opens it in a
same-origin frame, and tests it from Java with Mockatcha DOM.

The application includes a text field, select control, toggle button,
asynchronous readiness signal, and responsive style. Together they exercise
DOM queries, cross-window events, focus, application state, and viewport-based
CSS.

The integration tests live in `mockatcha-dom`, which owns the browser-facing
API being exercised. See
[`FramedApplicationIntegrationTest`](../../../../../mockatcha-dom/src/test/java/io/instanto/mockatcha/dom/FramedApplicationIntegrationTest.java).
