/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is a free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * Cosmic IDE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with Cosmic IDE. If not, see <https://www.gnu.org/licenses/>.
 */

package org.cosmicide.fragment.settings

import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.FragmentActivity
import de.Maxr1998.modernpreferences.PreferenceScreen
import de.Maxr1998.modernpreferences.helpers.editText
import de.Maxr1998.modernpreferences.helpers.seekBar
import de.Maxr1998.modernpreferences.helpers.singleChoice
import de.Maxr1998.modernpreferences.preferences.choice.SelectionItem
import org.cosmicide.R
import org.cosmicide.util.PreferenceKeys

class GeminiSettings(private val activity: FragmentActivity) : SettingsProvider {

    private val temperature: List<Float>
        get() = listOf(
            0.0f, 0.1f, 0.2f, 0.3f, 0.4f, 0.5f,
            0.6f, 0.7f, 0.8f, 0.9f, 1.0f
        )

    private val keys = temperature.map { SelectionItem(it.toString(), it.toString(), null) }

    override fun provideSettings(builder: PreferenceScreen.Builder) {
        builder.apply {
            icon = ResourcesCompat.getDrawable(
                activity.resources,
                R.drawable.outline_forum_24,
                activity.theme
            )

            editText(PreferenceKeys.GEMINI_API_KEY) {
                title = "Gemini API Key"
                summary =
                    "The API key to connect to the Gemini API. You can get one at https://makersuite.google.com/app/apikey"
            }

            editText(PreferenceKeys.GEMINI_MODEL) {
                title = "Gemini Model"
                summary =
                    "The model to use for Gemini. Default is 'gemini-2.0-flash'. Do not change this unless you know what you are doing."
                defaultValue = "gemini-2.0-flash"
            }

            singleChoice(PreferenceKeys.TEMPERATURE, keys) {
                title = "temperature"
                summary =
                    "Controls the randomness of the output. A value closer to 1.0 will produce responses that are more varied and creative, while a value closer to 0.0 will typically result in more straightforward responses from the model."
                initialSelection = "0.9"
            }

            singleChoice(PreferenceKeys.TOP_P, keys) {
                title = "top_p"
                summary =
                    "The maximum cumulative probability of tokens to consider when sampling. Tokens are sorted based on their assigned probabilities so that only the most likely tokens are considered. Top-k sampling directly limits the maximum number of tokens to consider, while Nucleus sampling limits number of tokens based on the cumulative probability."
                initialSelection = "1.0"
            }

            seekBar(PreferenceKeys.TOP_K) {
                title = "top_k"
                summary = "top_k sets the maximum number of tokens to sample from on each step."
                max = 60
                min = 1
                default = 1
            }


            seekBar(PreferenceKeys.MAX_TOKENS) {
                title = "max_tokens"
                summary = "max_tokens sets the maximum number of tokens to generate."
                max = 2048
                min = 60
                default = 1024
            }
        }
    }
}
