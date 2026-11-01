// Stub mínimo de androidx.test (solo Google Maven) para ejecutar Robolectric sin acceso a ese repositorio.
package androidx.test.runner.lifecycle;
public final class ActivityLifecycleMonitorRegistry {
  private static volatile ActivityLifecycleMonitor instance;
  public static ActivityLifecycleMonitor getInstance() { return instance; }
  public static void registerInstance(ActivityLifecycleMonitor m) { instance = m; }
}
