package org.cosmic.ide.fragment.settings

import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import androidx.lifecycle.ViewModelProvider
import org.cosmic.ide.R
import org.cosmic.ide.activity.model.GitViewModel

class GitSettingsFragment :
    BasePreferenceFragment(R.string.git),
    SharedPreferences.OnSharedPreferenceChangeListener {

    val mGitViewModel: GitViewModel by lazy {
        ViewModelProvider(requireActivity()).get(GitViewModel::class.java)
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.pref_git)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        settings.subscribe(this)
    }

    override fun onDestroyView() {
        settings.unsubscribe(this)
        super.onDestroyView()
    }

    override fun onSharedPreferenceChanged(prefs: SharedPreferences?, key: String?) {
        when (key) {
            "git_username" -> {
                val userName = settings.gitUserName

                mGitViewModel.apply {
                    gitLog.value = "The username has been changed to \"$userName\""
                }
            }
            "git_useremail" -> {
                val userEmail = settings.gitUserEmail

                mGitViewModel.apply {
                    gitLog.value = "The email address has been changed to \"$userEmail\""
                }
            }
        }
    }
}