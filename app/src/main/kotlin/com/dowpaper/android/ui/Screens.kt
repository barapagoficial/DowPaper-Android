package com.dowpaper.android.ui

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.*
import com.dowpaper.android.core.*
import com.dowpaper.android.scenes.BitmapCache
import java.util.Locale

abstract class Screen(val act: MainActivity) {
    abstract val view: View
    open fun onInsets(top: Int, bottom: Int) {}
    open fun onShow() {}
    open fun onHide() {}
    open fun onDestroy() {}
    open fun onBack(): Boolean = false
    open fun onPicked(video: Boolean, uri: Uri) {}
    protected fun dp(v: Float) = act.dp(v)
    protected fun dpi(v: Float) = act.dp(v).toInt()
}

// ─────────────────────────────── Tarjeta de vista previa ───────────────────────────────

class PreviewCard(val act: MainActivity, val def: SceneDef) : FrameLayout(act) {
    val sv = SceneView(act)
    private val heart = IconView(act, Ic.HEART)
    private val heartBox = FrameLayout(act)
    private val badge = act.label("● ACTIVO", 10.5f, Color.WHITE, T.bold)
    private var sig = ""

    init {
        roundClip(act.dp(26f))
        sv.virtualWidth = act.screenW
        sv.maxFps = 30
        addView(sv, flp(MATCH, MATCH, Gravity.CENTER))
        val scrim = View(act).apply {
            background = GradientDrawable(GradientDrawable.Orientation.BOTTOM_TOP, intArrayOf(0xE6000000.toInt(), 0x80000000.toInt(), 0, 0))
        }
        addView(scrim, flp(MATCH, MATCH, Gravity.BOTTOM))
        val texts = LinearLayout(act).apply { orientation = LinearLayout.VERTICAL }
        val name = act.label(def.name, 18f, Color.WHITE, T.bold)
        val tag = act.label(def.tagline, 12f, T.TEXT2).apply { maxLines = 2; ellipsize = TextUtils.TruncateAt.END; setLineSpacing(0f, 1.15f) }
        texts.addView(name)
        texts.addView(tag, llp(MATCH, WRAP, t = act.dp(5f).toInt()))
        val p = act.dp(14f).toInt()
        addView(texts, flp(MATCH, WRAP, Gravity.BOTTOM, p, 0, p, p))
        heartBox.background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(0x55000000); setStroke(1, 0x30FFFFFF) }
        val hp = act.dp(8f).toInt()
        heartBox.addView(heart, flp(MATCH, MATCH, Gravity.CENTER, hp, hp, hp, hp))
        val hs = act.dp(36f).toInt()
        addView(heartBox, flp(hs, hs, Gravity.TOP or Gravity.END, 0, act.dp(10f).toInt(), act.dp(10f).toInt(), 0))
        heartBox.setOnClickListener {
            val fav = act.prefs.toggleFav(def.id)
            heart.icon = if (fav) Ic.HEART_FILL else Ic.HEART
            heart.scaleX = 0.4f; heart.scaleY = 0.4f
            heart.animate().scaleX(1f).scaleY(1f).setDuration(450).setInterpolator(OvershootInterpolator(4f)).start()
            act.onFavoritesChanged()
        }
        badge.background = GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, intArrayOf(T.A1, T.A3)).apply { cornerRadius = act.dp(20f) }
        badge.setPadding(act.dpi(10), act.dpi(5), act.dpi(10), act.dpi(5))
        badge.letterSpacing = 0.08f
        addView(badge, flp(WRAP, WRAP, Gravity.TOP or Gravity.START, act.dpi(12), act.dpi(14), 0, 0))
        foreground = GradientDrawable().apply { cornerRadius = act.dp(26f); setColor(0); setStroke(act.dpi(1).coerceAtLeast(1), 0x26FFFFFF) }
        isClickable = true
        pressable(0.96f)
        setOnClickListener { act.openDetail(def) }
        refresh()
    }

    fun signature(): String {
        val pr = act.prefs
        val base = "${pr.palette(def.id, def.defaultPalette)}|${pr.speed(def.id)}|${pr.density(def.id)}"
        return when (def.kind) {
            Kind.PHOTO -> "$base|${pr.photoUri}|${pr.photoEffect}|${pr.photoDim}"
            Kind.VIDEO -> "$base|${pr.videoUri}"
            else -> base
        }
    }

    fun refresh() {
        val s = signature()
        if (s != sig) {
            sig = s
            sv.scene = Catalog.create(act, def, act.prefs).also { it.touchEnabled = false }
        }
        heart.icon = if (act.prefs.isFav(def.id)) Ic.HEART_FILL else Ic.HEART
        badge.visibility = if (act.isActive(def)) View.VISIBLE else View.GONE
    }
}

