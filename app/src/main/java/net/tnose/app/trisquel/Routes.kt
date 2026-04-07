package net.tnose.app.trisquel

import kotlinx.serialization.Serializable

@Serializable
object FilmRollsRoute

@Serializable
object CamerasRoute

@Serializable
object LensesRoute

@Serializable
object AccessoriesRoute

@Serializable
object FavoritesRoute

@Serializable
object SettingsRoute

@Serializable
object LicenseRoute

@Serializable
data class EditAccessoryRoute(val id: Int = -1)

@Serializable
data class EditCameraRoute(val type: Int, val id: Int = -1)

@Serializable
data class EditFilmRollRoute(
    val id: Int = -1,
    val defaultCamera: Int = -1,
    val defaultManufacturer: String = "",
    val defaultBrand: String = ""
)

@Serializable
data class PhotoListRoute(val id: Int)

@Serializable
data class EditPhotoRoute(
    val filmroll: Int,
    val id: Int = -1,
    val frameIndex: Int = -1
)

@Serializable
data class EditLensRoute(val id: Int = -1)

@Serializable
data class SearchRoute(val tags: String = "")
