package com.dowpaper.android.core

import android.content.Context
import com.dowpaper.android.scenes.*

enum class Kind { SCENE, PHOTO, VIDEO }

class SceneDef(
    val id: String,
    val name: String,
    val tagline: String,
    val category: Int,
    val defaultPalette: Int,
    val kind: Kind = Kind.SCENE,
    val factory: (Context) -> Scene
)

object Catalog {
    const val CAT_ALL = 0
    const val CAT_FAV = 1
    const val CAT_ABSTRACT = 2
    const val CAT_SPACE = 3
    const val CAT_NATURE = 4
    const val CAT_TECH = 5
    const val CAT_PERSONAL = 6

    val categories = listOf("Todos", "Favoritos", "Abstracto", "Espacio", "Naturaleza", "Tecno", "Personal")

    val all: List<SceneDef> = listOf(
        SceneDef("aurora", "Aurora", "Cortinas de luz polar sobre montañas", CAT_SPACE, 0) { AuroraScene(it) },
        SceneDef("fluid", "Fluido", "Gradientes líquidos que respiran", CAT_ABSTRACT, 1) { FluidScene(it) },
        SceneDef("cosmos", "Cosmos", "Nebulosas, estrellas y fugaces", CAT_SPACE, 3) { CosmosScene(it) },
        SceneDef("waves", "Olas", "Un mar infinito al atardecer", CAT_NATURE, 1) { WavesScene(it) },
        SceneDef("bokeh", "Bokeh", "Luces suaves fuera de foco", CAT_ABSTRACT, 8) { BokehScene(it) },
        SceneDef("warp", "Hiperespacio", "Viaja a la velocidad de la luz", CAT_SPACE, 2) { WarpScene(it) },
        SceneDef("fireflies", "Luciérnagas", "Un bosque mágico bajo la luna", CAT_NATURE, 5) { FirefliesScene(it) },
        SceneDef("mandala", "Mandala", "Geometría sagrada hipnótica", CAT_ABSTRACT, 4) { MandalaScene(it) },
        SceneDef("network", "Red Neural", "Nodos conectados que reaccionan a ti", CAT_TECH, 7) { NetworkScene(it) },
        SceneDef("code", "Código", "Lluvia digital de glifos", CAT_TECH, 5) { CodeRainScene(it) },
        SceneDef("snow", "Nevada", "Copos tranquilos en la noche", CAT_NATURE, 2) { SnowScene(it) },
        SceneDef("rain", "Lluvia", "Tormenta nocturna sobre la ciudad", CAT_NATURE, 3) { RainScene(it) },
        SceneDef("photo", "Tu foto", "Tu imagen con zoom vivo y efectos", CAT_PERSONAL, 3, Kind.PHOTO) { PhotoScene(it) },
        SceneDef("video", "Tu video", "Cualquier video como fondo en bucle", CAT_PERSONAL, 3, Kind.VIDEO) { PhotoScene(it).also { s -> s.videoFrame = true } }
    )

    fun byId(id: String?): SceneDef = all.firstOrNull { it.id == id } ?: all[0]

    val effects = listOf("none" to "Ninguno", "bokeh" to "Bokeh", "snow" to "Nieve", "rain" to "Lluvia", "fireflies" to "Luciérnagas", "stars" to "Estrellas")

    /** Construye una escena ya configurada según las preferencias guardadas. */
    fun create(ctx: Context, def: SceneDef, prefs: Prefs): Scene {
        val s = def.factory(ctx)
        if (s is PhotoScene) {
            if (def.kind == Kind.VIDEO) {
                s.uri = prefs.videoUri
                s.effect = "none"
                s.dim = 0f
            } else {
                s.uri = prefs.photoUri
                s.effect = prefs.photoEffect
                s.dim = prefs.photoDim
            }
        }
        s.configure(Palettes.get(prefs.palette(def.id, def.defaultPalette)), prefs.speed(def.id), prefs.density(def.id))
        s.touchEnabled = prefs.touch
        return s
    }
}