// ─────────────────────────────── Inicio ───────────────────────────────

class HomeScreen(act: MainActivity) : Screen(act) {
    private val scroll = ScrollView(act)
    private val col = LinearLayout(act)
    private val grid = TwoColumnGrid(act, act.dpi(14))
    private val cards = ArrayList<PreviewCard>()
    private val chips = ArrayList<ChipView>()
    private var category = 0
    private val activeTitle = act.label("", 11f, T.A3, T.bold)
    private val activeName = act.label("", 17f, Color.WHITE, T.bold)
    private val activeMini = SceneView(act)
    private val countLabel = act.label("", 13f, T.TEXT3, T.medium)
    private val empty = act.label("Aún no tienes favoritos.\nToca el ♥ en cualquier fondo para guardarlo aquí.", 14f, T.TEXT3).apply { gravity = Gravity.CENTER; setLineSpacing(0f, 1.3f) }
    private val header = FrameLayout(act)
    private var activeSig = ""
    override val view: View = scroll

    init {
        scroll.isVerticalScrollBarEnabled = false
        scroll.overScrollMode = View.OVER_SCROLL_NEVER
        scroll.clipToPadding = false
        col.orientation = LinearLayout.VERTICAL
        val h = dpi(20f)
        col.setPadding(h, 0, h, 0)
        scroll.addView(col, lp(MATCH, WRAP))

        // Encabezado
        val titles = LinearLayout(act).apply { orientation = LinearLayout.VERTICAL }
        val title = GradientText(act).apply {
            text = "DowPaper"; textSize = 38f; typeface = T.black; setTextColor(Color.WHITE); includeFontPadding = false; letterSpacing = -0.01f
        }
        titles.addView(title)
        titles.addView(act.label("Fondos vivos para tu Android", 15f, T.TEXT2), llp(WRAP, WRAP, t = dpi(6f)))
        header.addView(titles, flp(WRAP, WRAP, Gravity.START or Gravity.CENTER_VERTICAL))
        val gear = CircleButton(act, Ic.GEAR)
        gear.setOnClickListener { act.openSettings() }
        header.addView(gear, flp(dpi(46f), dpi(46f), Gravity.END or Gravity.CENTER_VERTICAL))
        col.addView(header, llp(MATCH, WRAP))

        // Tarjeta del fondo activo
        val active = LinearLayout(act).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
            background = act.glass(24f, 0x1AFFFFFF, 0x26FFFFFF)
            setPadding(dpi(12f), dpi(12f), dpi(16f), dpi(12f))
            isClickable = true
        }
        active.pressable(0.97f)
        activeMini.virtualWidth = act.screenW
        activeMini.maxFps = 30
        activeMini.roundClip(dp(16f))
        active.addView(activeMini, LinearLayout.LayoutParams(dpi(58f), dpi(58f)))
        val at = LinearLayout(act).apply { orientation = LinearLayout.VERTICAL }
        activeTitle.letterSpacing = 0.14f
        at.addView(activeTitle)
        at.addView(activeName, llp(WRAP, WRAP, t = dpi(6f)))
        active.addView(at, llp(0, WRAP, 1f, l = dpi(14f)))
        active.addView(IconView(act, Ic.CHEVRON, T.TEXT2), LinearLayout.LayoutParams(dpi(22f), dpi(22f)))
        active.setOnClickListener { act.openDetail(act.activeDef() ?: Catalog.byId(act.prefs.active)) }
        col.addView(active, llp(MATCH, WRAP, t = dpi(24f)))

        // Título de sección
        val sec = FrameLayout(act)
        sec.addView(act.label("Colección", 22f, Color.WHITE, T.bold), flp(WRAP, WRAP, Gravity.START or Gravity.BOTTOM))
        sec.addView(countLabel, flp(WRAP, WRAP, Gravity.END or Gravity.BOTTOM))
        col.addView(sec, llp(MATCH, WRAP, t = dpi(28f)))

