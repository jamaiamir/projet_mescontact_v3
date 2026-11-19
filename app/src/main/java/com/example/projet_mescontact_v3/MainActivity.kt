package com.example.projet_mescontact_v3

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.commit
import com.example.projet_mescontact_v3.databinding.ActivityMainBinding
import com.example.projet_mescontact_v3.ui.CarteContactFragment
import com.example.projet_mescontact_v3.ui.DetailContactFragment
import com.example.projet_mescontact_v3.ui.ListeContactsFragment

/**
 * Unique activité de l'application : elle ne contient qu'un FragmentContainerView
 * et centralise la navigation (FragmentManager / FragmentTransaction).
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Après une rotation, le FragmentManager recrée lui-même les fragments et la pile :
        // on n'ajoute la liste qu'au premier lancement, sinon elle serait empilée en double.
        if (savedInstanceState == null) {
            supportFragmentManager.commit {
                setReorderingAllowed(true)
                replace(R.id.fragment_container, ListeContactsFragment(), ListeContactsFragment.TAG)
            }
        }
    }

    /** Liste → Détail : seul l'identifiant du contact est transmis (Bundle). */
    fun openDetail(contactId: Long) {
        supportFragmentManager.commit {
            setReorderingAllowed(true)
            setCustomAnimations(
                android.R.anim.fade_in, android.R.anim.fade_out,
                android.R.anim.fade_in, android.R.anim.fade_out
            )
            replace(R.id.fragment_container, DetailContactFragment.newInstance(contactId), DetailContactFragment.TAG)
            addToBackStack(DetailContactFragment.TAG)
        }
    }

    /** Détail → Carte ; « Retour » dépile et revient au détail avec la saisie en cours. */
    fun openMap(contactId: Long, name: String, latitude: String, longitude: String) {
        supportFragmentManager.commit {
            setReorderingAllowed(true)
            setCustomAnimations(
                android.R.anim.fade_in, android.R.anim.fade_out,
                android.R.anim.fade_in, android.R.anim.fade_out
            )
            replace(
                R.id.fragment_container,
                CarteContactFragment.newInstance(contactId, name, latitude, longitude),
                CarteContactFragment.TAG
            )
            addToBackStack(CarteContactFragment.TAG)
        }
    }

    /** Retour à l'écran précédent de la pile (bouton flèche des barres d'outils). */
    fun goBack() {
        if (!supportFragmentManager.isStateSaved) supportFragmentManager.popBackStack()
    }
}
