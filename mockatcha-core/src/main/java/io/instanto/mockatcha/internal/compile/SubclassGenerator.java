package io.instanto.mockatcha.internal.compile;

import io.instanto.mockatcha.internal.MockRuntime;
import io.instanto.mockatcha.internal.MockState;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import org.teavm.asm.ClassWriter;
import org.teavm.asm.Label;
import org.teavm.asm.MethodVisitor;
import org.teavm.asm.Opcodes;
import org.teavm.extension.introspect.IntrospectClass;
import org.teavm.extension.introspect.IntrospectMethod;
import org.teavm.extension.introspect.IntrospectParameter;
import org.teavm.metaprogramming.MetaprogrammingEnvironment;

/**
 * Generates the subclass that lets Mockatcha mock or spy on an ordinary Java class.
 *
 * <h2>Why a subclass is generated rather than proxied</h2>
 *
 * <p>TeaVM's {@code proxy} operation can make a generated class extend a class rather than
 * implement an interface, but it supplies bodies only for methods it finds abstract. A concrete
 * class therefore yields a proxy that inherits every real method and intercepts nothing.
 *
 * <p>The obvious repair — generating an abstract shim subclass that redeclares each method as
 * abstract, then proxying that — does not work on TeaVM 0.15. {@code proxy} resolves its argument
 * through the compiler's <em>unprocessed</em> class source, while a class submitted through
 * {@code createClass} is only visible in the processed one, so the shim is invisible to the very
 * operation that would consume it.
 *
 * <p>So this generator emits the whole subclass, including method bodies, and the metaprogram
 * instantiates it through {@code Metaprogramming.caller(constructor).construct(...)}. Bodies are
 * deliberately thin: each one packs its arguments, asks {@link MockRuntime} what to do, and either
 * calls the real object or converts the runtime's answer. Every decision stays in ordinary Java.
 *
 * <p>Everything in this class runs inside the TeaVM compiler. None of it reaches the browser.
 */
public final class SubclassGenerator {

  private static final String STATE = internalName(MockState.class);
  private static final String RUNTIME = internalName(MockRuntime.class);
  private static final String OBJECT = "java/lang/Object";
  private static final String STATE_FIELD = "mockatchaState";
  private static final String DELEGATE_FIELD = "mockatchaDelegate";

  /** Object methods are handled deliberately rather than by discovery, so they never appear here. */
  private static final Set<String> OBJECT_METHODS =
      Set.of(
          "equals(Ljava/lang/Object;)Z",
          "hashCode()I",
          "toString()Ljava/lang/String;",
          "clone()Ljava/lang/Object;",
          "finalize()V",
          "getClass()Ljava/lang/Class;",
          "notify()V",
          "notifyAll()V",
          "wait()V",
          "wait(J)V",
          "wait(JI)V");

  private static final Map<MetaprogrammingEnvironment, Map<String, GeneratedSubclass>> GENERATED =
      new WeakHashMap<>();

  private SubclassGenerator() {}

  /** A generated subclass and the constructor a metaprogram calls to instantiate it. */
  public static final class GeneratedSubclass {
    private final IntrospectClass<?> type;
    private final IntrospectMethod constructor;

    private GeneratedSubclass(IntrospectClass<?> type, IntrospectMethod constructor) {
      this.type = type;
      this.constructor = constructor;
    }

    public IntrospectClass<?> type() {
      return type;
    }

    /** Takes the runtime state and the real object a spy delegates to, which is null for a mock. */
    public IntrospectMethod constructor() {
      return constructor;
    }
  }

  /**
   * Explains why a type cannot be mocked as a class, or returns null when it can.
   *
   * <p>The caller reports this while TeaVM compiles the test. A compile-time failure is more useful
   * than a partly working object in the browser.
   */
  public static String rejectionReason(IntrospectClass<?> type) {
    String name = type.name();
    if (type.isPrimitive() || type.isArray()) {
      return name + " is not a class";
    }
    if (type.isInterface()) {
      return name + " is an interface";
    }
    if (type.isAnnotation()) {
      return name + " is an annotation type";
    }
    if (type.isEnum()) {
      return name + " is an enum";
    }
    if (type.isRecord()) {
      return name + " is a record, and records are final";
    }
    if (Modifier.isFinal(type.modifiers())) {
      return name + " is final and cannot be subclassed";
    }
    if (isAnonymous(name)) {
      return name + " is an anonymous or local class";
    }
    IntrospectMethod constructor = type.declaredMethod("<init>");
    if (constructor == null) {
      return name
          + " has no no-argument constructor, and Mockatcha cannot allocate an object without"
          + " running one";
    }
    if (Modifier.isPrivate(constructor.modifiers())) {
      return name + " has a private no-argument constructor";
    }
    return null;
  }