        // Chips
        val hs = HorizontalScrollView(act).apply { isHorizontalScrollBarEnabled = false; overScrollMode = View.OVER_SCROLL_NEVER; clipToPadding = false }
        val chipRow = LinearLayout(act).apply { orientation = LinearLayout.HORIZONTAL }
        for ((i, name) in Catalog.categories.withIndex()) {
            val c = ChipView(act, name, i == 0)
            c.setOnClickListener { setCategory(i) }
            chips += c
            chipRow.addView(c, llp(WRAP, WRAP, r = dpi(8f)))
        }
        hs.addView(chipRow)
        col.addView(hs, llp(MATCH, WRAP, t = dpi(14f)))

        // Rejilla
        for (d in Catalog.all) {
            val card = PreviewCard(act, d)
            cards += card
            grid.addView(card)
        }
        col.addView(grid, llp(MATCH, WRAP, t = dpi(18f)))
        empty.visibility = View.GONE
        col.addView(empty, llp(MATCH, WRAP, t = dpi(40f), b = dpi(40f)))

        val foot = act.label("DowPaper · animaciones 100% procedurales · sin anuncios", 12f, T.TEXT3).apply { gravity = Gravity.CENTER }
        col.addView(foot, llp(MATCH, WRAP, t = dpi(28f)))
        updateCount()
    }

    override fun onInsets(top: Int, bottom: Int) {
        header.setPadding(0, top + dpi(18f), 0, 0)
        scroll.setPadding(0, 0, 0, bottom + dpi(24f))
    }

    fun entrance() {
        header.alpha = 0f; header.translationY = -dp(20f)
        header.animate().alpha(1f).translationY(0f).setDuration(600).setInterpolator(DecelerateInterpolator(2f)).start()
        var i = 0
        for (k in 0 until col.childCount) {
            val v = col.getChildAt(k)
            if (v === header || v === grid) continue
            v.alpha = 0f; v.translationY = dp(24f)
            v.animate().alpha(1f).translationY(0f).setStartDelay(100L + i * 70L).setDuration(550).setInterpolator(DecelerateInterpolator(2f)).start()
            i++
        }
        animateCards(250)
    }

    private fun animateCards(baseDelay: Long) {
        var idx = 0
        for (c in cards) {
            if (c.visibility != View.VISIBLE) continue
            c.alpha = 0f; c.translationY = dp(50f); c.scaleX = 0.94f; c.scaleY = 0.94f
            c.animate().alpha(1f).translationY(0f).scaleX(1f).scaleY(1f)
                .setStartDelay(baseDelay + idx * 55L).setDuration(520).setInterpolator(DecelerateInterpolator(2.2f)).start()
            idx++
        }
    }

    private fun matches(d: SceneDef): Boolean = when (category) {
        Catalog.CAT_ALL -> true
        Catalog.CAT_FAV -> act.prefs.isFav(d.id)
        else -> d.category == category
    }

    private fun setCategory(i: Int) {
        category = i
        for ((k, c) in chips.withIndex()) c.setSel(k == i)
        applyFilter(true)
    }

    private fun applyFilter(animate: Boolean) {
        var n = 0
        for (c in cards) {
            val m = matches(c.def)
            c.visibility = if (m) View.VISIBLE else View.GONE
            if (m) n++
        }
        empty.visibility = if (n == 0) View.VISIBLE else View.GONE
        grid.requestLayout()
        updateCount(n)
        if (animate) animateCards(0)
    }

    private fun updateCount(n: Int = Catalog.all.size) {
        countLabel.text = if (n == 1) "1 fondo" else "$n fondos"
    }

    fun onFavoritesChanged() { if (category == Catalog.CAT_FAV) applyFilter(false) }

    override fun onShow() {
        act.backdrop.paused = false
        val cur = act.activeDef()
        val activeOn = cur != null
        val def = cur ?: Catalog.byId(act.prefs.active)
        activeTitle.text = if (activeOn) "ACTIVO AHORA" else "DOWPAPER NO ESTÁ ACTIVO"
        activeTitle.setTextColor(if (activeOn) T.A3 else T.PINK)
        activeName.text = if (activeOn) def.name else "Elige un fondo y toca Aplicar"
        val sig = "${def.id}|${cards.firstOrNull { it.def.id == def.id }?.signature()}"
        if (sig != activeSig) {
            activeSig = sig
            activeMini.scene = Catalog.create(act, def, act.prefs).also { it.touchEnabled = false }
        }
        for (c in cards) c.refresh()
        if (category == Catalog.CAT_FAV) applyFilter(false)
    }
}

