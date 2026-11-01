package com.dowpaper.android.ui

import android.app.Activity
import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.WindowInsets
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout
import android.widget.LinearLayout
import com.dowpaper.android.core.*
import com.dowpaper.android.scenes.FluidScene
import com.dowpaper.android.wallpaper.DowPaperService
import com.dowpaper.android.wallpaper.DowVideoService

class MainActivity : Activity() {
    lateinit var prefs: Prefs
    lateinit var backdrop: SceneView
    private lateinit var root: FrameLayout
    private lateinit var host: FrameLayout
    private lateinit var home: HomeScreen
    private val stack = ArrayList<Screen>()
    private var insetTop = 0
    private var insetBottom = 0
    private var pendingApply: ((Boolean) -> Unit)? = null
    private var toastView: View? = null

    val screenW: Float get() = resources.displayMetrics.widthPixels.toFloat()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = Prefs(this)
        setupWindow()

        root = FrameLayout(this)
        root.setBackgroundColor(T.BG)
        backdrop = SceneView(this).apply {
            scene = FluidScene(this@MainActivity).also { it.configure(Palettes.get(3), 0.45f, 0.9f); it.touchEnabled = false }
            maxFps = 30
            alpha = 0.7f
        }
        root.addView(backdrop, flp(MATCH, MATCH, Gravity.CENTER))
        root.addView(View(this).apply {
            background = GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, intArrayOf(0x9907070D.toInt(), 0xE607070D.toInt(), 0xFA07070D.toInt()))
        }, flp(MATCH, MATCH, Gravity.CENTER))
        host = FrameLayout(this)
        root.addView(host, flp(MATCH, MATCH, Gravity.CENTER))

        root.setOnApplyWindowInsetsListener { _, ins ->
            if (Build.VERSION.SDK_INT >= 30) {
                val i = ins.getInsets(WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout())
                insetTop = i.top; insetBottom = i.bottom
            } else {
                @Suppress("DEPRECATION")
                run { insetTop = ins.systemWindowInsetTop; insetBottom = ins.systemWindowInsetBottom }
            }
            for (s in stack) s.onInsets(insetTop, insetBottom)
            ins
        }
        setContentView(root)

        Thread { MediaImport.migrate(applicationContext, prefs) }.start()
        home = HomeScreen(this)
        push(home, false)
        if (savedInstanceState == null) showSplash() else home.entrance()
    }

    @Suppress("DEPRECATION")
    private fun setupWindow() {
        val w = window
        if (Build.VERSION.SDK_INT >= 30) w.setDecorFitsSystemWindows(false)
        else w.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
        w.statusBarColor = Color.TRANSPARENT
        w.navigationBarColor = Color.TRANSPARENT
        if (Build.VERSION.SDK_INT >= 29) { w.isNavigationBarContrastEnforced = false; w.isStatusBarContrastEnforced = false }
        if (Build.VERSION.SDK_INT >= 28) w.attributes = w.attributes.apply { layoutInDisplayCutoutMode = android.view.WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES }
    }

    private fun showSplash() {
        val sp = SplashView(this)
        sp.isClickable = true
        root.addView(sp, flp(MATCH, MATCH, Gravity.CENTER))
        sp.postDelayed({
            sp.animate().alpha(0f).scaleX(1.08f).scaleY(1.08f).setDuration(450).setInterpolator(DecelerateInterpolator())
                .withEndAction { root.removeView(sp) }.start()
            home.entrance()
        }, 1500)
    }

    override fun onStart() { super.onStart(); SceneView.appVisible = true }
    override fun onStop() { SceneView.appVisible = false; super.onStop() }

    override fun onPause() {
        stack.lastOrNull()?.onHide()
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        stack.lastOrNull()?.onShow()
    }

    // ── Navegación ──

    private fun push(s: Screen, animate: Boolean = true) {
        host.addView(s.view, flp(MATCH, MATCH, Gravity.CENTER))
        s.onInsets(insetTop, insetBottom)
        val prev = stack.lastOrNull()
        stack.add(s)
        if (animate) {
            s.view.alpha = 0f; s.view.translationY = dp(36f)
            s.view.animate().alpha(1f).translationY(0f).setDuration(380).setInterpolator(DecelerateInterpolator(2f)).start()
            prev?.let { p ->
                p.view.animate().alpha(0f).scaleX(0.96f).scaleY(0.96f).setDuration(300).withEndAction {
                    if (stack.lastOrNull() !== p) p.view.visibility = View.INVISIBLE
                }.start()
                p.onHide()
            }
        }
        s.onShow()
        root.requestApplyInsets()
    }

    private fun pop(): Boolean {
        if (stack.size <= 1) return false
        val top = stack.removeAt(stack.size - 1)
        val prev = stack.last()
        prev.view.visibility = View.VISIBLE
        prev.view.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(320).setInterpolator(DecelerateInterpolator(2f)).start()
        top.onHide()
        top.view.animate().alpha(0f).translationY(dp(40f)).setDuration(260).withEndAction {
            host.removeView(top.view); top.onDestroy()
        }.start()
        prev.onShow()
        return true
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (stack.lastOrNull()?.onBack() == true) return
        if (!pop()) @Suppress("DEPRECATION") super.onBackPressed()
    }

    fun openDetail(def: SceneDef) { if (stack.last() is HomeScreen) push(DetailScreen(this, def)) }
    fun openSettings() { if (stack.last() is HomeScreen) push(SettingsScreen(this)) }
    fun onFavoritesChanged() = home.onFavoritesChanged()

    // ── Fondo de pantalla ──

    /** Nombre de clase del servicio de DowPaper activo, o null si el fondo actual no es nuestro. */
    fun activeService(): String? = try {
        val info = WallpaperManager.getInstance(this).wallpaperInfo
        if (info?.packageName == packageName) info.serviceName else null
    } catch (e: Exception) { null }

    fun isActiveWallpaper(): Boolean = activeService() != null

    /** Escena que se está mostrando como fondo ahora mismo (o null). */
    fun activeDef(): SceneDef? = when (activeService()) {
        DowVideoService::class.java.name -> Catalog.byId("video")
        DowPaperService::class.java.name -> Catalog.byId(prefs.active).let { if (it.kind == Kind.VIDEO) Catalog.all[0] else it }
        else -> null
    }

    fun isActive(def: SceneDef): Boolean = activeDef()?.id == def.id

    fun applyWallpaper(def: SceneDef, onDone: (Boolean) -> Unit) {
        val video = def.kind == Kind.VIDEO
        val service = if (video) DowVideoService::class.java else DowPaperService::class.java
        if (!video) prefs.active = def.id
        prefs.lastRotate = System.currentTimeMillis()
        if (activeService() == service.name) {
            toast("¡Fondo actualizado!", Ic.SPARK)
            onDone(true)
            return
        }
        pendingApply = onDone
        val i = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER)
            .putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT, ComponentName(this, service))
        try {
            @Suppress("DEPRECATION") startActivityForResult(i, REQ_APPLY)
        } catch (e: Exception) {
            openSystemChooser()
        }
    }

    fun openSystemChooser() {
        try {
            @Suppress("DEPRECATION") startActivityForResult(Intent(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER), REQ_APPLY)
            toast("Elige DowPaper en la lista", Ic.LAYERS)
        } catch (e: Exception) {
            toast("Tu dispositivo no admite fondos animados", Ic.EYE_OFF)
        }
    }

    fun pickMedia(video: Boolean) {
        val i = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = if (video) "video/*" else "image/*"
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
        }
        try {
            @Suppress("DEPRECATION") startActivityForResult(i, if (video) REQ_VIDEO else REQ_PHOTO)
        } catch (e: Exception) {
            toast("No se encontró un selector de archivos", Ic.EYE_OFF)
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        @Suppress("DEPRECATION") super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQ_APPLY -> {
                val ok = isActiveWallpaper()
                if (ok) toast("¡DowPaper activado!", Ic.SPARK)
                pendingApply?.invoke(ok)
                pendingApply = null
            }
            REQ_PHOTO, REQ_VIDEO -> {
                val uri: Uri = data?.data ?: return
                if (resultCode != RESULT_OK) return
                try { contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) } catch (e: Exception) {}
                val video = requestCode == REQ_VIDEO
                toast(if (video) "Importando video…" else "Importando foto…", if (video) Ic.VIDEO else Ic.IMAGE)
                Thread {
                    val local = MediaImport.import(applicationContext, uri, video)
                    runOnUiThread {
                        if (local == null) toast(if (video) "Video muy grande: se usará desde la galería" else "No se pudo copiar la foto; se usará el original", Ic.EYE_OFF)
                        stack.lastOrNull()?.onPicked(video, local ?: uri)
                    }
                }.start()
            }
        }
    }

    // ── Notificación tipo píldora ──

    fun toast(msg: String, icon: Ic = Ic.CHECK) {
        toastView?.let { root.removeView(it) }
        val pill = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
            background = glass(26f, 0xF0181628.toInt(), 0x33FFFFFF)
            setPadding(dpi(16), dpi(12), dpi(20), dpi(12))
            elevation = dp(12f)
        }
        val s = dpi(20)
        pill.addView(IconView(this, icon, if (icon == Ic.HEART_FILL) T.PINK else T.A3), LinearLayout.LayoutParams(s, s).apply { rightMargin = dpi(10) })
        pill.addView(label(msg, 14.5f, Color.WHITE, T.medium))
        root.addView(pill, flp(WRAP, WRAP, Gravity.TOP or Gravity.CENTER_HORIZONTAL, 0, insetTop + dpi(12), 0, 0))
        toastView = pill
        pill.translationY = -dp(100f); pill.alpha = 0f
        pill.animate().translationY(0f).alpha(1f).setDuration(450).setInterpolator(OvershootInterpolator(1.4f)).start()
        pill.postDelayed({
            pill.animate().translationY(-dp(100f)).alpha(0f).setDuration(300).withEndAction {
                root.removeView(pill); if (toastView === pill) toastView = null
            }.start()
        }, 2200)
    }

    fun versionName(): String = try { packageManager.getPackageInfo(packageName, 0).versionName ?: "1.0" } catch (e: Exception) { "1.0" }

    companion object {
        const val REQ_APPLY = 41
        const val REQ_PHOTO = 42
        const val REQ_VIDEO = 43
    }
}
