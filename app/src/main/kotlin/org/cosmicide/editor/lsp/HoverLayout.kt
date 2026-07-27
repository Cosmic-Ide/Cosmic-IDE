package org.cosmicide.editor.lsp

import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ComposeViewContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.findViewTreeViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.findViewTreeSavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.mikepenz.markdown.m3.Markdown
import com.mikepenz.markdown.m3.markdownTypography
import io.github.rosemoe.sora.lsp.editor.hover.HoverLayout
import io.github.rosemoe.sora.lsp.editor.hover.HoverWindow
import io.github.rosemoe.sora.lsp.editor.hover.formatMarkedStringEither
import io.github.rosemoe.sora.lsp.editor.hover.formatMarkupContent
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme
import org.cosmicide.ui.theme.IDETheme
import org.eclipse.lsp4j.Hover
import android.graphics.Color as AndroidColor

/**
 * Compose hover card with a stable viewport.
 *
 * Markdown parsing can finish after Sora's one-time pre-show measurement. A minimum viewport keeps
 * the popup usable during that race, while the card's own scroll state handles the final content.
 */
class HoverLayout : HoverLayout {

    private lateinit var window: HoverWindow

    private var hoverMarkdown by mutableStateOf("")
    private var codeTypeface by mutableStateOf(Typeface.MONOSPACE)
    private var revision by mutableIntStateOf(0)

    override fun attach(window: HoverWindow) {
        this.window = window
        window.parentView.setBackgroundColor(AndroidColor.TRANSPARENT)
    }

    override fun createView(inflater: LayoutInflater): View {
        return ComposeView(inflater.context).apply {
            setViewTreeLifecycleOwner(window.editor.findViewTreeLifecycleOwner())
            setViewTreeViewModelStoreOwner(window.editor.findViewTreeViewModelStoreOwner())
            setViewTreeSavedStateRegistryOwner(
                window.editor.findViewTreeSavedStateRegistryOwner()
            )
            setBackgroundColor(AndroidColor.TRANSPARENT)
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            setContent {
                IDETheme {
                    HoverCard(
                        markdown = hoverMarkdown,
                        revision = revision,
                    )
                }
            }

            // HoverWindow measures before attaching the PopupWindow.
            createComposition(ComposeViewContext(window.editor))
        }
    }

    override fun applyColorScheme(
        colorScheme: EditorColorScheme, typeface: Typeface
    ) {
        codeTypeface = typeface
    }

    override fun renderHover(hover: Hover) {
        hoverMarkdown = buildHoverText(hover)
        revision++
    }

    override fun onTextSizeChanged(oldSize: Float, newSize: Float) {

    }

    private fun buildHoverText(hover: Hover): String {
        val contents = hover.contents ?: return ""
        return if (contents.isLeft) {
            contents.left.orEmpty().mapNotNull(::formatMarkedStringEither)
                .filter(String::isNotBlank).joinToString("\n\n")
        } else {
            formatMarkupContent(contents.right).orEmpty()
        }
    }
}

private const val MarkdownFontScale = 0.8f

@Composable
private fun HoverCard(
    markdown: String,
    revision: Int,
) {
    val scrollState = rememberScrollState()

    LaunchedEffect(revision) {
        scrollState.scrollTo(0)
    }

    Surface(
        modifier = Modifier
            .widthIn(min = 300.dp, max = 640.dp)
            .heightIn(min = 170.dp, max = 260.dp),
        shape = RoundedCornerShape(12.dp),
        tonalElevation = 5.dp,
        shadowElevation = 10.dp
    ) {
        Column(modifier = Modifier.verticalScroll(scrollState)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(28.dp),
                    shape = CircleShape,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.Code,
                            contentDescription = null,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }
                Spacer(Modifier.width(9.dp))
                Text(
                    text = "Symbol information",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }

            HorizontalDivider()

            if (markdown.isBlank()) {
                Text(
                    text = "No hover information available",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodySmall
                )
            } else {
                val density = LocalDensity.current

                CompositionLocalProvider(
                    LocalDensity provides Density(
                        density = density.density,
                        fontScale = density.fontScale * MarkdownFontScale
                    )
                ) {
                    SelectionContainer {
                        Markdown(
                            content = markdown,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            typography = markdownTypography(
                                h1 = MaterialTheme.typography.titleLargeEmphasized,
                                h2 = MaterialTheme.typography.titleMediumEmphasized,
                                h3 = MaterialTheme.typography.titleSmallEmphasized,
                                h4 = MaterialTheme.typography.headlineMedium,
                                h5 = MaterialTheme.typography.headlineSmall,
                                h6 = MaterialTheme.typography.titleLarge,
                                text = MaterialTheme.typography.bodyMedium,
                                code = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                                inlineCode = MaterialTheme.typography.bodyMedium.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = TextUnit.Unspecified
                                ),
                                quote = MaterialTheme.typography.bodyMedium.plus(SpanStyle(fontStyle = FontStyle.Italic)),
                                paragraph = MaterialTheme.typography.bodyMedium,
                                ordered = MaterialTheme.typography.bodyMedium,
                                bullet = MaterialTheme.typography.bodyMedium,
                                list = MaterialTheme.typography.bodyMedium,
                                textLink = TextLinkStyles(
                                    style = MaterialTheme.typography.bodyMediumEmphasized.copy(
                                        textDecoration = TextDecoration.Underline
                                    ).toSpanStyle()
                                ),
                                table = MaterialTheme.typography.bodyMedium
                            )
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
            }
        }
    }
}
