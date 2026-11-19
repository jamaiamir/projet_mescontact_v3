package com.example.projet_mescontact_v3.ui

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.os.bundleOf
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.projet_mescontact_v3.MainActivity
import com.example.projet_mescontact_v3.R
import com.example.projet_mescontact_v3.data.Contact
import com.example.projet_mescontact_v3.data.ContactsViewModel
import com.example.projet_mescontact_v3.databinding.FragmentDetailContactBinding
import com.example.projet_mescontact_v3.util.CoordinateValidator
import com.example.projet_mescontact_v3.util.ImageUtils
import java.io.File

/**
 * Écran 2 : consultation / modification d'un contact et actions associées
 * (appel, e-mail, galerie, caméra, carte). Reçoit uniquement l'id du contact.
 */
class DetailContactFragment : Fragment(R.layout.fragment_detail_contact) {

    private var _binding: FragmentDetailContactBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ContactsViewModel by activityViewModels()

    private val contactId: Long by lazy {
        requireArguments().getLong(ARG_CONTACT_ID, ContactsViewModel.NEW_CONTACT_ID)
    }
    private lateinit var original: Contact

    /** Le formulaire a-t-il déjà été rempli depuis le modèle ? (évite d'écraser la saisie
     *  après une rotation ou un retour depuis la carte). */
    private var formInitialized = false
    private var currentPhotoPath: String? = null
    private var cameraTempFile: File? = null
    /** Photos créées pendant l'édition : supprimées si l'on annule. */
    private val newPhotos = ArrayList<String>()

