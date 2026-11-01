// Stub mínimo de androidx.test (solo Google Maven) para ejecutar Robolectric sin acceso a ese repositorio.
package androidx.test.runner.lifecycle;
import android.app.Activity;
import java.util.Collection;
public interface ActivityLifecycleMonitor {
  void addLifecycleCallback(ActivityLifecycleCallback cb);
  void removeLifecycleCallback(ActivityLifecycleCallback cb);
  Collection<Activity> getActivitiesInStage(Stage stage);
  Stage getLifecycleStageOf(Activity activity);
}
