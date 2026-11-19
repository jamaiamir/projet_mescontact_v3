package com.example.projet_mescontact_v3.data

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.projet_mescontact_v3.util.ContactFilter
import com.example.projet_mescontact_v3.util.ImageUtils

/**
 * ViewModel PARTAGÉ par les fragments (portée = MainActivity, via activityViewModels()).
 *
 * - Il survit à la rotation : la liste et le filtre ne sont pas perdus.
 * - Quand DetailContactFragment enregistre, il appelle [save] ; ListeContactsFragment
 *   observe [filteredContacts] et se met à jour immédiatement, sans transmettre
 *   d'objet en retour.
 * Le stockage reste en mémoire (pas de Room/SQLite exigé pour ce TP).
 */
class ContactsViewModel : ViewModel() {

    private val contacts = MutableLiveData(sampleContacts())
    private val query = MutableLiveData("")
    private var nextId = (contacts.value.orEmpty().maxOfOrNull { it.id } ?: 0L) + 1

    /** Liste affichée = liste complète passée au filtre courant. */
    val filteredContacts: LiveData<List<Contact>> = MediatorLiveData<List<Contact>>().apply {
        fun update() {
            value = ContactFilter.filter(contacts.value.orEmpty(), query.value.orEmpty())
        }
        addSource(contacts) { update() }
        addSource(query) { update() }
    }

    val currentQuery: String get() = query.value.orEmpty()
    val totalCount: Int get() = contacts.value.orEmpty().size

    fun setQuery(text: String) {
        if (text != query.value) query.value = text
    }

    fun getContact(id: Long): Contact? = contacts.value.orEmpty().firstOrNull { it.id == id }

    /**
     * Ajoute ([contact].id == [NEW_CONTACT_ID]) ou remplace le contact ayant le même id.
     * Recherche par identifiant : aucune erreur d'indice possible, même si la liste est filtrée.
     */
    fun save(contact: Contact): Contact {
        val list = contacts.value.orEmpty().toMutableList()
        val index = list.indexOfFirst { it.id == contact.id }
        val saved: Contact
        if (contact.id == NEW_CONTACT_ID || index < 0) {
            saved = contact.copy(id = nextId++)
            list.add(saved)
        } else {
            val old = list[index]
            if (old.photoPath != contact.photoPath) ImageUtils.deleteQuietly(old.photoPath)
            saved = contact
            list[index] = saved
        }
        contacts.value = list
        return saved
    }

    private fun sampleContacts() = listOf(
        Contact(1, "Ben Salah", "Amira", "+216 20 123 456", "amira.bensalah@example.com",
            latitude = 36.8065, longitude = 10.1815),        // Tunis
        Contact(2, "Trabelsi", "Youssef", "+216 22 987 654", "youssef.trabelsi@example.com",
            latitude = 34.7406, longitude = 10.7603),        // Sfax
        Contact(3, "Gharbi", "Mariem", "+216 55 444 321", "mariem.gharbi@example.com",
            latitude = 35.8256, longitude = 10.6360),        // Sousse
        Contact(4, "Dupont", "Claire", "06 12 34 56 78", "claire.dupont@example.com",
            latitude = 48.8584, longitude = 2.2945),         // Paris, tour Eiffel
        Contact(5, "Jlassi", "Karim", "+216 98 111 222", "karim.jlassi@example.com"),
        // ↑ volontairement sans position : teste le message « coordonnées absentes »
        Contact(6, "Haddad", "Lina", "+216 50 765 432", "lina.haddad@example.com",
            latitude = 33.8869, longitude = 9.5375)          // Centre de la Tunisie
    )

    companion object {
        const val NEW_CONTACT_ID = -1L
    }
}
