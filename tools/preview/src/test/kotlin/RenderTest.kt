import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import androidx.test.core.app.ApplicationProvider
import com.dowpaper.android.core.Catalog
import com.dowpaper.android.core.Prefs
import com.dowpaper.android.ui.MainActivity
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowLooper
import java.io.File
import java.io.FileOutputStream
import java.time.Duration

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xxhdpi")
class RenderTest {
    private val out = File(System.getProperty("preview.out") ?: "screens").also { it.mkdirs() }

    private fun save(b: Bitmap, name: String) {
        FileOutputStream(File(out, "$name.png")).use { b.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    @Test
    fun renderScenes() {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        val prefs = Prefs(ctx)
        val w = 1080; val h = 2340
        for (def in Catalog.all) {
            val s = Catalog.create(ctx, def, prefs)
            s.resize(w, h)
            val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            val c = Canvas(bmp)
            val t0 = System.nanoTime()
            for (i in 0 until 240) {
                if (i == 120) s.touch(w * 0.5f, h * 0.45f, android.view.MotionEvent.ACTION_DOWN)
                if (i == 140) s.touch(w * 0.5f, h * 0.45f, android.view.MotionEvent.ACTION_UP)
                c.drawColor(0, android.graphics.PorterDuff.Mode.CLEAR)
                s.frame(c, 1f / 30f)
            }
            val ms = (System.nanoTime() - t0) / 1e6 / 240
            println("SCENE ${def.id}: ok, ${"%.2f".format(ms)} ms/frame (software JVM)")
            save(Bitmap.createScaledBitmap(bmp, 540, 1170, true), "scene_${def.id}")
        }
    }

    private fun shot(act: MainActivity, name: String) {
        val root = act.window.decorView
        val b = Bitmap.createBitmap(root.width, root.height, Bitmap.Config.ARGB_8888)
        root.draw(Canvas(b))
        save(b, name)
    }

    private fun advance(ms: Long) {
        var left = ms
        while (left > 0) { ShadowLooper.idleMainLooper(16, java.util.concurrent.TimeUnit.MILLISECONDS); left -= 16 }
    }

    private fun findView(v: View, pred: (View) -> Boolean): View? {
        if (pred(v)) return v
        if (v is android.view.ViewGroup) for (i in 0 until v.childCount) findView(v.getChildAt(i), pred)?.let { return it }
        return null
    }

    @Test
    fun renderScreens() {
        val ctl = Robolectric.buildActivity(MainActivity::class.java).setup()
        val act = ctl.get()
        advance(300); shot(act, "ui_splash")
        advance(2600); shot(act, "ui_home")
        val card = findView(act.window.decorView) { it is com.dowpaper.android.ui.PreviewCard && it.def.id == "aurora" }!!
        card.performClick()
        advance(1200); shot(act, "ui_detail_aurora")
        act.onBackPressed(); advance(800)
        val photo = findView(act.window.decorView) { it is com.dowpaper.android.ui.PreviewCard && it.def.id == "photo" }!!
        photo.performClick(); advance(1200); shot(act, "ui_detail_photo")
        act.onBackPressed(); advance(800)
        act.openSettings(); advance(1000); shot(act, "ui_settings")
        act.onBackPressed(); advance(800)
        act.toast("¡Fondo actualizado!", com.dowpaper.android.ui.Ic.SPARK); advance(600); shot(act, "ui_toast")
        ctl.pause().stop().destroy()
    }
}