// ─────────────────────────────── Detalle / personalización ───────────────────────────────

class DetailScreen(act: MainActivity, private val def: SceneDef) : Screen(act) {
    private val root = FrameLayout(act)
    private val sv = SceneView(act)
    private var video: VideoPreview? = null
    private val topBar = FrameLayout(act)
    private val panelScroll = ScrollView(act)
    private val panel = LinearLayout(act)
    private val applyBtn = GradientButton(act, "Aplicar como fondo")
    private val heart = CircleButton(act, Ic.HEART)
    private val eye = CircleButton(act, Ic.EYE)
    private val paletteLabel = act.label("", 14f, T.TEXT2, T.medium)
    private val swatches = ArrayList<PaletteSwatch>()
    private val hint = act.label("Toca y desliza la vista previa para interactuar", 12f, T.TEXT3).apply { gravity = Gravity.CENTER }
    private var controlsHidden = false
    private var videoError: TextView? = null
    private val prefs = act.prefs
    override val view: View = root

    init {
        root.setBackgroundColor(T.BG)
        sv.interactive = true
        sv.maxFps = 60
        root.addView(sv, flp(MATCH, MATCH, Gravity.CENTER))
        if (def.kind == Kind.VIDEO) {
            val vp = VideoPreview(act)
            video = vp
            root.addView(vp, flp(MATCH, MATCH, Gravity.CENTER))
        }
        rebuildScene()
        // Degradado superior para legibilidad
        root.addView(View(act).apply {
            background = GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, intArrayOf(0x88000000.toInt(), 0))
            isClickable = false
        }, flp(MATCH, dpi(140f), Gravity.TOP))

        // Barra superior
        val back = CircleButton(act, Ic.BACK)
        back.setOnClickListener { act.onBackPressed() }
        topBar.addView(back, flp(dpi(46f), dpi(46f), Gravity.START or Gravity.CENTER_VERTICAL))
        val right = LinearLayout(act).apply { orientation = LinearLayout.HORIZONTAL }
        right.addView(heart, llp(dpi(46f), dpi(46f), r = dpi(10f)))
        right.addView(eye, llp(dpi(46f), dpi(46f)))
        topBar.addView(right, flp(WRAP, WRAP, Gravity.END or Gravity.CENTER_VERTICAL))
        root.addView(topBar, flp(MATCH, WRAP, Gravity.TOP))
        heart.iconView.icon = if (prefs.isFav(def.id)) Ic.HEART_FILL else Ic.HEART
        heart.setOnClickListener {
            val f = prefs.toggleFav(def.id)
            heart.iconView.icon = if (f) Ic.HEART_FILL else Ic.HEART
            heart.iconView.scaleX = 0.3f; heart.iconView.scaleY = 0.3f
            heart.iconView.animate().scaleX(1f).scaleY(1f).setDuration(450).setInterpolator(OvershootInterpolator(4f)).start()
            act.toast(if (f) "Añadido a favoritos" else "Quitado de favoritos", if (f) Ic.HEART_FILL else Ic.HEART)
            act.onFavoritesChanged()
        }
        eye.setOnClickListener { toggleControls() }

        // Panel inferior
        panel.orientation = LinearLayout.VERTICAL
        panel.background = GradientDrawable().apply {
            setColor(T.PANEL)
            val r = dp(30f)
            cornerRadii = floatArrayOf(r, r, r, r, 0f, 0f, 0f, 0f)
            setStroke(1, 0x22FFFFFF)
        }
        panel.setPadding(dpi(22f), dpi(10f), dpi(22f), dpi(12f))
        panel.isClickable = true
        buildPanel()
        panelScroll.isVerticalScrollBarEnabled = false
        panelScroll.overScrollMode = View.OVER_SCROLL_NEVER
        panelScroll.addView(panel, lp(MATCH, WRAP))
        root.addView(panelScroll, flp(MATCH, WRAP, Gravity.BOTTOM))
        root.addView(hint, flp(MATCH, WRAP, Gravity.BOTTOM))
        hint.alpha = 0f

