package org.cosmicide.rewrite.editor

/**
 * Enum class for the supported editor languages.
 *
 * @property source The string identifier for the language in the text editor.
 */
enum class EditorLanguage(val source: String) {
    KOTLIN("source.kotlin"),
    JAVA("source.java")
}