    // ---------- Résultats d'activités (enregistrés avant onCreate) ----------
    private val pickImage = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri == null) toast(R.string.msg_photo_selection_cancelled) else applyNewPhoto(uri)
    }

    private val takePicture = registerForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val temp = cameraTempFile
        cameraTempFile = null
        if (ok && temp != null && temp.exists() && temp.length() > 0) applyNewPhoto(Uri.fromFile(temp))
        else toast(R.string.msg_photo_capture_cancelled)
        temp?.delete()
    }

    private val requestCamera = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) launchCamera() else toast(R.string.msg_camera_permission_denied)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState != null) {
            formInitialized = savedInstanceState.getBoolean(STATE_INITIALIZED)
            currentPhotoPath = savedInstanceState.getString(STATE_PHOTO)
            cameraTempFile = savedInstanceState.getString(STATE_TEMP)?.let { File(it) }
            savedInstanceState.getStringArrayList(STATE_NEW_PHOTOS)?.let { newPhotos.addAll(it) }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentDetailContactBinding.bind(view)

        val found = if (contactId == ContactsViewModel.NEW_CONTACT_ID)
            Contact(ContactsViewModel.NEW_CONTACT_ID, "", "", "", "")
        else viewModel.getContact(contactId)
        if (found == null) {
            toast(R.string.msg_invalid_contact)
            view.post { (activity as? MainActivity)?.goBack() }
            return
        }
        original = found

        binding.toolbar.setTitle(
            if (contactId == ContactsViewModel.NEW_CONTACT_ID) R.string.title_new else R.string.title_detail
        )
        binding.toolbar.setNavigationOnClickListener { cancel() }

        if (!formInitialized) {
            binding.editNom.setText(original.nom)
            binding.editPrenom.setText(original.prenom)
            binding.editTelephone.setText(original.telephone)
            binding.editEmail.setText(original.email)
            binding.editLatitude.setText(CoordinateValidator.format(original.latitude))
            binding.editLongitude.setText(CoordinateValidator.format(original.longitude))
            currentPhotoPath = original.photoPath
            formInitialized = true
        }
        // Sinon : les EditText restaurent eux-mêmes leur texte (état de la vue).
        showPhoto()
        showSavedPosition()

        // Efface l'erreur d'un champ dès qu'on le corrige
        binding.editNom.doAfterTextChanged { binding.layoutNom.error = null }
        binding.editPrenom.doAfterTextChanged { binding.layoutPrenom.error = null }
        binding.editTelephone.doAfterTextChanged { binding.layoutTelephone.error = null }
        binding.editEmail.doAfterTextChanged { binding.layoutEmail.error = null }
        binding.editLatitude.doAfterTextChanged { binding.layoutLatitude.error = null }
        binding.editLongitude.doAfterTextChanged { binding.layoutLongitude.error = null }

        binding.btnCall.setOnClickListener { call() }
        binding.btnEmail.setOnClickListener { sendEmail() }
        binding.btnGallery.setOnClickListener { pickFromGallery() }
        binding.btnCamera.setOnClickListener { takePhoto() }
        binding.btnMap.setOnClickListener { openMap() }
        binding.btnSave.setOnClickListener { save() }
        binding.btnCancel.setOnClickListener { cancel() }

        // Retour système = Annuler (lié au cycle de vie de la vue)
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() = cancel()
            })
    }

    private fun showSavedPosition() {
        binding.txtPosition.text = if (original.aUnePosition) getString(
            R.string.position_saved,
            CoordinateValidator.format(original.latitude),
            CoordinateValidator.format(original.longitude)
        ) else getString(R.string.position_none)
    }

    // ================= Appel / e-mail (Intents implicites) =================

    private fun call() {
        val phone = binding.editTelephone.text.toString().trim()
        if (phone.isEmpty()) {
            binding.layoutTelephone.error = getString(R.string.error_phone_empty); return
        }
        val intent = Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", phone, null))
        try { startActivity(intent) } catch (e: ActivityNotFoundException) { toast(R.string.msg_no_dialer) }
    }

    private fun sendEmail() {
        val email = binding.editEmail.text.toString().trim()
        when {
            email.isEmpty() -> { binding.layoutEmail.error = getString(R.string.error_email_empty); return }
            !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                binding.layoutEmail.error = getString(R.string.error_email_invalid); return
            }
        }
        val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:")).apply {
            putExtra(Intent.EXTRA_EMAIL, arrayOf(email))
        }
        try { startActivity(intent) } catch (e: ActivityNotFoundException) { toast(R.string.msg_no_mail_app) }
    }

    // ================= Photo =================

    private fun pickFromGallery() {
        try {
            pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        } catch (e: ActivityNotFoundException) {
            toast(R.string.msg_no_gallery)
        }
    }

    private fun takePhoto() {
        val ctx = requireContext()
        if (!ctx.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)) {
            toast(R.string.msg_camera_unavailable); return
        }
        if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
            launchCamera()
        else requestCamera.launch(Manifest.permission.CAMERA)
    }

    private fun launchCamera() {
        val ctx = requireContext()
        val temp = ImageUtils.newPhotoFile(ctx, "tmp")
        cameraTempFile = temp
        try {
            val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", temp)
            takePicture.launch(uri)
        } catch (e: ActivityNotFoundException) {
            cameraTempFile = null; temp.delete(); toast(R.string.msg_no_camera_app)
        } catch (e: SecurityException) {
            cameraTempFile = null; temp.delete(); toast(R.string.msg_camera_permission_denied)
        } catch (e: IllegalArgumentException) {
            cameraTempFile = null; temp.delete(); toast(R.string.msg_photo_load_failed)
        }
    }

    private fun applyNewPhoto(source: Uri) {
        val ctx = context ?: return
        val dest = ImageUtils.newPhotoFile(ctx, "photo")
        if (!ImageUtils.importPhoto(ctx, source, dest)) { toast(R.string.msg_photo_load_failed); return }
        // Une photo non enregistrée remplacée par une autre est supprimée tout de suite
        currentPhotoPath?.let { prev -> if (newPhotos.remove(prev)) ImageUtils.deleteQuietly(prev) }
        newPhotos.add(dest.path)
        currentPhotoPath = dest.path
        if (_binding != null) showPhoto()
    }

    private fun showPhoto() {
        val bmp = ImageUtils.decodeScaled(currentPhotoPath, 512)
        if (bmp != null) binding.imgPhoto.setImageBitmap(bmp)
        else binding.imgPhoto.setImageResource(R.drawable.placeholder_avatar)
    }

    // ================= Carte =================

    private fun openMap() {
        val lat = binding.editLatitude.text.toString()
        val lng = binding.editLongitude.text.toString()
        // On signale les erreurs dans le formulaire ; la carte affichera aussi un message explicite.
        showCoordinateErrors(CoordinateValidator.parse(lat, lng))
        val name = "${binding.editPrenom.text.toString().trim()} ${binding.editNom.text.toString().trim()}"
            .trim().ifEmpty { getString(R.string.title_new) }
        (requireActivity() as MainActivity).openMap(contactId, name, lat, lng)
    }

    private fun showCoordinateErrors(result: CoordinateValidator.Result): Boolean {
        if (result !is CoordinateValidator.Result.Invalid) return true
        result.latError?.let { binding.layoutLatitude.error = coordinateMessage(it, isLatitude = true) }
        result.lngError?.let { binding.layoutLongitude.error = coordinateMessage(it, isLatitude = false) }
        return false
    }

    private fun coordinateMessage(error: CoordinateValidator.Error, isLatitude: Boolean): String = when (error) {
        CoordinateValidator.Error.MISSING -> getString(R.string.error_coord_missing)
        CoordinateValidator.Error.NOT_A_NUMBER -> getString(R.string.error_coord_nan)
        CoordinateValidator.Error.OUT_OF_RANGE ->
            getString(if (isLatitude) R.string.error_lat_range else R.string.error_lng_range)
    }

    // ================= Enregistrer / Annuler =================

    private fun save() {
        val nom = binding.editNom.text.toString().trim()
        val prenom = binding.editPrenom.text.toString().trim()
        val phone = binding.editTelephone.text.toString().trim()
        val email = binding.editEmail.text.toString().trim()
        val coords = CoordinateValidator.parse(
            binding.editLatitude.text.toString(), binding.editLongitude.text.toString()
        )

        var firstInvalid: View? = null
        if (nom.isEmpty()) {
            binding.layoutNom.error = getString(R.string.error_last_name); firstInvalid = binding.editNom
        }
        if (prenom.isEmpty()) {
            binding.layoutPrenom.error = getString(R.string.error_first_name)
            firstInvalid = firstInvalid ?: binding.editPrenom
        }
        if (phone.isEmpty()) {
            binding.layoutTelephone.error = getString(R.string.error_phone_empty)
            firstInvalid = firstInvalid ?: binding.editTelephone
        } else if (!Patterns.PHONE.matcher(phone).matches()) {
            binding.layoutTelephone.error = getString(R.string.error_phone_invalid)
            firstInvalid = firstInvalid ?: binding.editTelephone
        }
        if (email.isNotEmpty() && !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.layoutEmail.error = getString(R.string.error_email_invalid)
            firstInvalid = firstInvalid ?: binding.editEmail
        }
        if (!showCoordinateErrors(coords)) {
            firstInvalid = firstInvalid ?: if (binding.layoutLatitude.error != null)
                binding.editLatitude else binding.editLongitude
        }
        if (firstInvalid != null) {
            firstInvalid.requestFocus()
            toast(R.string.msg_fix_errors)
            return
        }

        val (lat, lng) = when (coords) {
            is CoordinateValidator.Result.Valid -> coords.latitude to coords.longitude
            else -> null to null   // Empty : aucune position
        }
        val updated = original.copy(
            nom = nom, prenom = prenom, telephone = phone, email = email,
            photoPath = currentPhotoPath, latitude = lat, longitude = lng
        )
        newPhotos.filter { it != currentPhotoPath }.forEach { ImageUtils.deleteQuietly(it) }
        newPhotos.clear()

        viewModel.save(updated)            // la liste observe le ViewModel → actualisée
        toast(R.string.msg_saved)
        (requireActivity() as MainActivity).goBack()
    }

    /** Annuler : rien n'est écrit dans le ViewModel, les anciennes valeurs sont conservées. */
    private fun cancel() {
        newPhotos.forEach { ImageUtils.deleteQuietly(it) }
        newPhotos.clear()
        cameraTempFile?.delete()
        cameraTempFile = null
        (requireActivity() as MainActivity).goBack()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(STATE_INITIALIZED, formInitialized)
        outState.putString(STATE_PHOTO, currentPhotoPath)
        outState.putString(STATE_TEMP, cameraTempFile?.path)
        outState.putStringArrayList(STATE_NEW_PHOTOS, newPhotos)
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    private fun toast(resId: Int) {
        context?.let { Toast.makeText(it, resId, Toast.LENGTH_SHORT).show() }
    }

    companion object {
        const val TAG = "detail"
        private const val ARG_CONTACT_ID = "contact_id"
        private const val STATE_INITIALIZED = "state_initialized"
        private const val STATE_PHOTO = "state_photo"
        private const val STATE_TEMP = "state_temp"
        private const val STATE_NEW_PHOTOS = "state_new_photos"

        fun newInstance(contactId: Long) = DetailContactFragment().apply {
            arguments = bundleOf(ARG_CONTACT_ID to contactId)
        }
    }
}
