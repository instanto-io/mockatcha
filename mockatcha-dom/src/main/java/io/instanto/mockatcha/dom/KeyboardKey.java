package io.instanto.mockatcha.dom;

/** Key-only helpers use the standard US position for printable ASCII keys. */
record KeyboardKey(String key, String code, int legacyCode) {
  static KeyboardKey of(String key) {
    return switch (key) {
      case "Esc", "Escape" -> new KeyboardKey("Escape", "Escape", 27);
      case "Left", "ArrowLeft" -> new KeyboardKey("ArrowLeft", "ArrowLeft", 37);
      case "Up", "ArrowUp" -> new KeyboardKey("ArrowUp", "ArrowUp", 38);
      case "Right", "ArrowRight" -> new KeyboardKey("ArrowRight", "ArrowRight", 39);
      case "Down", "ArrowDown" -> new KeyboardKey("ArrowDown", "ArrowDown", 40);
      case "Spacebar", "Space", " " -> new KeyboardKey(" ", "Space", 32);
      case "Enter" -> new KeyboardKey(key, key, 13);
      case "Tab" -> new KeyboardKey(key, key, 9);
      case "Backspace" -> new KeyboardKey(key, key, 8);
      case "Delete", "Del" -> new KeyboardKey("Delete", "Delete", 46);
      case "Insert" -> new KeyboardKey(key, key, 45);
      case "Home" -> new KeyboardKey(key, key, 36);
      case "End" -> new KeyboardKey(key, key, 35);
      case "PageUp" -> new KeyboardKey(key, key, 33);
      case "PageDown" -> new KeyboardKey(key, key, 34);
      default -> printableOrFunction(key);
    };
  }

  private static KeyboardKey printableOrFunction(String key) {
    if (key.length() == 1) {
      char c = key.charAt(0);
      if (c >= 'a' && c <= 'z') return new KeyboardKey(key, "Key" + (char) (c - 32), c - 32);
      if (c >= 'A' && c <= 'Z') return new KeyboardKey(key, "Key" + c, c);
      if (c >= '0' && c <= '9') return new KeyboardKey(key, "Digit" + c, c);
      String plain = "-=[]\\;',./`";
      String shifted = "_+{}|:\"<>?~";
      String[] codes = {"Minus", "Equal", "BracketLeft", "BracketRight", "Backslash",
          "Semicolon", "Quote", "Comma", "Period", "Slash", "Backquote"};
      int[] numbers = {189, 187, 219, 221, 220, 186, 222, 188, 190, 191, 192};
      int index = plain.indexOf(c);
      if (index < 0) index = shifted.indexOf(c);
      if (index >= 0) return new KeyboardKey(key, codes[index], numbers[index]);
      index = ")!@#$%^&*(".indexOf(c);
      if (index >= 0) return new KeyboardKey(key, "Digit" + index, 48 + index);
    }
    if (key.matches("F([1-9]|1[0-9]|2[0-4])")) {
      return new KeyboardKey(key, key, 111 + Integer.parseInt(key.substring(1)));
    }
    return new KeyboardKey(key, "", 0);
  }
}