        // Entrada animada
        panelScroll.translationY = dp(500f)
        panelScroll.animate().translationY(0f).setStartDelay(120).setDuration(650).setInterpolator(DecelerateInterpolator(2.5f)).start()
        topBar.alpha = 0f
        topBar.animate().alpha(1f).setStartDelay(200).setDuration(400).start()
        sv.alpha = 0f; sv.scaleX = 1.08f; sv.scaleY = 1.08f
        sv.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(600).setInterpolator(DecelerateInterpolator(2f)).start()
    }

    private fun rebuildScene() {
        val vid = video
        if (vid != null) {
            val u = prefs.videoUri
            if (u != null) {
                vid.visibility = View.VISIBLE
                if (vid.uri?.toString() != u) vid.uri = Uri.parse(u)
                sv.scene = null
                sv.visibility = View.GONE
                return
            } else {
                vid.visibility = View.GONE
                sv.visibility = View.VISIBLE
            }
        }
        sv.scene = Catalog.create(act, def, prefs)
    }

    private fun sectionLabel(text: String): TextView = act.label(text, 14f, T.TEXT2, T.medium)

    private fun buildPanel() {
        // Asa
        val handle = View(act).apply { background = act.glass(3f, 0x40FFFFFF, 0) }
        panel.addView(handle, llp(dpi(40f), dpi(5f)).apply { gravity = Gravity.CENTER_HORIZONTAL })

        // Título
        val titleRow = LinearLayout(act).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        val tcol = LinearLayout(act).apply { orientation = LinearLayout.VERTICAL }
        tcol.addView(act.label(def.name, 28f, Color.WHITE, T.black))
        tcol.addView(act.label(def.tagline, 14f, T.TEXT2), llp(WRAP, WRAP, t = dpi(6f)))
        titleRow.addView(tcol, llp(0, WRAP, 1f))
        panel.addView(titleRow, llp(MATCH, WRAP, t = dpi(16f)))

        when (def.kind) {
            Kind.VIDEO -> buildVideoControls()
            Kind.PHOTO -> { buildPhotoControls(); buildPalette(); buildSliders() }
            Kind.SCENE -> { buildPalette(); buildSliders() }
        }

        applyBtn.setOnClickListener {
            if (def.kind == Kind.VIDEO && prefs.videoUri == null) { act.toast("Primero elige un video", Ic.VIDEO); return@setOnClickListener }
            if (def.kind == Kind.PHOTO && prefs.photoUri == null) { act.toast("Primero elige una foto", Ic.IMAGE); return@setOnClickListener }
            act.applyWallpaper(def) { ok -> if (ok) applyBtn.showSuccess("¡Aplicado!") }
        }
        panel.addView(applyBtn, llp(MATCH, WRAP, t = dpi(14f)))
    }

    private fun buildPalette() {
        val cur = prefs.palette(def.id, def.defaultPalette)
        paletteLabel.text = "Paleta · ${Palettes.get(cur).name}"
        panel.addView(paletteLabel, llp(WRAP, WRAP, t = dpi(22f)))
        val hs = HorizontalScrollView(act).apply { isHorizontalScrollBarEnabled = false; overScrollMode = View.OVER_SCROLL_NEVER }
        val row = LinearLayout(act).apply { orientation = LinearLayout.HORIZONTAL }
        for ((i, p) in Palettes.all.withIndex()) {
            val sw = PaletteSwatch(act, p, i == Math.floorMod(cur, Palettes.all.size))
            sw.setOnClickListener {
                prefs.setPalette(def.id, i)
                for ((k, s) in swatches.withIndex()) s.setSel(k == i)
                paletteLabel.text = "Paleta · ${p.name}"
                rebuildScene()
            }
            swatches += sw
            row.addView(sw, llp(WRAP, WRAP, r = dpi(4f)))
        }
        hs.addView(row)
        panel.addView(hs, llp(MATCH, WRAP, t = dpi(8f)))
    }

    private fun buildSliders() {
        val speed = GlassSlider(act, "Velocidad", 0.2f, 2.5f, prefs.speed(def.id)) { String.format(Locale.US, "%.1f×", it) }
        speed.onChange = { v -> sv.scene?.speed = v }
        speed.onCommit = { v -> prefs.setSpeed(def.id, v) }
        panel.addView(speed, llp(MATCH, WRAP, t = dpi(16f)))
        val dens = GlassSlider(act, "Densidad", 0.3f, 2f, prefs.density(def.id)) { "${(it * 100).toInt()}%" }
        var lastBuild = 0L
        dens.onChange = { v ->
            val now = System.currentTimeMillis()
            if (now - lastBuild > 120) {
                lastBuild = now
                sv.scene?.let { s -> s.configure(s.palette, s.speed, v) }
            }
        }
        dens.onCommit = { v -> prefs.setDensity(def.id, v); sv.scene?.let { s -> s.configure(s.palette, s.speed, v) } }
        panel.addView(dens, llp(MATCH, WRAP, t = dpi(6f)))
    }

    private fun buildPhotoControls() {
        val pick = GhostButton(act, if (prefs.photoUri == null) "Elegir foto" else "Cambiar foto", Ic.IMAGE)
        pick.setOnClickListener { act.pickMedia(false) }
        panel.addView(pick, llp(MATCH, dpi(52f), t = dpi(20f)))
        panel.addView(sectionLabel("Efecto encima"), llp(WRAP, WRAP, t = dpi(20f)))
        val hs = HorizontalScrollView(act).apply { isHorizontalScrollBarEnabled = false; overScrollMode = View.OVER_SCROLL_NEVER }
        val row = LinearLayout(act).apply { orientation = LinearLayout.HORIZONTAL }
        val chips = ArrayList<ChipView>()
        for ((id, name) in Catalog.effects) {
            val c = ChipView(act, name, prefs.photoEffect == id)
            c.setOnClickListener {
                prefs.photoEffect = id
                for (x in chips) x.setSel(x === c)
                rebuildScene()
            }
            chips += c
            row.addView(c, llp(WRAP, WRAP, r = dpi(8f)))
        }
        hs.addView(row)
        panel.addView(hs, llp(MATCH, WRAP, t = dpi(10f)))
        val dim = GlassSlider(act, "Oscurecer foto", 0f, 0.7f, prefs.photoDim) { "${(it * 100).toInt()}%" }
        dim.onCommit = { v -> prefs.photoDim = v; rebuildScene() }
        panel.addView(dim, llp(MATCH, WRAP, t = dpi(14f)))
    }

    private fun buildVideoControls() {
        val pick = GhostButton(act, if (prefs.videoUri == null) "Elegir video" else "Cambiar video", Ic.VIDEO)
        pick.setOnClickListener { act.pickMedia(true) }
        panel.addView(pick, llp(MATCH, dpi(52f), t = dpi(20f)))
        val warn = act.label("", 13f, T.PINK)
        warn.setLineSpacing(0f, 1.3f)
        warn.visibility = View.GONE
        videoError = warn
        panel.addView(warn, llp(MATCH, WRAP, t = dpi(12f)))
        val note = act.label("Se reproduce en bucle, en silencio y con recorte automático a tu pantalla. Ideal: videos cortos verticales.", 13f, T.TEXT3)
        note.setLineSpacing(0f, 1.3f)
        panel.addView(note, llp(MATCH, WRAP, t = dpi(12f)))
    }

    override fun onPicked(video: Boolean, uri: Uri) {
        if (video) { prefs.videoError = null; prefs.videoUri = uri.toString(); updateVideoNote() } else { prefs.photoUri = uri.toString(); BitmapCache.invalidate() }
        for (i in 0 until panel.childCount) {
            val v = panel.getChildAt(i)
            if (v is GhostButton) v.label.text = if (video) "Cambiar video" else "Cambiar foto"
        }
        rebuildScene()
        act.toast(if (video) "Video listo" else "Foto lista", if (video) Ic.VIDEO else Ic.IMAGE)
    }

    private fun toggleControls() {
        controlsHidden = !controlsHidden
        eye.iconView.icon = if (controlsHidden) Ic.EYE_OFF else Ic.EYE
        val interp = DecelerateInterpolator(2f)
        if (controlsHidden) {
            panelScroll.animate().translationY(panelScroll.height + dp(40f)).alpha(0.4f).setDuration(420).setInterpolator(interp).start()
            hint.animate().alpha(1f).setStartDelay(250).setDuration(400).start()
        } else {
            panelScroll.animate().translationY(0f).alpha(1f).setDuration(480).setInterpolator(interp).start()
            hint.animate().alpha(0f).setStartDelay(0).setDuration(200).start()
        }
    }

    override fun onBack(): Boolean {
        if (controlsHidden) { toggleControls(); return true }
        return false
    }

    override fun onInsets(top: Int, bottom: Int) {
        topBar.setPadding(dpi(16f), top + dpi(10f), dpi(16f), 0)
        panel.setPadding(dpi(22f), dpi(10f), dpi(22f), bottom + dpi(12f))
        (hint.layoutParams as FrameLayout.LayoutParams).bottomMargin = bottom + dpi(20f)
        hint.requestLayout()
    }

    override fun onShow() {
        act.backdrop.paused = true
        sv.paused = false
        video?.let { if (it.visibility == View.VISIBLE) it.restart() }
        updateVideoNote()
    }

    // Se libera el reproductor (no solo se pausa) para dejar libre el decodificador al fondo real.
    override fun onHide() {
        sv.paused = true
        video?.release()
    }

    private fun updateVideoNote() {
        val n = videoError ?: return
        val err = prefs.videoError
        n.visibility = if (err != null && act.activeService() != null) View.VISIBLE else View.GONE
        n.text = "El fondo tuvo problemas para reproducir este video ($err). Si persiste, prueba con un video 1080p o más corto."
    }

    override fun onDestroy() {
        video?.release()
        sv.scene = null
    }
}

