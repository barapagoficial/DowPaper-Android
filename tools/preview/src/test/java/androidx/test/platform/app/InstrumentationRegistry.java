// Stub mínimo de androidx.test (solo Google Maven) para ejecutar Robolectric sin acceso a ese repositorio.
package androidx.test.platform.app;
public final class InstrumentationRegistry {
  private static volatile android.app.Instrumentation inst;
  public static void registerInstance(android.app.Instrumentation i, android.os.Bundle args) { inst = i; }
  public static android.app.Instrumentation getInstrumentation() { return inst; }
}
