package io.instanto.mockatcha.browser;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Stages a built application under the test classpath for TeaVMTestRunner's resource server. */
public final class ApplicationFiles {
  private static final Pattern HEAD = Pattern.compile("(?is)<head(?:\\s[^>]*)?>");
  private static final String MARKER = "data-mockatcha-diagnostics";
  private static final String STAGED = ".mockatcha-staged-app";
  private static final String DIAGNOSTICS = """
      <script data-mockatcha-diagnostics>
      (function () {
        if (window.__mockatchaFrameDiagnostics) return;
        var state = window.__mockatchaFrameDiagnostics = { errors: [] };
        window.addEventListener('error', function (event) {
          if (event.target && event.target !== window) {
            state.errors.push('Resource failed: ' + (event.target.src || event.target.href || event.target.tagName));
          } else {
            state.errors.push(String(event.message) + ' at ' + event.filename + ':' + event.lineno);
          }
        }, true);
        window.addEventListener('unhandledrejection', function (event) {
          state.errors.push('Unhandled rejection: ' + String(event.reason));
        });
      })();
      </script>
      """;

  private ApplicationFiles() {}

  /**
   * Copies a complete UTF-8 application tree and adds error observation before its scripts run.
   * Replaces only destinations previously created by this tool. The source is never modified.
   */
  public static void stage(Path source, Path destination) throws IOException {
    Path root = source.toRealPath();
    Path target = destination.toAbsolutePath().normalize();
    if (!Files.isDirectory(root)) throw new IOException("Application is not a directory: " + root);
    Path parent = target.getParent();
    Files.createDirectories(parent);
    target = parent.toRealPath().resolve(target.getFileName());
    if (target.startsWith(root)) throw new IOException("Destination must be outside the source application");
    if (root.startsWith(target)) throw new IOException("Destination must not contain the source application");
    if (Files.exists(target)) {
      if (Files.isSymbolicLink(target) || !Files.isRegularFile(target.resolve(STAGED))) {
        throw new IOException("Application destination already exists: " + target);
      }
      deleteTree(target);
    }
    try (var paths = Files.walk(root)) {
      for (Path file : paths.sorted().toList()) {
        if (Files.isSymbolicLink(file)) throw new IOException("Application contains a symbolic link: " + file);
        Path output = target.resolve(root.relativize(file));
        if (Files.isDirectory(file)) {
          Files.createDirectories(output);
        } else if (file.toString().endsWith(".html") || file.toString().endsWith(".htm")) {
          Files.writeString(output, instrument(Files.readString(file, StandardCharsets.UTF_8)), StandardCharsets.UTF_8);
        } else {
          Files.copy(file, output, StandardCopyOption.COPY_ATTRIBUTES);
        }
      }
      Files.writeString(target.resolve(STAGED), "Staged browser application\n");
    } catch (IOException failure) {
      try {
        deleteTree(target);
      } catch (IOException cleanup) {
        failure.addSuppressed(cleanup);
      }
      throw failure;
    }
  }

  private static void deleteTree(Path root) throws IOException {
    if (!Files.exists(root)) return;
    try (var paths = Files.walk(root)) {
      for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) Files.delete(path);
    }
  }

  static String instrument(String html) throws IOException {
    if (html.contains(MARKER)) throw new IOException("Application has already been instrumented");
    Matcher head = HEAD.matcher(html);
    if (!head.find()) throw new IOException("Application HTML requires an explicit head element");
    return html.substring(0, head.end()) + "\n" + DIAGNOSTICS + html.substring(head.end());
  }

  /** Build-tool entry point: source directory followed by a new test-classpath directory. */
  public static void main(String[] args) throws IOException {
    if (args.length != 2) throw new IllegalArgumentException("Usage: ApplicationFiles <built-app> <test-classpath-app>");
    stage(Path.of(args[0]), Path.of(args[1]));
  }
}
