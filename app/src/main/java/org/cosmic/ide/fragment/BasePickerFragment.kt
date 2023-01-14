package org.cosmic.ide.fragment

import android.content.Intent
import android.os.Environment
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.core.provider.DocumentsContractCompat
import androidx.core.provider.DocumentsContractCompat.buildDocumentUriUsingTree
import androidx.core.provider.DocumentsContractCompat.getTreeDocumentId
import androidx.documentfile.provider.DocumentFile
import androidx.fragment.app.Fragment
import org.cosmic.ide.util.AndroidUtilities
import java.io.File

abstract class BasePickerFragment : Fragment() {

    private var callback: OnDirectoryPickedCallback? = null

    companion object {
        const val ANDROID_DOCS_AUTHORITY = "com.android.externalstorage.documents"
    }

    private val startForResult =
        registerForActivityResult(StartActivityForResult()) {
            val context = requireContext()
            val uri = it?.data?.data ?: return@registerForActivityResult
            val pickedDir = DocumentFile.fromTreeUri(context, uri)

            if (pickedDir == null) {
                AndroidUtilities.showToast("Invalid data from intent")
                return@registerForActivityResult
            }

            if (!pickedDir.exists()) {
                AndroidUtilities.showToast("The selected folder does not exist")
                return@registerForActivityResult
            }

            val docUri = buildDocumentUriUsingTree(uri, getTreeDocumentId(uri)!!)!!
            val docId = DocumentsContractCompat.getDocumentId(docUri)!!
            val authority = docUri.authority

            if (!ANDROID_DOCS_AUTHORITY.equals(authority)) {
                AndroidUtilities.showToast("This authority is not allowed")
                return@registerForActivityResult
            }

            val split = docId.split(':')
            if ("primary" != split[0]) {
                AndroidUtilities.showToast("Please select a directory from the primary storage")
                return@registerForActivityResult
            }
            val dir = File(Environment.getExternalStorageDirectory(), split[1])

            if (!dir.exists() || !dir.isDirectory) {
                AndroidUtilities.showToast("The selected file does not exist or is not a folder")
                return@registerForActivityResult
            }

            if (callback != null) {
                callback!!.onDirectoryPicked(dir)
            }
        }

    protected fun pickDirectory(callback: OnDirectoryPickedCallback?) {
        this.callback = callback
        this.startForResult.launch(Intent(Intent.ACTION_OPEN_DOCUMENT_TREE))
    }

    fun interface OnDirectoryPickedCallback {
        fun onDirectoryPicked(file: File)
    }
}