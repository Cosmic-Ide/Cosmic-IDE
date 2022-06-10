/*
 * Copyright (c) 2015-2017 Angelo ZERR.
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * <p>
 * SPDX-License-Identifier: EPL-2.0
 * <p>
 * Contributors:
 * Angelo Zerr <angelo.zerr@gmail.com> - initial API and implementation
 */
package io.github.rosemoe.sora.textmate.core.internal.parser.xml;

import io.github.rosemoe.sora.textmate.core.internal.parser.PList;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;
import org.xmlpull.v1.sax2.Driver;

import org.xml.sax.InputSource;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import javax.xml.XMLConstants;

public class XMLPListParser<T> {

    private final boolean theme;

    public XMLPListParser(boolean theme) {
        this.theme = theme;
    }

    public T parse(InputStream contents) throws Exception {
        XmlPullParserFactory parserFactory = XmlPullParserFactory.newInstance();
        parserFactory.setNamespaceAware(true);
        // make parser invulnerable to XXE attacks, see https://rules.sonarsource.com/java/RSPEC-2755
        parserFactory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        parserFactory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);

        XmlPullParser pullParser = parserFactory.newPullParser();
        
        // make parser invulnerable to XXE attacks, see https://rules.sonarsource.com/java/RSPEC-2755
        pullParser.setProperty("http://javax.xml.XMLConstants/property/accessExternalDTD", "");
        pullParser.setProperty("http://javax.xml.XMLConstants/property/accessExternalSchema", "");
        
        Driver driver = new Driver(pullParser);
        driver.setEntityResolver(
                (arg0, arg1) ->
                        new InputSource(
                                new ByteArrayInputStream(
                                        "<?xml version='1.0' encoding='UTF-8'?>".getBytes())));
        PList<T> result = new PList<>(theme);
        driver.setContentHandler(result);
        driver.parse(new InputSource(contents));
        return result.getResult();
    }
}
