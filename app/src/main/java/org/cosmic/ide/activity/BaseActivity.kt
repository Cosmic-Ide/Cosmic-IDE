package org.cosmic.ide.activity

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.view.MenuItem
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.appcompat.app.AppCompatActivity
import androidx.core.provider.DocumentsContractCompat
import androidx.core.provider.DocumentsContractCompat.buildDocumentUriUsingTree
import androidx.core.provider.DocumentsContractCompat.getTreeDocumentId
import androidx.core.view.WindowCompat
import androidx.documentfile.provider.DocumentFile
import org.cosmic.ide.R
import org.cosmic.ide.ui.preference.Settings
import org.cosmic.ide.util.addSystemWindowInsetToPadding
import org.cosmic.ide.util.AndroidUtilities
import java.io.File

abstract class BaseActivity : AppCompatActivity() {

    protected val settings: Settings by lazy { Settings() }
    private var callback: OnDirectoryPickedCallback? = null

    companion object {
        const val ANDROID_DOCS_AUTHORITY = "com.android.externalstorage.documents"
    }

    private val startForResult =
        registerForActivityResult(StartActivityForResult()) {
            val context = this@BaseActivity
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

    override fun onCreate(savedInstanceState: Bundle?) {
        val isDynamic = settings.isDynamicTheme
        when {
            isDynamic -> setTheme(R.style.Theme_CosmicIde_Monet)
        }
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        getRootActivityView().addSystemWindowInsetToPadding(
            left = true,
            right = true
        )
        val callback: OnBackPressedCallback = object : OnBackPressedCallback(
            true
        ) {
            override fun handleOnBackPressed() {
                if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q &&
                    isTaskRoot &&
                    supportFragmentManager.backStackEntryCount == 0
                ) {
                    finishAfterTransition()
                }
                finish()
            }
        }
        onBackPressedDispatcher.addCallback(
            this,
            callback
        )
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                onBackPressedDispatcher.onBackPressed()
            }
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    private fun getRootActivityView(): View =
        window.decorView.findViewById(android.R.id.content)

    protected fun pickDirectory(callback: OnDirectoryPickedCallback?) {
        this.callback = callback
        this.startForResult.launch(Intent(Intent.ACTION_OPEN_DOCUMENT_TREE))
    }

    fun interface OnDirectoryPickedCallback {
        fun onDirectoryPicked(file: File)
    }
}