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
package org.milyn.cdr.xsd11.extensiontests;

import junit.framework.TestCase;
import org.milyn.Smooks;
import org.milyn.cdr.SmooksConfigurationException;
import org.milyn.payload.StringResult;
import org.milyn.payload.StringSource;
import org.milyn.xml.XmlUtil;
import org.milyn.xml.XsdDOMValidator;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;

/**
 * @author <a href="mailto:tom.fennelly@jboss.com">tom.fennelly@jboss.com</a>
 */
public class XSD11ExtendTest extends TestCase {

    public void test_validation() throws IOException, SAXException, ParserConfigurationException {
        Document configDoc = XmlUtil.parseStream(getClass().getResourceAsStream("config_01.xml"));
        XsdDOMValidator validator = new XsdDOMValidator(configDoc);

        assertEquals("http://www.milyn.org/xsd/smooks-1.1.xsd", validator.getDefaultNamespace().toString());
        assertEquals("[http://www.milyn.org/xsd/smooks-1.1.xsd, http://www.milyn.org/xsd/smooks/test-xsd-01.xsd]", validator.getNamespaces().toString());

        validator.validate();
    }

    public void test_digest_01_simple() throws IOException, SAXException {
        Smooks smooks = new Smooks(getClass().getResourceAsStream("config_01.xml"));
        StringResult result = new StringResult();

        smooks.filter(new StringSource("<a><b/><c/><d/></a>"), result);
        assertEquals("<a><c></c><c></c><d></d></a>", result.getResult());
    }

    public void test_digest_02_simple_import() throws IOException, SAXException {
        Smooks smooks = new Smooks(getClass().getResourceAsStream("config_02.xml"));
        StringResult result = new StringResult();

        smooks.filter(new StringSource("<a><b/><c/></a>"), result);
        assertEquals("<a><c></c><b></b></a>", result.getResult());
    }

    public void test_digest_03_simple_invalid_condition() throws IOException, SAXException {
        try {
            new Smooks(getClass().getResourceAsStream("config_03.xml"));
            fail("Expected SmooksConfigurationException");
        } catch (SmooksConfigurationException e) {
            assertEquals("Failed to construct Smooks instance for processing extended configuration resource '/META-INF/xsd/smooks/test-xsd-03.xsd-smooks.xml'.", e.getMessage());
            assertEquals("Configuration element 'condition' not supported in an extension configuration.", e.getCause().getMessage());
        }
    }

    public void test_digest_04_simple_invalid_profiles() throws IOException, SAXException {
        try {
            new Smooks(getClass().getResourceAsStream("config_04.xml"));
            fail("Expected SmooksConfigurationException");
        } catch (SmooksConfigurationException e) {
            assertEquals("Failed to construct Smooks instance for processing extended configuration resource '/META-INF/xsd/smooks/test-xsd-04.xsd-smooks.xml'.", e.getMessage());
            assertEquals("Configuration element 'profiles' not supported in an extension configuration.", e.getCause().getMessage());
        }
    }
}
