// Stub mínimo de androidx.test (solo Google Maven) para ejecutar Robolectric sin acceso a ese repositorio.
package androidx.test.internal.runner.intent;
public class IntentMonitorImpl implements androidx.test.runner.intent.IntentMonitor {
  public IntentMonitorImpl() {}
  public void signalIntent(android.content.Intent i) {}
}