  /**
   * Returns the subclass for a mocked type, generating it once per TeaVM compilation.
   *
   * <p>The name is derived from the mocked type so that an incremental build reuses the same class
   * graph. One generated class serves both mocks and spies; each instance still receives its own
   * runtime state, and a mock is simply an instance whose delegate is null.
   */
  public static GeneratedSubclass subclassOf(
      MetaprogrammingEnvironment environment, IntrospectClass<?> type) {
    Map<String, GeneratedSubclass> generated =
        GENERATED.computeIfAbsent(environment, key -> new HashMap<>());
    return generated.computeIfAbsent(type.name(), name -> generate(environment, type));
  }

  private static GeneratedSubclass generate(
      MetaprogrammingEnvironment environment, IntrospectClass<?> type) {
    String target = type.name().replace('.', '/');
    String generated = target + "$Mockatcha";

    ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
    // Java 5 bytecode keeps the generated methods free of stack map frames, which would otherwise
    // have to be computed for every branch by loading the mocked class's whole hierarchy.
    writer.visit(
        Opcodes.V1_5,
        Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL | Opcodes.ACC_SUPER,
        generated,
        null,
        target,
        null);

    writer
        .visitField(Opcodes.ACC_PRIVATE | Opcodes.ACC_FINAL, STATE_FIELD, "L" + STATE + ";", null,
            null)
        .visitEnd();
    writer
        .visitField(Opcodes.ACC_PRIVATE | Opcodes.ACC_FINAL, DELEGATE_FIELD, "L" + target + ";",
            null, null)
        .visitEnd();

    writeConstructor(writer, generated, target);

    for (IntrospectMethod method : overridableMethods(type)) {
      writeOverride(writer, generated, target, method);
    }
    writeObjectMethods(writer, generated, type);

    writer.visitEnd();

    IntrospectClass<?> subclass = environment.createClass(writer.toByteArray());
    IntrospectMethod constructor =
        subclass.declaredMethod("<init>", environment.findClass(MockState.class), type);
    return new GeneratedSubclass(subclass, constructor);
  }

  private static void writeConstructor(ClassWriter writer, String generated, String target) {
    MethodVisitor visitor =
        writer.visitMethod(
            Opcodes.ACC_PUBLIC, "<init>", "(L" + STATE + ";L" + target + ";)V", null, null);
    visitor.visitCode();
    visitor.visitVarInsn(Opcodes.ALOAD, 0);
    visitor.visitMethodInsn(Opcodes.INVOKESPECIAL, target, "<init>", "()V", false);
    visitor.visitVarInsn(Opcodes.ALOAD, 0);
    visitor.visitVarInsn(Opcodes.ALOAD, 1);
    visitor.visitFieldInsn(Opcodes.PUTFIELD, generated, STATE_FIELD, "L" + STATE + ";");
    visitor.visitVarInsn(Opcodes.ALOAD, 0);
    visitor.visitVarInsn(Opcodes.ALOAD, 2);
    visitor.visitFieldInsn(Opcodes.PUTFIELD, generated, DELEGATE_FIELD, "L" + target + ";");
    visitor.visitInsn(Opcodes.RETURN);
    visitor.visitMaxs(0, 0);
    visitor.visitEnd();
  }

