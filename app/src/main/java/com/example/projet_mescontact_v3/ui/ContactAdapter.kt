package com.example.projet_mescontact_v3.ui

import android.graphics.Bitmap
import android.util.LruCache
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.projet_mescontact_v3.R
import com.example.projet_mescontact_v3.data.Contact
import com.example.projet_mescontact_v3.databinding.ItemContactBinding
import com.example.projet_mescontact_v3.util.ImageUtils

/**
 * Adapter de la RecyclerView. ListAdapter + DiffUtil calcule les lignes modifiées,
 * ce qui anime proprement le filtrage et la mise à jour après enregistrement.
 *
 * Le clic renvoie le CONTACT de la ligne (donc son id), jamais l'indice : après un
 * filtrage, l'indice 0 n'est plus le contact n°0 de la liste complète.
 */
class ContactAdapter(
    private val onClick: (Contact) -> Unit
) : ListAdapter<Contact, ContactAdapter.ContactViewHolder>(DIFF) {

    private val thumbnails = LruCache<String, Bitmap>(50)

    inner class ContactViewHolder(val binding: ItemContactBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val pos = bindingAdapterPosition
                if (pos != RecyclerView.NO_POSITION) onClick(getItem(pos))
            }
        }

        fun bind(contact: Contact) {
            binding.txtNom.text = contact.nom
            binding.txtPrenom.text = contact.prenom
            binding.txtTelephone.text = contact.telephone
            binding.imgLocation.visibility =
                if (contact.aUnePosition) android.view.View.VISIBLE else android.view.View.GONE

            val bitmap = contact.photoPath?.let { path ->
                thumbnails.get(path) ?: ImageUtils.decodeScaled(path, 160)?.also { thumbnails.put(path, it) }
            }
            if (bitmap != null) binding.imgPhoto.setImageBitmap(bitmap)
            else binding.imgPhoto.setImageResource(R.drawable.placeholder_avatar)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ContactViewHolder(
        ItemContactBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun onBindViewHolder(holder: ContactViewHolder, position: Int) =
        holder.bind(getItem(position))

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<Contact>() {
            override fun areItemsTheSame(old: Contact, new: Contact) = old.id == new.id
            override fun areContentsTheSame(old: Contact, new: Contact) = old == new
        }
    }
}
