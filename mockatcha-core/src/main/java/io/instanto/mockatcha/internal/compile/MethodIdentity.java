package io.instanto.mockatcha.internal.compile;

import java.util.List;
import org.teavm.extension.introspect.IntrospectMethod;
import org.teavm.extension.introspect.IntrospectParameter;

/** Names a method for the runtime, in a form that separates overloads. */
public final class MethodIdentity {

  private MethodIdentity() {}

  /** Produces an identifier such as {@code total(java.lang.String,int)}. */
  public static String of(IntrospectMethod method) {
    StringBuilder identifier = new StringBuilder(method.name()).append('(');
    List<? extends IntrospectParameter> parameters = method.parameters();
    for (int index = 0; index < parameters.size(); index++) {
      if (index > 0) {
        identifier.append(',');
      }
      identifier.append(parameters.get(index).type().name());
    }
    return identifier.append(')').toString();
  }
}