  /**
   * Emits one overriding method.
   *
   * <p>In Java the body reads:
   *
   * <pre>{@code
   * Object result = MockRuntime.dispatchSpy(mockatchaState, "total(java.lang.String)",
   *         new Object[] { account });
   * if (MockRuntime.callsReal(result, mockatchaDelegate)) {
   *     return mockatchaDelegate.total(account);
   * }
   * return MockRuntime.toInt(result);
   * }</pre>
   */
  private static void writeOverride(
      ClassWriter writer, String generated, String target, IntrospectMethod method) {
    List<? extends IntrospectParameter> parameters = method.parameters();
    String descriptor = descriptorOf(method);
    MethodVisitor visitor =
        writer.visitMethod(
            Opcodes.ACC_PUBLIC, method.name(), descriptor, null, exceptionsOf(method));
    visitor.visitCode();

    int[] slots = new int[parameters.size()];
    int nextSlot = 1;
    for (int index = 0; index < parameters.size(); index++) {
      slots[index] = nextSlot;
      nextSlot += slotSize(parameters.get(index).type());
    }
    int argumentsSlot = nextSlot++;
    int resultSlot = nextSlot++;

    visitor.visitIntInsn(Opcodes.BIPUSH, parameters.size());
    visitor.visitTypeInsn(Opcodes.ANEWARRAY, OBJECT);
    for (int index = 0; index < parameters.size(); index++) {
      IntrospectClass<?> parameterType = parameters.get(index).type();
      visitor.visitInsn(Opcodes.DUP);
      visitor.visitIntInsn(Opcodes.BIPUSH, index);
      visitor.visitVarInsn(loadOpcode(parameterType), slots[index]);
      box(visitor, parameterType);
      visitor.visitInsn(Opcodes.AASTORE);
    }
    visitor.visitVarInsn(Opcodes.ASTORE, argumentsSlot);

    visitor.visitVarInsn(Opcodes.ALOAD, 0);
    visitor.visitFieldInsn(Opcodes.GETFIELD, generated, STATE_FIELD, "L" + STATE + ";");
    visitor.visitLdcInsn(MethodIdentity.of(method));
    visitor.visitVarInsn(Opcodes.ALOAD, argumentsSlot);
    visitor.visitMethodInsn(
        Opcodes.INVOKESTATIC,
        RUNTIME,
        "dispatchSpy",
        "(L" + STATE + ";Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/Object;",
        false);
    visitor.visitVarInsn(Opcodes.ASTORE, resultSlot);

    Label answerFromRuntime = new Label();
    visitor.visitVarInsn(Opcodes.ALOAD, resultSlot);
    visitor.visitVarInsn(Opcodes.ALOAD, 0);
    visitor.visitFieldInsn(Opcodes.GETFIELD, generated, DELEGATE_FIELD, "L" + target + ";");
    visitor.visitMethodInsn(
        Opcodes.INVOKESTATIC,
        RUNTIME,
        "callsReal",
        "(Ljava/lang/Object;Ljava/lang/Object;)Z",
        false);
    visitor.visitJumpInsn(Opcodes.IFEQ, answerFromRuntime);

    visitor.visitVarInsn(Opcodes.ALOAD, 0);
    visitor.visitFieldInsn(Opcodes.GETFIELD, generated, DELEGATE_FIELD, "L" + target + ";");
    for (int index = 0; index < parameters.size(); index++) {
      visitor.visitVarInsn(loadOpcode(parameters.get(index).type()), slots[index]);
    }
    visitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, target, method.name(), descriptor, false);
    visitor.visitInsn(returnOpcode(method.returnType()));

    visitor.visitLabel(answerFromRuntime);
    emitRuntimeAnswer(visitor, method.returnType(), resultSlot);

