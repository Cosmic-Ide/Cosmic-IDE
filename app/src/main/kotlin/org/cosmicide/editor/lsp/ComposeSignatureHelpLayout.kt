package org.cosmicide.editor.lsp

import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Functions
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ComposeViewContext
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.findViewTreeViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.findViewTreeSavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.mikepenz.markdown.m3.Markdown
import com.mikepenz.markdown.m3.markdownTypography
import io.github.rosemoe.sora.lsp.editor.signature.SignatureHelpLayout
import io.github.rosemoe.sora.lsp.editor.signature.SignatureHelpWindow
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme
import org.cosmicide.ui.theme.IDETheme
import org.eclipse.lsp4j.MarkupContent
import org.eclipse.lsp4j.MarkupKind
import org.eclipse.lsp4j.ParameterInformation
import org.eclipse.lsp4j.SignatureHelp
import org.eclipse.lsp4j.SignatureInformation
import org.eclipse.lsp4j.jsonrpc.messages.Either
import android.graphics.Color as AndroidColor

/**
 * Material 3 signature help hosted inside Sora's Android popup window.
 */
class ComposeSignatureHelpLayout : SignatureHelpLayout {

    private lateinit var window: SignatureHelpWindow
    private lateinit var composeView: ComposeView

    private var codeTypeface by mutableStateOf(Typeface.MONOSPACE)
    private var state by mutableStateOf(SignatureHelpState())
    private var revision by mutableIntStateOf(0)

    override fun attach(window: SignatureHelpWindow) {
        this.window = window
        window.parentView.setBackgroundColor(AndroidColor.TRANSPARENT)
    }

    override fun createView(inflater: LayoutInflater): View {
        composeView = ComposeView(inflater.context).apply {
            minimumWidth = (window.editor.dpUnit * 300f).toInt()
            minimumHeight = (window.editor.dpUnit * 180f).toInt()
            setViewTreeLifecycleOwner(window.editor.findViewTreeLifecycleOwner())
            setViewTreeViewModelStoreOwner(window.editor.findViewTreeViewModelStoreOwner())
            setViewTreeSavedStateRegistryOwner(
                window.editor.findViewTreeSavedStateRegistryOwner()
            )
            setBackgroundColor(AndroidColor.TRANSPARENT)
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            setContent {
                IDETheme {
                    val fontFamily = remember(codeTypeface) {
                        FontFamily(codeTypeface)
                    }
                    SignatureHelpCard(
                        state = state,
                        revision = revision,
                        codeFontFamily = fontFamily,
                        onPrevious = { moveSelection(-1) },
                        onNext = { moveSelection(1) }
                    )
                }
            }

            // SignatureHelpWindow measures before attaching its PopupWindow.
            createComposition(ComposeViewContext(window.editor))
        }
        return composeView
    }

    override fun applyColorScheme(
        colorScheme: EditorColorScheme,
        typeface: Typeface
    ) {
        codeTypeface = typeface
    }

    override fun renderSignatures(signatureHelp: SignatureHelp) {
        val signatures = signatureHelp.signatures.orEmpty()
        if (signatures.isEmpty()) {
            state = SignatureHelpState()
            revision++
            composeBeforeHostMeasurement()
            return
        }

        val activeSignature = (signatureHelp.activeSignature ?: 0)
            .coerceIn(0, signatures.lastIndex)
        state = SignatureHelpState(
            signatures = signatures.toList(),
            currentIndex = activeSignature,
            activeSignatureIndex = activeSignature,
            fallbackActiveParameter = signatureHelp.activeParameter ?: -1
        )
        revision++
        composeBeforeHostMeasurement()
    }

    override fun onTextSizeChanged(oldSize: Float, newSize: Float) {

    }

    private fun moveSelection(offset: Int) {
        val signatures = state.signatures
        if (signatures.size < 2) return

        state = state.copy(
            currentIndex = (state.currentIndex + offset).floorMod(signatures.size)
        )
        revision++
    }

