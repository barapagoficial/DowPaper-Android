// Stub mínimo de androidx.test (solo Google Maven) para ejecutar Robolectric sin acceso a ese repositorio.
package androidx.test.espresso;
public interface IdlingResource {
  String getName();
  boolean isIdleNow();
  void registerIdleTransitionCallback(ResourceCallback cb);
  interface ResourceCallback { void onTransitionToIdle(); }
}