    visitor.visitMaxs(0, 0);
    visitor.visitEnd();
  }

  private static void emitRuntimeAnswer(
      MethodVisitor visitor, IntrospectClass<?> returnType, int resultSlot) {
    if (returnType.name().equals("void")) {
      visitor.visitInsn(Opcodes.RETURN);
      return;
    }
    visitor.visitVarInsn(Opcodes.ALOAD, resultSlot);
    String conversion = conversionFor(returnType);
    if (conversion == null) {
      visitor.visitMethodInsn(
          Opcodes.INVOKESTATIC, RUNTIME, "toObject", "(Ljava/lang/Object;)Ljava/lang/Object;",
          false);
      visitor.visitTypeInsn(Opcodes.CHECKCAST, castTargetOf(returnType));
    } else {
      visitor.visitMethodInsn(
          Opcodes.INVOKESTATIC,
          RUNTIME,
          conversion,
          "(Ljava/lang/Object;)" + descriptorOf(returnType),
          false);
    }
    visitor.visitInsn(returnOpcode(returnType));
  }

  /**
   * Emits {@code equals}, {@code hashCode}, and {@code toString}.
   *
   * <p>These are answered directly rather than routed through the runtime, so state lookup stays
   * keyed on identity and a logged mock never runs the mocked class's own code against an object
   * whose fields a test never arranged. A class that makes one of them final keeps its own.
   */
  private static void writeObjectMethods(
      ClassWriter writer, String generated, IntrospectClass<?> type) {
    Set<String> unavailable = finalMethods(type);

    if (!unavailable.contains("equals(Ljava/lang/Object;)Z")) {
      MethodVisitor equals =
          writer.visitMethod(Opcodes.ACC_PUBLIC, "equals", "(Ljava/lang/Object;)Z", null, null);
      equals.visitCode();
      equals.visitVarInsn(Opcodes.ALOAD, 0);
      equals.visitVarInsn(Opcodes.ALOAD, 1);
      equals.visitMethodInsn(
          Opcodes.INVOKESTATIC, RUNTIME, "sameInstance",
          "(Ljava/lang/Object;Ljava/lang/Object;)Z", false);
      equals.visitInsn(Opcodes.IRETURN);
      equals.visitMaxs(0, 0);
      equals.visitEnd();
    }

    if (!unavailable.contains("hashCode()I")) {
      MethodVisitor hashCode = writer.visitMethod(Opcodes.ACC_PUBLIC, "hashCode", "()I", null, null);
      hashCode.visitCode();
      hashCode.visitVarInsn(Opcodes.ALOAD, 0);
      hashCode.visitMethodInsn(
          Opcodes.INVOKESTATIC, RUNTIME, "identityHashCode", "(Ljava/lang/Object;)I", false);
      hashCode.visitInsn(Opcodes.IRETURN);
      hashCode.visitMaxs(0, 0);
      hashCode.visitEnd();
    }

    if (!unavailable.contains("toString()Ljava/lang/String;")) {
      MethodVisitor toString =
          writer.visitMethod(Opcodes.ACC_PUBLIC, "toString", "()Ljava/lang/String;", null, null);
      toString.visitCode();
      toString.visitVarInsn(Opcodes.ALOAD, 0);
      toString.visitFieldInsn(Opcodes.GETFIELD, generated, STATE_FIELD, "L" + STATE + ";");
      toString.visitMethodInsn(
          Opcodes.INVOKESTATIC, RUNTIME, "describe", "(L" + STATE + ";)Ljava/lang/String;", false);
      toString.visitInsn(Opcodes.ARETURN);
      toString.visitMaxs(0, 0);
      toString.visitEnd();
    }
  }

  /**
   * Collects the methods a generated subclass may legally override.
   *
   * <p>Static, private, and final methods cannot be overridden. Package-private methods wait until
   * generated-package placement has explicit tests. A bridge method is left in place so the real
   * bridge forwards to the override, which keeps one stub per source-level method.
   */
  static List<IntrospectMethod> overridableMethods(IntrospectClass<?> type) {
    Map<String, IntrospectMethod> chosen = new LinkedHashMap<>();
    for (IntrospectMethod method : type.methods()) {
      if (method.isConstructor() || method.name().equals("<clinit>")) {
        continue;
      }
      int modifiers = method.modifiers();
      if (Modifier.isStatic(modifiers)
          || Modifier.isPrivate(modifiers)
          || Modifier.isFinal(modifiers)) {
        continue;
      }
      if (!Modifier.isPublic(modifiers) && !Modifier.isProtected(modifiers)) {
        continue;
      }
      if (OBJECT_METHODS.contains(method.name() + descriptorOf(method))) {
        continue;
      }

      String signature = method.name() + parameterDescriptors(method);
      IntrospectMethod previous = chosen.get(signature);
      if (previous == null || isBridgeOf(previous, method)) {
        chosen.put(signature, method);
      }
    }
    return new ArrayList<>(chosen.values());
  }

  /**
   * Reports whether {@code candidate} is the more specific of two same-signature methods, which
   * makes {@code previous} the compiler-generated bridge.
   */
  private static boolean isBridgeOf(IntrospectMethod previous, IntrospectMethod candidate) {
    IntrospectClass<?> previousReturn = previous.returnType();
    IntrospectClass<?> candidateReturn = candidate.returnType();
    return !previousReturn.name().equals(candidateReturn.name())
        && previousReturn.isAssignableFrom(candidateReturn);
  }

  private static Set<String> finalMethods(IntrospectClass<?> type) {
    Set<String> unavailable = new HashSet<>();
    for (IntrospectMethod method : type.methods()) {
      if (Modifier.isFinal(method.modifiers())) {
        unavailable.add(method.name() + descriptorOf(method));
      }
    }
    return unavailable;
  }

  static String descriptorOf(IntrospectMethod method) {
    return parameterDescriptors(method) + descriptorOf(method.returnType());
  }

  private static String parameterDescriptors(IntrospectMethod method) {
    StringBuilder descriptor = new StringBuilder("(");
    for (IntrospectParameter parameter : method.parameters()) {
      descriptor.append(descriptorOf(parameter.type()));
    }
    return descriptor.append(')').toString();
  }

  private static String descriptorOf(IntrospectClass<?> type) {
    if (type.isArray()) {
      return "[" + descriptorOf(type.componentType());
    }
    switch (type.name()) {
      case "void":
        return "V";
      case "boolean":
        return "Z";
      case "byte":
        return "B";
      case "short":
        return "S";
      case "char":
        return "C";
      case "int":
        return "I";
      case "long":
        return "J";
      case "float":
        return "F";
      case "double":
        return "D";
      default:
        return "L" + type.name().replace('.', '/') + ";";
    }
  }

  private static String castTargetOf(IntrospectClass<?> type) {
    return type.isArray() ? descriptorOf(type) : type.name().replace('.', '/');
  }

  private static String conversionFor(IntrospectClass<?> type) {
    if (type.isArray()) {
      return null;
    }
    switch (type.name()) {
      case "boolean":
        return "toBoolean";
      case "byte":
        return "toByte";
      case "short":
        return "toShort";
      case "char":
        return "toChar";
      case "int":
        return "toInt";
      case "long":
        return "toLong";
      case "float":
        return "toFloat";
      case "double":
        return "toDouble";
      default:
        return null;
    }
  }

  private static void box(MethodVisitor visitor, IntrospectClass<?> type) {
    if (type.isArray()) {
      return;
    }
    String boxed;
    switch (type.name()) {
      case "boolean":
        boxed = "java/lang/Boolean";
        break;
      case "byte":
        boxed = "java/lang/Byte";
        break;
      case "short":
        boxed = "java/lang/Short";
        break;
      case "char":
        boxed = "java/lang/Character";
        break;
      case "int":
        boxed = "java/lang/Integer";
        break;
      case "long":
        boxed = "java/lang/Long";
        break;
      case "float":
        boxed = "java/lang/Float";
        break;
      case "double":
        boxed = "java/lang/Double";
        break;
      default:
        return;
    }
    visitor.visitMethodInsn(
        Opcodes.INVOKESTATIC,
        boxed,
        "valueOf",
        "(" + descriptorOf(type) + ")L" + boxed + ";",
        false);
  }

  private static int loadOpcode(IntrospectClass<?> type) {
    if (type.isArray()) {
      return Opcodes.ALOAD;
    }
    switch (type.name()) {
      case "boolean":
      case "byte":
      case "short":
      case "char":
      case "int":
        return Opcodes.ILOAD;
      case "long":
        return Opcodes.LLOAD;
      case "float":
        return Opcodes.FLOAD;
      case "double":
        return Opcodes.DLOAD;
      default:
        return Opcodes.ALOAD;
    }
  }

  private static int returnOpcode(IntrospectClass<?> type) {
    if (type.isArray()) {
      return Opcodes.ARETURN;
    }
    switch (type.name()) {
      case "void":
        return Opcodes.RETURN;
      case "boolean":
      case "byte":
      case "short":
      case "char":
      case "int":
        return Opcodes.IRETURN;
      case "long":
        return Opcodes.LRETURN;
      case "float":
        return Opcodes.FRETURN;
      case "double":
        return Opcodes.DRETURN;
      default:
        return Opcodes.ARETURN;
    }
  }

  private static int slotSize(IntrospectClass<?> type) {
    if (type.isArray()) {
      return 1;
    }
    return type.name().equals("long") || type.name().equals("double") ? 2 : 1;
  }

  private static String[] exceptionsOf(IntrospectMethod method) {
    List<? extends IntrospectClass<? extends Throwable>> thrown = method.exceptionTypes();
    if (thrown.isEmpty()) {
      return null;
    }
    return thrown.stream().map(type -> type.name().replace('.', '/')).toArray(String[]::new);
  }

  private static boolean isAnonymous(String name) {
    int lastSeparator = name.lastIndexOf('$');
    if (lastSeparator < 0 || lastSeparator == name.length() - 1) {
      return false;
    }
    for (int index = lastSeparator + 1; index < name.length(); index++) {
      if (!Character.isDigit(name.charAt(index))) {
        return false;
      }
    }
    return true;
  }

  private static String internalName(Class<?> type) {
    return type.getName().replace('.', '/');
  }
}
