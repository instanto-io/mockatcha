package io.instanto.mockatcha.browser;

import static org.junit.Assert.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class ApplicationFilesTest {
  @Rule public TemporaryFolder temporary = new TemporaryFolder();

  @Test public void preservesAssetTreeAndObservesErrorsBeforeApplicationStartup() throws Exception {
    Path app = temporary.newFolder("app").toPath();
    Files.createDirectories(app.resolve("permutations"));
    String html = "<!doctype html><html><head><base href='./'><script src='permutations/app.js'></script></head><body></body></html>";
    Files.writeString(app.resolve("index.html"), html);
    byte[] asset = {0, 1, 2, (byte) 255};
    Files.write(app.resolve("permutations/app.js"), asset);
    Path target = temporary.getRoot().toPath().resolve("staged");
    ApplicationFiles.stage(app, target);
    String output = Files.readString(target.resolve("index.html"));
    assertTrue(output.indexOf("data-mockatcha-diagnostics") < output.indexOf("<base"));
    assertTrue(output.contains("src='permutations/app.js'"));
    assertArrayEquals(asset, Files.readAllBytes(target.resolve("permutations/app.js")));
    assertEquals(html, Files.readString(app.resolve("index.html")));
  }

  @Test public void doesNotOverwriteAnExistingDestination() throws Exception {
    Path app = temporary.newFolder("app").toPath();
    Path target = temporary.newFolder("staged").toPath();
    Files.writeString(target.resolve("keep"), "existing");
    assertThrows(IOException.class, () -> ApplicationFiles.stage(app, target));
    assertEquals("existing", Files.readString(target.resolve("keep")));
  }

  @Test public void rejectsRecursiveCopiesAndMalformedDocuments() throws Exception {
    Path app = temporary.newFolder("app").toPath();
    assertThrows(IOException.class, () -> ApplicationFiles.stage(app, app.resolve("copy")));
    Files.writeString(app.resolve("index.html"), "<body>No head</body>");
    Path target = temporary.getRoot().toPath().resolve("staged");
    assertThrows(IOException.class, () -> ApplicationFiles.stage(app, target));
    assertFalse(Files.exists(target));
  }

  @Test public void rejectsSymlinksWithoutCopyingExternalFiles() throws Exception {
    Path app = temporary.newFolder("app").toPath();
    Path outside = temporary.newFile("outside").toPath();
    Files.createSymbolicLink(app.resolve("link"), outside);
    Path target = temporary.getRoot().toPath().resolve("staged");
    assertThrows(IOException.class, () -> ApplicationFiles.stage(app, target));
    assertTrue(Files.exists(outside));
    assertFalse(Files.exists(target));
  }

  @Test public void restagingRemovesStaleOutputs() throws Exception {
    Path app = temporary.newFolder("app").toPath();
    Files.writeString(app.resolve("old.js"), "old");
    Path target = temporary.getRoot().toPath().resolve("staged");
    ApplicationFiles.stage(app, target);
    Files.delete(app.resolve("old.js"));
    Files.writeString(app.resolve("new.js"), "new");
    ApplicationFiles.stage(app, target);
    assertFalse(Files.exists(target.resolve("old.js")));
    assertEquals("new", Files.readString(target.resolve("new.js")));
  }
}
