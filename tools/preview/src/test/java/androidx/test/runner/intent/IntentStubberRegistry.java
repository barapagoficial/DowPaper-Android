// Stub mínimo de androidx.test (solo Google Maven) para ejecutar Robolectric sin acceso a ese repositorio.
package androidx.test.runner.intent;
public final class IntentStubberRegistry {
  public static boolean isLoaded() { return false; }
  public static IntentStubber getInstance() { return null; }
}