    /**
     * SignatureHelpWindow renders and measures in the same call stack. Recreate an off-tree
     * composition immediately so that measurement observes the new signature instead of the
     * initial empty state.
     */
    private fun composeBeforeHostMeasurement() {
        if (!::composeView.isInitialized || composeView.isAttachedToWindow) return
        composeView.disposeComposition()
        composeView.createComposition(ComposeViewContext(window.editor))
    }

    private fun Int.floorMod(divisor: Int): Int {
        return ((this % divisor) + divisor) % divisor
    }
}

private data class SignatureHelpState(
    val signatures: List<SignatureInformation> = emptyList(),
    val currentIndex: Int = 0,
    val activeSignatureIndex: Int = 0,
    val fallbackActiveParameter: Int = -1
)

private data class SignatureDocumentation(
    val text: String,
    val isMarkdown: Boolean
)

@Composable
private fun SignatureHelpCard(
    state: SignatureHelpState,
    revision: Int,
    codeFontFamily: FontFamily,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    val scrollState = rememberScrollState()
    val signatures = state.signatures
    val signature = signatures.getOrNull(state.currentIndex)
    val activeParameterIndex = signature?.activeParameter
        ?: state.fallbackActiveParameter

    LaunchedEffect(revision) {
        scrollState.scrollTo(0)
    }

    Surface(
        modifier = Modifier
            .width(360.dp)
            .height(220.dp),
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 5.dp,
        shadowElevation = 10.dp
    ) {
        Column(modifier = Modifier.verticalScroll(scrollState)) {
            SignatureHeader(
                currentIndex = state.currentIndex,
                signatureCount = signatures.size,
                onPrevious = onPrevious,
                onNext = onNext
            )

            if (signature == null) {
                Text(
                    text = "No signature information",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodySmall
                )
                return@Column
            }

            SignatureLabel(
                signature = signature,
                activeParameterIndex = activeParameterIndex,
                isServerSelected = state.currentIndex == state.activeSignatureIndex,
                codeFontFamily = codeFontFamily
            )

            val activeParameter = signature.parameters
                .orEmpty()
                .getOrNull(activeParameterIndex)
            val parameterDocumentation = activeParameter?.documentation.toDocumentation()
            if (activeParameter != null && parameterDocumentation?.text?.isNotBlank() == true) {
                ParameterDocumentation(
                    parameter = activeParameter,
                    signatureLabel = signature.label,
                    documentation = parameterDocumentation,
                    codeFontFamily = codeFontFamily
                )
            }

            val signatureDocumentation = signature.documentation.toDocumentation()
            if (signatureDocumentation?.text?.isNotBlank() == true) {
                HorizontalDivider()
                Documentation(
                    documentation = signatureDocumentation
                )
            }
        }
    }
}

@Composable
private fun SignatureHeader(
    currentIndex: Int,
    signatureCount: Int,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 12.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(28.dp),
            shape = CircleShape,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Rounded.Functions,
                    contentDescription = null,
                    modifier = Modifier.size(17.dp)
                )
            }
        }
        Spacer(Modifier.width(9.dp))
        Text(
            text = "Signature help",
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold
        )

        if (signatureCount > 1) {
            IconButton(
                onClick = onPrevious,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                    contentDescription = "Previous signature",
                )
            }
            Surface(
                shape = CircleShape,
            ) {
                Text(
                    text = "${currentIndex + 1} / $signatureCount",
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
            }
            IconButton(
                onClick = onNext,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                    contentDescription = "Next signature",
                )
            }
        }
    }
}

@Composable
private fun SignatureLabel(
    signature: SignatureInformation,
    activeParameterIndex: Int,
    isServerSelected: Boolean,
    codeFontFamily: FontFamily
) {
    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
        if (isServerSelected) {
            Text(
                text = "ACTIVE OVERLOAD",
                style = MaterialTheme.typography.labelSmall
            )
            Spacer(Modifier.height(5.dp))
        }
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp)
        ) {
            SelectionContainer {
                Text(
                    text = signature.annotatedLabel(
                        activeParameterIndex = activeParameterIndex,
                        MaterialTheme.colorScheme
                    ),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    fontFamily = codeFontFamily,
                    fontSize = 12.sp,
                    overflow = TextOverflow.Visible
                )
            }
        }
    }
}

