/************************************************************************************
 * This file is part of Java Language Server (https://github.com/itsaky/java-language-server)
 *
 * Copyright (C) 2021 Akash Yadav
 *
 * Java Language Server is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Java Language Server is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Java Language Server.  If not, see <https://www.gnu.org/licenses/>.
 *
 **************************************************************************************/

package org.javacs;

import com.sun.source.doctree.DocCommentTree;
import com.sun.source.doctree.DocTree;

import org.eclipse.lsp4j.MarkupContent;
import org.eclipse.lsp4j.MarkupKind;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.CharBuffer;
import java.util.List;
import java.util.StringJoiner;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

public class MarkdownHelper {

    private static final Pattern HTML_TAG = Pattern.compile("<(\\w+)[^>]*>");
    private static final Logger LOG = Logger.getLogger("main");

    public static MarkupContent asMarkupContent(DocCommentTree comment) {
        var markdown = asMarkdown(comment);
        var content = new MarkupContent();
        content.setKind(MarkupKind.MARKDOWN);
        content.setValue(markdown);
        return content;
    }

    public static String asMarkdown(DocCommentTree comment) {
        var lines = comment.getFirstSentence();
        return asMarkdown(lines);
    }

    private static String asMarkdown(List<? extends DocTree> lines) {
        var join = new StringJoiner("\n");
        for (var l : lines) join.add(l.toString());
        var html = join.toString();
        return asMarkdown(html);
    }

    private static Document parse(String html) {
        try {
            var xml = "<wrapper>" + html + "</wrapper>";
            var factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(false);
            var builder = factory.newDocumentBuilder();
            return builder.parse(new InputSource(new StringReader(xml)));
        } catch (ParserConfigurationException | SAXException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void replaceNodes(Document doc, String tagName, Function<String, String> replace) {
        var nodes = doc.getElementsByTagName(tagName);
        while (nodes.getLength() > 0) {
            var node = nodes.item(0);
            var parent = node.getParentNode();
            var text = replace.apply(node.getTextContent().trim());
            var replacement = doc.createTextNode(text);
            parent.replaceChild(replacement, node);
            nodes = doc.getElementsByTagName(tagName);
        }
    }

    private static String print(Document doc) {
        try {
            var tf = TransformerFactory.newInstance();
            var transformer = tf.newTransformer();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            var writer = new StringWriter();
            transformer.transform(new DOMSource(doc), new StreamResult(writer));
            var wrapped = writer.getBuffer().toString();
            return wrapped.substring("<wrapper>".length(), wrapped.length() - "</wrapper>".length());
        } catch (TransformerException e) {
            throw new RuntimeException(e);
        }
    }

    private static void check(CharBuffer in, char expected) {
        var head = in.get();
        if (head != expected) {
            throw new RuntimeException(String.format("want `%s` got `%s`", expected, head));
        }
    }

    private static boolean empty(CharBuffer in) {
        return in.position() == in.limit();
    }

    private static char peek(CharBuffer in) {
        return in.get(in.position());
    }

    private static String parseTag(CharBuffer in) {
        check(in, '@');
        var tag = new StringBuilder();
        while (!empty(in) && Character.isAlphabetic(peek(in))) {
            tag.append(in.get());
        }
        return tag.toString();
    }

    private static void parseBlock(CharBuffer in, StringBuilder out) {
        check(in, '{');
        if (peek(in) == '@') {
            var tag = parseTag(in);
            if (peek(in) == ' ') in.get();
            switch (tag) {
                case "code":
                case "link":
                case "linkplain":
                    out.append("`");
                    parseInner(in, out);
                    out.append("`");
                    break;
                case "literal":
                    parseInner(in, out);
                    break;
                default:
                    LOG.warning(String.format("Unknown tag `@%s`", tag));
                    parseInner(in, out);
            }
        } else {
            parseInner(in, out);
        }
        check(in, '}');
    }

    private static void parseInner(CharBuffer in, StringBuilder out) {
        while (!empty(in)) {
            switch (peek(in)) {
                case '{':
                    parseBlock(in, out);
                    break;
                case '}':
                    return;
                default:
                    out.append(in.get());
            }
        }
    }

    private static void parse(CharBuffer in, StringBuilder out) {
        while (!empty(in)) {
            parseInner(in, out);
        }
    }

    private static String replaceTags(String in) {
        var out = new StringBuilder();
        parse(CharBuffer.wrap(in), out);
        return out.toString();
    }

    private static String htmlToMarkdown(String html) {
        html = replaceTags(html);

        var doc = parse(html);

        replaceNodes(doc, "i", contents -> String.format("*%s*", contents));
        replaceNodes(doc, "b", contents -> String.format("**%s**", contents));
        replaceNodes(doc, "pre", contents -> String.format("`%s`", contents));
        replaceNodes(doc, "code", contents -> String.format("`%s`", contents));
        replaceNodes(doc, "a", contents -> contents);

        return print(doc);
    }

    private static boolean isHtml(String text) {
        var tags = HTML_TAG.matcher(text);
        while (tags.find()) {
            var tag = tags.group(1);
            var close = String.format("</%s>", tag);
            var findClose = text.indexOf(close, tags.end());
            if (findClose != -1) return true;
        }
        return false;
    }

    /**
     * If `commentText` looks like HTML, convert it to markdown
     */
    static String asMarkdown(String commentText) {
        if (isHtml(commentText)) {
            commentText = htmlToMarkdown(commentText);
        }
        commentText = replaceTags(commentText);
        return commentText;
    }
}
