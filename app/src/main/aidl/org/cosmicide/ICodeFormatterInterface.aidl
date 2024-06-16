// ICodeFormatterInterface.aidl
package org.cosmicide;

// Declare any non-default types here with import statements

interface ICodeFormatterInterface {
    String formatCode(String code, int indentWidth, boolean tabIndent);

    String formatPartialCode(String code, int indentWidth, int indentLevel, boolean tabIndent);
}
