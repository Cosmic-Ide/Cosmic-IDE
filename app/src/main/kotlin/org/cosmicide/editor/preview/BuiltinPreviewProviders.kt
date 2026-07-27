package org.cosmicide.editor.preview

import android.net.Uri
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ImageView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import com.mikepenz.markdown.m3.Markdown
import com.mikepenz.markdown.m3.markdownTypography
import org.cosmicide.editor.EditorExtensionPoints
import org.cosmicide.editor.EditorPreviewMatchRequest
import org.cosmicide.editor.EditorPreviewPresentation
import org.cosmicide.editor.EditorPreviewProvider
import org.cosmicide.editor.EditorPreviewRenderRequest
import org.cosmicide.plugin.api.MutableExtensionRegistry
import org.cosmicide.plugin.api.PluginIds
import org.cosmicide.ui.theme.IDETheme

fun registerBuiltinPreviewExtensions(registry: MutableExtensionRegistry) {
    listOf(
        MarkdownPreviewProvider, HtmlPreviewProvider, ImagePreviewProvider
    ).forEach { provider ->
        registry.register(
            point = EditorExtensionPoints.PREVIEW_PROVIDER,
            extension = provider,
            ownerPluginId = PluginIds.CORE,
            priority = provider.priority
        )
    }
}

internal object MarkdownPreviewProvider : EditorPreviewProvider {
    override val id = "org.cosmicide.editor.preview.markdown"
    override val displayName = "Markdown preview"
    override val description = "Renders Markdown documents"
    override val priority = 100

    override fun supports(request: EditorPreviewMatchRequest): Boolean {
        return request.file.extension.lowercase() in setOf("md", "markdown")
    }

    override fun createView(request: EditorPreviewRenderRequest): View {
        return ComposeView(request.context).apply {
            setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnDetachedFromWindowOrReleasedFromPool
            )
        }
    }

    override fun updateView(view: View, request: EditorPreviewRenderRequest) {
        val composeView = view as ComposeView
        val content = request.content.orEmpty()
        val contentKey = "${request.file.absolutePath}:${content.hashCode()}"
        if (composeView.tag == contentKey) return
        composeView.tag = contentKey
        composeView.setContent {
            IDETheme {
                MarkdownPreviewContent(content)
            }
        }
    }

    override fun releaseView(view: View) {
        (view as ComposeView).disposeComposition()
    }
}

@Composable
private fun MarkdownPreviewContent(content: String) {
    val scrollState = rememberScrollState()
    Surface(
        modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface
    ) {
        SelectionContainer {
            Markdown(
                content = content,
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(18.dp),
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
                        fontFamily = FontFamily.Monospace, fontSize = TextUnit.Unspecified
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
}

internal object HtmlPreviewProvider : EditorPreviewProvider {
    override val id = "org.cosmicide.editor.preview.html"
    override val displayName = "HTML preview"
    override val description = "Renders HTML documents"
    override val priority = 100

    override fun supports(request: EditorPreviewMatchRequest): Boolean {
        return request.file.extension.lowercase() in setOf("html", "htm")
    }

    override fun createView(request: EditorPreviewRenderRequest): View {
        return WebView(request.context).apply {
            setBackgroundColor(request.backgroundColor)
            webViewClient = WebViewClient()
            settings.javaScriptEnabled = false
            settings.allowFileAccess = true
        }
    }

    override fun updateView(view: View, request: EditorPreviewRenderRequest) {
        val webView = view as WebView
        val content = request.content.orEmpty()
        val contentKey = "${request.file.absolutePath}:${content.hashCode()}"
        webView.setBackgroundColor(request.backgroundColor)
        if (webView.tag == contentKey) return
        webView.tag = contentKey
        webView.loadDataWithBaseURL(
            request.file.parentFile?.toURI()?.toString(),
            content,
            "text/html",
            Charsets.UTF_8.name(),
            null
        )
    }

    override fun releaseView(view: View) {
        (view as WebView).apply {
            stopLoading()
            loadUrl("about:blank")
            destroy()
        }
    }
}

internal object ImagePreviewProvider : EditorPreviewProvider {
    override val id = "org.cosmicide.editor.preview.image"
    override val displayName = "Image preview"
    override val description = "Displays image files without opening them as text"
    override val priority = 100
    override val canDisable = false
    override val presentation = EditorPreviewPresentation.PREVIEW_ONLY

    override fun supports(request: EditorPreviewMatchRequest): Boolean {
        val extension = request.file.extension.lowercase()
        return extension != "svg" && extension in PreviewImageExtensions
    }

    override fun createView(request: EditorPreviewRenderRequest): View {
        return ImageView(request.context).apply {
            setBackgroundColor(request.backgroundColor)
            scaleType = ImageView.ScaleType.FIT_CENTER
        }
    }

    override fun updateView(view: View, request: EditorPreviewRenderRequest) {
        val imageView = view as ImageView
        imageView.setBackgroundColor(request.backgroundColor)
        imageView.contentDescription = "Preview of ${request.file.name}"
        if (imageView.tag == request.file.absolutePath) return
        imageView.tag = request.file.absolutePath
        imageView.setImageURI(Uri.fromFile(request.file))
    }

    override fun releaseView(view: View) {
        (view as ImageView).setImageDrawable(null)
    }
}

private val PreviewImageExtensions = setOf(
    "png",
    "jpg",
    "jpeg",
    "jpe",
    "jfif",
    "gif",
    "apng",
    "webp",
    "bmp",
    "tif",
    "tiff",
    "heic",
    "heif",
    "avif",
    "ico",
    "dng",
    "psd"
)
