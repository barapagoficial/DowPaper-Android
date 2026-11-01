// Stub mínimo de androidx.test (solo Google Maven) para ejecutar Robolectric sin acceso a ese repositorio.
package androidx.test.internal.runner.lifecycle;
import androidx.test.runner.lifecycle.*;
public class ApplicationLifecycleMonitorImpl implements ApplicationLifecycleMonitor {
  public ApplicationLifecycleMonitorImpl() {}
  public void signalLifecycleChange(android.app.Application app, ApplicationStage stage) {}
}
