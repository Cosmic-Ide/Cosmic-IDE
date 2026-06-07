/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is a free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * Cosmic IDE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with Cosmic IDE. If not, see <https://www.gnu.org/licenses/>.
 */

package org.cosmicide.fragment

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.commit
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import org.cosmicide.R
import org.cosmicide.common.BaseBindingFragment
import org.cosmicide.databinding.InstallResourcesFragmentBinding
import org.cosmicide.rewrite.util.FileUtil
import org.cosmicide.util.Download
import org.cosmicide.util.ResourceUtil
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.GZIPInputStream

class InstallResourcesFragment : BaseBindingFragment<InstallResourcesFragmentBinding>() {

    private val client = OkHttpClient()

    private val rawUrl = "https://github.com/Cosmic-Ide/binaries/raw/main/"
    private val jdkUrl =
        "https://download.java.net/java/early_access/jdk27/24/GPL/openjdk-27-ea+24_linux-aarch64_bin.tar.gz"

    override fun getViewBinding() = InstallResourcesFragmentBinding.inflate(layoutInflater)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.installResourcesButton.setOnClickListener {
            startResourceInstallationFlow()
        }
    }

    private fun startResourceInstallationFlow() {
        setUiLoadingState(isLoading = true)

        lifecycleScope.launch {
            // 1. Download and process standard IDE resources
            val missingResources = withContext(Dispatchers.IO) { ResourceUtil.missingResources() }
            for (res in missingResources) {
                binding.installResourcesText.text = "Preparing resource: $res"
                resetProgress()

                if (!installResource(
                        rawUrl + res.substringAfterLast('/'),
                        File(FileUtil.dataDir, res)
                    )
                ) {
                    return@launch
                }
            }

            // 2. Download and Extract JDK 27
            binding.installResourcesText.text = "Downloading JDK 27..."
            resetProgress()

            val jdkArchiveFile = File(FileUtil.dataDir, "jdk27_archive.tar.gz")
            val downloadSuccess = installResource(jdkUrl, jdkArchiveFile)

            if (!downloadSuccess) {
                return@launch
            }

            // Extracting the JDK
            binding.installResourcesText.text = "Extracting JDK 27..."
            val extractionSuccess = withContext(Dispatchers.IO) {
                extractJdkFolder(jdkArchiveFile, FileUtil.dataDir)
            }

            if (!extractionSuccess) {
                binding.installResourcesText.text = "Failed to extract JDK 27."
                setUiLoadingState(isLoading = false)
                return@launch
            }

            // Clean up the downloaded archive file to save disk space
            if (jdkArchiveFile.exists()) jdkArchiveFile.delete()

            // Navigate to next screen on complete success
            parentFragmentManager.commit {
                replace(R.id.fragment_container, ProjectFragment())
            }
        }
    }

    private suspend fun installResource(url: String, destinationFile: File): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                if (destinationFile.exists()) destinationFile.delete()

                Download(client, url) { progress ->
                    lifecycleScope.launch(Dispatchers.Main) {
                        if (progress in 0..100) {
                            binding.installResourcesProgressText.text = "$progress%"
                            binding.installResourcesProgress.progress = progress
                        } else {
                            binding.installResourcesProgressText.text = ""
                            binding.installResourcesProgress.isIndeterminate = true
                        }
                    }
                }.start(destinationFile)
                true
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    binding.installResourcesText.text = "Download failed: ${e.localizedMessage}"
                    setUiLoadingState(isLoading = false)
                }
                false
            }
        }
    }

    /**
     * Extracts the "jdk-27" root directory and its contents from the tar.gz archive into targetDir.
     */
    private fun extractJdkFolder(tarGzFile: File, targetDir: File): Boolean {
        return try {
            GZIPInputStream(FileInputStream(tarGzFile)).use { gisIn ->
                val buffer = ByteArray(8192)
                var bytesRead: Int

                while (true) {
                    val header = ByteArray(512)
                    var offset = 0
                    while (offset < 512) {
                        val read = gisIn.read(header, offset, 512 - offset)
                        if (read == -1) break
                        offset += read
                    }
                    if (offset < 512 || header[0].toInt() == 0) break // End of tar archive

                    // Read entry file name (first 100 bytes of tar header)
                    val name = String(header, 0, 100).trim { it <= ' ' || it.code == 0 }
                    if (name.isEmpty()) continue

                    // Parse file size from Octal representation in header (offset 124, length 12)
                    val sizeString = String(header, 124, 12).trim { it <= ' ' || it.code == 0 }
                    val fileSize = sizeString.toLongOrNull(8) ?: 0L

                    // Check if the file is part of the targeted "jdk-27/" directory hierarchy
                    if (name.startsWith("jdk-27/")) {
                        // Keeping 'name' intact means it will create a folder called "jdk-27" inside targetDir
                        val targetFile = File(targetDir, name)

                        if (name.endsWith("/")) {
                            targetFile.mkdirs()
                        } else {
                            targetFile.parentFile?.mkdirs()
                            FileOutputStream(targetFile).use { fos ->
                                var remaining = fileSize
                                while (remaining > 0) {
                                    val toRead = minOf(remaining, buffer.size.toLong()).toInt()
                                    bytesRead = gisIn.read(buffer, 0, toRead)
                                    if (bytesRead == -1) break
                                    fos.write(buffer, 0, bytesRead)
                                    remaining -= bytesRead
                                }
                            }
                        }
                    } else {
                        // Skip file contents if it belongs to any other unintended directory blocks
                        var remaining = fileSize
                        while (remaining > 0) {
                            val toRead = minOf(remaining, buffer.size.toLong()).toInt()
                            bytesRead = gisIn.read(buffer, 0, toRead)
                            if (bytesRead == -1) break
                            remaining -= bytesRead
                        }
                    }

                    // Tar records are aligned to 512-byte blocks; skip trailing padding bytes
                    val padding = (512 - (fileSize % 512)) % 512
                    var skipped = 0L
                    while (skipped < padding) {
                        val skipRead = gisIn.read(
                            buffer,
                            0,
                            minOf(padding - skipped, buffer.size.toLong()).toInt()
                        )
                        if (skipRead == -1) break
                        skipped += skipRead
                    }
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun setUiLoadingState(isLoading: Boolean) {
        binding.installResourcesButton.isEnabled = !isLoading
        binding.installResourcesProgress.isVisible = isLoading
        binding.installResourcesProgressText.isVisible = isLoading
    }

    private fun resetProgress() {
        binding.installResourcesProgress.isIndeterminate = false
        binding.installResourcesProgress.progress = 0
        binding.installResourcesProgressText.text = "0%"
    }
}