/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is a free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * Cosmic IDE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with Foobar. If not, see <https://www.gnu.org/licenses/>.
 */

package org.cosmicide.rewrite.fragment

import android.os.Bundle
import android.view.View
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.cosmicide.rewrite.R
import org.cosmicide.rewrite.common.BaseBindingFragment
import org.cosmicide.rewrite.databinding.InstallResourcesFragmentBinding
import org.cosmicide.rewrite.util.Download
import org.cosmicide.rewrite.util.FileUtil
import org.cosmicide.rewrite.util.ResourceUtil

class InstallResourcesFragment : BaseBindingFragment<InstallResourcesFragmentBinding>() {

    val rawUrl = "https://github.com/Cosmic-Ide/binaries/raw/main/"
    override fun getViewBinding() = InstallResourcesFragmentBinding.inflate(layoutInflater)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.installResourcesButton.setOnClickListener {
            lifecycleScope.launch(Dispatchers.IO) {
                for (res in ResourceUtil.missingResources()) {
                    lifecycleScope.launch(Dispatchers.Main) {
                        binding.installResourcesText.text = "Preparing resource $res"
                    }
                    installResource(res)
                    lifecycleScope.launch(Dispatchers.Main) {
                        binding.installResourcesText.text = "Downloaded resource $res"
                    }
                }
                lifecycleScope.launch(Dispatchers.Main) {
                    parentFragmentManager.beginTransaction().apply {
                        replace(R.id.fragment_container, ProjectFragment())
                        setTransition(androidx.fragment.app.FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                    }.commit()
                }
            }
        }
    }

    fun installResource(res: String) {
        val url = rawUrl + res.substringAfterLast('/')
        val file = FileUtil.dataDir.resolve(res)
        file.parentFile!!.mkdirs()
        file.createNewFile()
        Download(url) {
            lifecycleScope.launch(Dispatchers.Main) {
                binding.installResourcesProgressText.text = "$it%"
                binding.installResourcesProgress.progress = it
            }
        }.start(file)
    }
}