// ─────────────────────────────── Ajustes ───────────────────────────────

class SettingsScreen(act: MainActivity) : Screen(act) {
    private val scroll = ScrollView(act)
    private val col = LinearLayout(act)
    private val top = FrameLayout(act)
    private val prefs = act.prefs
    override val view: View = scroll

    init {
        scroll.isVerticalScrollBarEnabled = false
        scroll.overScrollMode = View.OVER_SCROLL_NEVER
        scroll.clipToPadding = false
        scroll.setBackgroundColor(0xE607070D.toInt())
        col.orientation = LinearLayout.VERTICAL
        col.setPadding(dpi(20f), 0, dpi(20f), 0)
        scroll.addView(col, lp(MATCH, WRAP))

        val back = CircleButton(act, Ic.BACK)
        back.setOnClickListener { act.onBackPressed() }
        top.addView(back, flp(dpi(46f), dpi(46f), Gravity.START or Gravity.CENTER_VERTICAL))
        top.addView(act.label("Ajustes", 20f, Color.WHITE, T.bold), flp(WRAP, WRAP, Gravity.CENTER))
        col.addView(top, llp(MATCH, WRAP))

        // Rendimiento
        val perf = card("Rendimiento")
        val fpsVals = listOf(20, 30, 45, 60)
        val fpsSeg = Segmented(act, listOf("20", "30", "45", "60 FPS"), fpsVals.indexOf(prefs.fps).coerceAtLeast(0))
        fpsSeg.onSelect = { prefs.fps = fpsVals[it] }
        rowText(perf, "Fotogramas por segundo", "Más FPS = más fluido. 30 FPS ahorra batería.")
        perf.addView(fpsSeg, llp(MATCH, WRAP, t = dpi(12f), b = dpi(6f)))
        divider(perf)
        toggleRow(perf, "Ahorro inteligente", "Baja a 20 FPS cuando el modo ahorro de batería está activo", prefs.saver) { prefs.saver = it }

        // Interacción
        val inter = card("Interacción")
        toggleRow(inter, "Reaccionar al tacto", "Las animaciones responden a tus toques", prefs.touch) { prefs.touch = it }
        divider(inter)
        toggleRow(inter, "Efecto parallax", "El fondo se desplaza al pasar entre pantallas", prefs.parallax) { prefs.parallax = it }
        divider(inter)
        toggleRow(inter, "Doble toque: siguiente fondo", "Cambia de fondo tocando dos veces la pantalla de inicio", prefs.doubleTap) { prefs.doubleTap = it }

        // Rotación
        val rot = card("Rotación automática")
        val rotVals = listOf(0, 15, 60, 360, 1440)
        rowText(rot, "Cambiar fondo cada…", "Rota entre fondos animados al encender la pantalla")
        val rotSeg = Segmented(act, listOf("No", "15 m", "1 h", "6 h", "24 h"), rotVals.indexOf(prefs.rotateMinutes).coerceAtLeast(0))
        rotSeg.onSelect = { prefs.rotateMinutes = rotVals[it]; prefs.lastRotate = System.currentTimeMillis() }
        rot.addView(rotSeg, llp(MATCH, WRAP, t = dpi(12f), b = dpi(6f)))
        divider(rot)
        toggleRow(rot, "Solo favoritos", "Usa únicamente los fondos marcados con ♥", prefs.rotateFavs) { prefs.rotateFavs = it }

        // Sistema
        val sys = card("Sistema")
        val chooser = GhostButton(act, "Abrir selector de fondos animados", Ic.LAYERS)
        chooser.setOnClickListener { act.openSystemChooser() }
        sys.addView(chooser, llp(MATCH, dpi(52f), t = dpi(4f), b = dpi(4f)))

        // Acerca de
        val about = card("Acerca de")
        val row = LinearLayout(act).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        val icon = ImageView(act).apply { setImageResource(com.dowpaper.android.R.mipmap.ic_launcher) }
        row.addView(icon, LinearLayout.LayoutParams(dpi(56f), dpi(56f)))
        val tc = LinearLayout(act).apply { orientation = LinearLayout.VERTICAL }
        tc.addView(act.label("DowPaper", 18f, Color.WHITE, T.bold))
        tc.addView(act.label("Versión ${act.versionName()}", 13f, T.TEXT3), llp(WRAP, WRAP, t = dpi(4f)))
        row.addView(tc, llp(0, WRAP, 1f, l = dpi(14f)))
        about.addView(row, llp(MATCH, WRAP, t = dpi(4f)))
        val desc = act.label("${Catalog.all.size - 2} fondos animados generados en tiempo real, más tu propia foto o video. Sin anuncios, sin conexión a internet y sin permisos especiales.", 13.5f, T.TEXT2)
        desc.setLineSpacing(0f, 1.35f)
        about.addView(desc, llp(MATCH, WRAP, t = dpi(14f), b = dpi(4f)))
    }

