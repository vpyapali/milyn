/*
	Milyn - Copyright (C) 2006

	This library is free software; you can redistribute it and/or
	modify it under the terms of the GNU Lesser General Public
	License (version 2.1) as published by the Free Software
	Foundation.

	This library is distributed in the hope that it will be useful,
	but WITHOUT ANY WARRANTY; without even the implied warranty of
	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.

	See the GNU Lesser General Public License for more details:
	http://www.gnu.org/licenses/lgpl.txt
*/
package org.milyn.templating.bugfixes.MILYN_285;

import junit.framework.TestCase;
import org.milyn.Smooks;
import org.milyn.payload.StringResult;
import org.xml.sax.SAXException;
import org.custommonkey.xmlunit.XMLUnit;

import javax.xml.transform.stream.StreamSource;
import java.io.IOException;

/**
 * @author <a href="mailto:tom.fennelly@jboss.com">tom.fennelly@jboss.com</a>
 */
public class MILYN285Test extends TestCase {

    public void test() throws IOException, SAXException {
        Smooks smooks = new Smooks(getClass().getResourceAsStream("smooks-config.xml"));
        StringResult result = new StringResult();

        smooks.filter(new StreamSource(getClass().getResourceAsStream("message.xml")), result);
        XMLUnit.compareXML("<root>\n" +
                "\t<abc>def</abc>\n" +
                "\t<bla>\n" +
                "\t\tdef\n" +
                "\t</bla>\n" +
                "</root>", result.toString());
    }
}