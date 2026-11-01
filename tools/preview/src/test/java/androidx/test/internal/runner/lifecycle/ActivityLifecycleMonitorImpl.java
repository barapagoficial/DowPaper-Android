// Stub mínimo de androidx.test (solo Google Maven) para ejecutar Robolectric sin acceso a ese repositorio.
package androidx.test.internal.runner.lifecycle;
import android.app.Activity;
import androidx.test.runner.lifecycle.*;
import java.util.*;
public class ActivityLifecycleMonitorImpl implements ActivityLifecycleMonitor {
  private final Map<Activity, Stage> stages = new WeakHashMap<>();
  private final List<ActivityLifecycleCallback> cbs = new ArrayList<>();
  public ActivityLifecycleMonitorImpl() {}
  public void addLifecycleCallback(ActivityLifecycleCallback cb) { cbs.add(cb); }
  public void removeLifecycleCallback(ActivityLifecycleCallback cb) { cbs.remove(cb); }
  public Collection<Activity> getActivitiesInStage(Stage stage) {
    List<Activity> r = new ArrayList<>();
    for (Map.Entry<Activity, Stage> e : stages.entrySet()) if (e.getValue() == stage) r.add(e.getKey());
    return r;
  }
  public Stage getLifecycleStageOf(Activity a) { return stages.get(a); }
  public void signalLifecycleChange(Stage stage, Activity a) {
    stages.put(a, stage);
    for (ActivityLifecycleCallback cb : new ArrayList<>(cbs)) cb.onActivityLifecycleChanged(a, stage);
  }
}