    private fun card(title: String): LinearLayout {
        col.addView(act.label(title.uppercase(Locale.getDefault()), 12f, T.A3, T.bold).apply { letterSpacing = 0.14f }, llp(WRAP, WRAP, l = dpi(6f), t = dpi(26f)))
        val c = LinearLayout(act).apply {
            orientation = LinearLayout.VERTICAL
            background = act.glass(24f, 0x12FFFFFF, 0x22FFFFFF)
            setPadding(dpi(18f), dpi(14f), dpi(18f), dpi(14f))
        }
        col.addView(c, llp(MATCH, WRAP, t = dpi(10f)))
        return c
    }

    private fun rowText(parent: LinearLayout, title: String, sub: String) {
        val tc = LinearLayout(act).apply { orientation = LinearLayout.VERTICAL }
        tc.addView(act.label(title, 16f, Color.WHITE, T.medium))
        tc.addView(act.label(sub, 13f, T.TEXT3).apply { setLineSpacing(0f, 1.2f) }, llp(MATCH, WRAP, t = dpi(4f)))
        parent.addView(tc, llp(MATCH, WRAP, t = dpi(6f)))
    }

    private fun toggleRow(parent: LinearLayout, title: String, sub: String, value: Boolean, onChange: (Boolean) -> Unit) {
        val row = LinearLayout(act).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        val tc = LinearLayout(act).apply { orientation = LinearLayout.VERTICAL }
        tc.addView(act.label(title, 16f, Color.WHITE, T.medium))
        tc.addView(act.label(sub, 13f, T.TEXT3).apply { setLineSpacing(0f, 1.2f) }, llp(MATCH, WRAP, t = dpi(4f)))
        row.addView(tc, llp(0, WRAP, 1f, r = dpi(12f)))
        val sw = ToggleSwitch(act, value)
        sw.onChange = onChange
        row.addView(sw)
        row.setOnClickListener { sw.set(!sw.checked, true) }
        parent.addView(row, llp(MATCH, WRAP, t = dpi(8f), b = dpi(8f)))
    }

    private fun divider(parent: LinearLayout) {
        parent.addView(View(act).apply { setBackgroundColor(0x14FFFFFF) }, llp(MATCH, 1, t = dpi(8f), b = dpi(8f)))
    }

    override fun onInsets(top: Int, bottom: Int) {
        this.top.setPadding(0, top + dpi(10f), 0, 0)
        scroll.setPadding(0, 0, 0, bottom + dpi(30f))
    }

    override fun onShow() { act.backdrop.paused = false }
}
