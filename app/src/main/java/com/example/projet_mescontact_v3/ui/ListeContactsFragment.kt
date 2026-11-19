package com.example.projet_mescontact_v3.ui

import android.os.Bundle
import android.view.View
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.projet_mescontact_v3.MainActivity
import com.example.projet_mescontact_v3.R
import com.example.projet_mescontact_v3.data.ContactsViewModel
import com.example.projet_mescontact_v3.databinding.FragmentListeContactsBinding

/** Écran 1 : liste des contacts (RecyclerView) avec recherche instantanée. */
class ListeContactsFragment : Fragment(R.layout.fragment_liste_contacts) {

    private var _binding: FragmentListeContactsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ContactsViewModel by activityViewModels()
    private lateinit var adapter: ContactAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentListeContactsBinding.bind(view)

        adapter = ContactAdapter { contact -> (requireActivity() as MainActivity).openDetail(contact.id) }
        binding.recyclerContacts.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerContacts.adapter = adapter

        binding.toolbar.inflateMenu(R.menu.menu_liste)
        binding.toolbar.setOnMenuItemClickListener { item ->
            if (item.itemId == R.id.action_add) {
                (requireActivity() as MainActivity).openDetail(ContactsViewModel.NEW_CONTACT_ID)
                true
            } else false
        }

        // Le filtre est mémorisé dans le ViewModel : après rotation ou retour, on le réaffiche
        if (binding.editSearch.text.toString() != viewModel.currentQuery) {
            binding.editSearch.setText(viewModel.currentQuery)
        }
        // Filtrage à chaque frappe, sans bouton de validation
        binding.editSearch.doAfterTextChanged { viewModel.setQuery(it?.toString().orEmpty()) }

        viewModel.filteredContacts.observe(viewLifecycleOwner) { list ->
            adapter.submitList(list)
            val searching = viewModel.currentQuery.isNotBlank()
            binding.txtEmpty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
            binding.txtEmpty.text = when {
                list.isNotEmpty() -> ""
                searching -> getString(R.string.empty_search, viewModel.currentQuery.trim())
                else -> getString(R.string.empty_list)
            }
            binding.txtCount.text = if (searching)
                getString(R.string.count_filtered, list.size, viewModel.totalCount)
            else resources.getQuantityString(R.plurals.count_all, list.size, list.size)
        }
    }

    override fun onDestroyView() {
        binding.recyclerContacts.adapter = null
        _binding = null
        super.onDestroyView()
    }

    companion object {
        const val TAG = "liste"
    }
}
