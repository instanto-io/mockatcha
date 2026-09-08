package io.instanto.mockatcha.dom.compile;

import org.teavm.backend.javascript.TeaVMJavaScriptHost;
import org.teavm.backend.javascript.spi.Injector;
import org.teavm.backend.javascript.spi.InjectorContext;
import org.teavm.model.MethodReference;
import org.teavm.model.ValueType;
import org.teavm.vm.spi.TeaVMHost;
import org.teavm.vm.spi.TeaVMPlugin;

/** Preserves checked JSO casts while accepting DOM nodes from their own window. */
public final class DomRealmPlugin implements TeaVMPlugin {
  @Override public void install(TeaVMHost host) {
    TeaVMJavaScriptHost javascript = host.getExtension(TeaVMJavaScriptHost.class);
    if (javascript == null) return;
    ValueType object = ValueType.object("org.teavm.jso.JSObject");
    for (String name : new String[] {"instanceOf", "instanceOfOrNull"}) {
      javascript.add(new MethodReference("org.teavm.jso.impl.JS", name,
          object, object, ValueType.BOOLEAN), new DomInstanceOf(name.endsWith("OrNull")));
    }
  }

  private record DomInstanceOf(boolean nullable) implements Injector {
    @Override public void generate(InjectorContext context, MethodReference method) {
      var writer = context.getWriter();
      writer.append("(function(value, type) {");
      writer.append(nullable ? "if (value == null) return true;" : "if (value == null) return false;");
      writer.append("if (value instanceof type) return true;");
      // Limit the fallback to native DOM constructors; unrelated JavaScript casts stay checked.
      writer.append("var name = type.name;");
      writer.append("if (typeof window === 'undefined' || window[name] !== type || ");
      writer.append("!(/^(HTML.*Element|HTMLElement|HTMLDocument|Document|Element|Node|Text|Comment)$/.test(name))) return false;");
      writer.append("var doc = value.nodeType === 9 ? value : value.ownerDocument;");
      writer.append("var view = doc && doc.defaultView;");
      writer.append("return !!(view && typeof view[name] === 'function' && value instanceof view[name]);");
      writer.append("})(");
      context.writeExpr(context.getArgument(0));
      writer.append(",");
      context.writeExpr(context.getArgument(1));
      writer.append(")");
    }
  }
}
