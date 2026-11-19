package com.example.projet_mescontact_v3.util

import com.example.projet_mescontact_v3.data.Contact
import java.text.Normalizer
import java.util.Locale

/**
 * Filtrage de la liste : nom, prénom, nom complet (dans les deux ordres) et téléphone.
 * Insensible à la casse et aux accents ; pour le téléphone, les espaces, tirets et
 * points sont ignorés (« 20123 » trouve « +216 20 123 456 »).
 */
object ContactFilter {

    fun filter(contacts: List<Contact>, query: String): List<Contact> {
        val q = normalize(query)
        if (q.isEmpty()) return contacts
        val qDigits = query.filter { it.isDigit() }
        return contacts.filter { c ->
            val textMatch = listOf(c.nom, c.prenom, "${c.nom} ${c.prenom}", "${c.prenom} ${c.nom}",
                c.telephone).any { normalize(it).contains(q) }
            // Recherche « numérique » : on compare uniquement les chiffres
            val phoneMatch = qDigits.isNotEmpty() && query.all { it.isDigit() || it in PHONE_SEPARATORS } &&
                c.telephone.filter { it.isDigit() }.contains(qDigits)
            textMatch || phoneMatch
        }
    }

    private const val PHONE_SEPARATORS = " +-.()"

    /** Minuscules, sans accents, espaces multiples réduits. */
    fun normalize(s: String): String {
        val noAccents = Normalizer.normalize(s, Normalizer.Form.NFD)
            .replace(Regex("\\p{Mn}+"), "")
        return noAccents.lowercase(Locale.ROOT).trim().replace(Regex("\\s+"), " ")
    }
}
