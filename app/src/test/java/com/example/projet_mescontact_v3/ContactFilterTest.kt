package com.example.projet_mescontact_v3

import com.example.projet_mescontact_v3.data.Contact
import com.example.projet_mescontact_v3.util.ContactFilter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ContactFilterTest {

    private val contacts = listOf(
        Contact(1, "Ben Salah", "Amira", "+216 20 123 456", "a@example.com"),
        Contact(2, "Trabelsi", "Youssef", "+216 22 987 654", "y@example.com"),
        Contact(3, "Gharbi", "Mériem", "+216 55 444 321", "m@example.com")
    )

    private fun ids(q: String) = ContactFilter.filter(contacts, q).map { it.id }

    @Test fun filtreVide_afficheTout() = assertEquals(listOf(1L, 2L, 3L), ids(""))
    @Test fun parNom_insensibleCasse() = assertEquals(listOf(2L), ids("TRABELSI"))
    @Test fun parPrenom() = assertEquals(listOf(1L), ids("amira"))
    @Test fun sansAccent() = assertEquals(listOf(3L), ids("meriem"))
    @Test fun prenomPuisNom() = assertEquals(listOf(1L), ids("amira ben"))
    @Test fun telephoneSansEspaces() = assertEquals(listOf(1L), ids("20123"))
    @Test fun telephoneAvecEspaces() = assertEquals(listOf(2L), ids("22 987"))
    @Test fun aucunResultat() = assertTrue(ids("zzz").isEmpty())

    /** Après filtrage, la 1re ligne n'est pas le contact n°1 : on doit utiliser l'id. */
    @Test fun listeFiltree_bonContact() {
        val filtered = ContactFilter.filter(contacts, "gharbi")
        assertEquals(3L, filtered[0].id)
    }
}