@Composable
private fun ParameterDocumentation(
    parameter: ParameterInformation,
    signatureLabel: String,
    documentation: SignatureDocumentation,
    codeFontFamily: FontFamily
) {
    val parameterLabel = parameter.displayLabel(signatureLabel)
    Column(
        modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 10.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Rounded.Info,
                contentDescription = null,
                modifier = Modifier.size(15.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = parameterLabel,
                color = MaterialTheme.colorScheme.primary,
                fontFamily = codeFontFamily,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.height(5.dp))
        DocumentationBody(
            documentation = documentation,
        )
    }
}

@Composable
private fun Documentation(
    title: String = "Documentation",
    documentation: SignatureDocumentation
) {
    Column(modifier = Modifier.padding(12.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(5.dp))
        DocumentationBody(
            documentation = documentation,
        )
    }
}

@Composable
private fun DocumentationBody(
    documentation: SignatureDocumentation,
) {
    if (documentation.isMarkdown) {
        Markdown(
            content = documentation.text,
            modifier = Modifier.fillMaxWidth(),
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
    } else {
        SelectionContainer {
            Text(
                text = documentation.text,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

private fun SignatureInformation.annotatedLabel(
    activeParameterIndex: Int,
    colorScheme: ColorScheme
): AnnotatedString {
    val signatureLabel = label.orEmpty()
    val activeParameter = parameters.orEmpty().getOrNull(activeParameterIndex)
    val activeRange = activeParameter?.labelRange(signatureLabel)

    return buildAnnotatedString {
        append(signatureLabel)
        if (signatureLabel.isNotEmpty()) {
            addStyle(
                SpanStyle(color = colorScheme.onSurface),
                start = 0,
                end = signatureLabel.length
            )
        }

        val functionNameEnd = signatureLabel.indexOf('(')
            .takeIf { it > 0 }
            ?: signatureLabel.indexOf(' ').takeIf { it > 0 }
            ?: 0
        if (functionNameEnd > 0) {
            addStyle(
                SpanStyle(fontWeight = FontWeight.SemiBold),
                start = 0,
                end = functionNameEnd
            )
        }

        if (activeRange != null && activeRange.first < activeRange.last) {
            addStyle(
                SpanStyle(
                    color = colorScheme.onSurfaceVariant,
                    background = colorScheme.surface,
                    fontWeight = FontWeight.Bold
                ),
                start = activeRange.first,
                end = activeRange.last
            )
        }
    }
}

private fun ParameterInformation.labelRange(signatureLabel: String): IntRange? {
    if (label.isRight) {
        val range = label.right ?: return null
        val start = range.first.coerceIn(0, signatureLabel.length)
        val end = range.second.coerceIn(start, signatureLabel.length)
        return start until end
    }

    val text = label.left?.takeIf(String::isNotEmpty) ?: return null
    val start = signatureLabel.indexOf(text)
    return if (start >= 0) start until (start + text.length) else null
}

private fun ParameterInformation.displayLabel(signatureLabel: String): String {
    if (label.isLeft) {
        return label.left.orEmpty()
    }
    val range = labelRange(signatureLabel) ?: return "Parameter"
    return signatureLabel.substring(range)
}

private fun Either<String, MarkupContent>?.toDocumentation(): SignatureDocumentation? {
    this ?: return null
    return if (isLeft) {
        left
            ?.takeIf(String::isNotBlank)
            ?.let { SignatureDocumentation(it, isMarkdown = true) }
    } else {
        right?.value
            ?.takeIf(String::isNotBlank)
            ?.let {
                SignatureDocumentation(
                    text = it,
                    isMarkdown = right?.kind == MarkupKind.MARKDOWN
                )
            }
    }
}
