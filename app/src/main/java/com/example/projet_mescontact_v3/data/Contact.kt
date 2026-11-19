package com.example.projet_mescontact_v3.data

/**
 * Modèle d'un contact (V3) : identité, coordonnées, photo et position GPS.
 *
 * @property id identifiant unique et stable : c'est lui (et non la position dans la liste)
 *              qui est transmis entre fragments.
 * @property photoPath chemin d'un JPEG dans le stockage interne, null = pas de photo.
 * @property latitude  null = aucune position définie (sinon entre -90 et 90).
 * @property longitude null = aucune position définie (sinon entre -180 et 180).
 */
data class Contact(
    val id: Long,
    val nom: String,
    val prenom: String,
    val telephone: String,
    val email: String,
    val photoPath: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null
) {
    val nomComplet: String get() = "$nom $prenom".trim()

    /** Vrai seulement si les deux coordonnées existent et sont dans les bornes. */
    val aUnePosition: Boolean
        get() = latitude != null && longitude != null &&
            latitude in -90.0..90.0 && longitude in -180.0..180.0
}
