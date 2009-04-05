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

package org.milyn.edisax.v1_2.model;

import static org.milyn.io.StreamUtils.readStream;
import junit.framework.TestCase;
import org.milyn.edisax.model.EDIConfigDigester;
import org.milyn.edisax.model.internal.*;
import org.milyn.edisax.EDIConfigurationException;
import org.xml.sax.SAXException;

import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * This testcase tests that all new elements introduced in version 1.2 is digested from
 * configurationfile.
 *
 * @author bardl
 */
public class EDIConfigDigesterTest extends TestCase {

    /**
     * This testcase tests that all values are read from ValueNode.
     * @throws org.milyn.edisax.EDIConfigurationException is thrown when error occurs during config-digestion.
     * @throws java.io.IOException is thrown when unable to read edi-config in testcase.
     * @throws org.xml.sax.SAXException is thrown when error occurs during config-digestion.
     */
    public void testReadValueNodes() throws IOException, EDIConfigurationException, SAXException {
        InputStream input = new ByteArrayInputStream(readStream(getClass().getResourceAsStream("edi-config-all-new-elements.xml")));
        Edimap edimap = EDIConfigDigester.digestConfig(input);

        Segment segment = (Segment)edimap.getSegments().getSegments().get(0).getSegments().get(0);
        List<Field> fields = segment.getFields();

        // Assert field is read correctly.
        // <medi:field xmltag="aTime" type="Time" format="HHmm" minLength="0" maxLength="4"/>
        assertEquals("Failed to digest type-attribute for Field", fields.get(0).getType(), "Time");
        assertEquals("Failed to digest parameters-attribute for Field", fields.get(0).getParameters().get(0).getKey(), "format");
        assertEquals("Failed to digest parameters-attribute for Field", fields.get(0).getParameters().get(0).getValue(), "HHmm");
        assertEquals("Failed to digest minLength-attribute for Field", fields.get(0).getMinLength(), new Integer(0));
        assertEquals("Failed to digest maxLength-attribute for Field", fields.get(0).getMaxLength(), new Integer(4));

        // Assert Component is read correctly.
        // <medi:component xmltag="aBinary" required="true" type="Binary" minLength="0" maxLength="8"/>
        Component component = fields.get(1).getComponent().get(0);
        assertEquals("Failed to digest type-attribute for Component", component.getType(), "Binary");
        assertNull("Parameters-attribute should be null in Component", component.getParameters());
        assertEquals("Failed to digest minLength-attribute for Component", component.getMinLength(), new Integer(0));
        assertEquals("Failed to digest maxLength-attribute for Component", component.getMaxLength(), new Integer(8));

        // Assert SubComponent is read correctly.
        // <medi:sub-component xmltag="aNumeric" type="Numeric" format="#0.00" minLength="1" maxLength="4"/>
        SubComponent subcomponent = fields.get(1).getComponent().get(1).getSubComponent().get(0);
        assertEquals("Failed to digest type-attribute for SubComponent", subcomponent.getType(), "Numeric");
        assertEquals("Failed to digest parameters-attribute for SubComponent", subcomponent.getParameters().get(0).getKey(), "format");
        assertEquals("Failed to digest format-attribute for SubComponent", subcomponent.getParameters().get(0).getValue(), "#0.00");
        assertEquals("Failed to digest minLength-attribute for SubComponent", subcomponent.getMinLength(), new Integer(1));
        assertEquals("Failed to digest maxLength-attribute for SubComponent", subcomponent.getMaxLength(), new Integer(4));
    }

    /**
     * This testcase tests that description attribute is read from Segment.
     * @throws org.milyn.edisax.EDIConfigurationException is thrown when error occurs during config-digestion.
     * @throws java.io.IOException is thrown when unable to read edi-config in testcase.
     * @throws org.xml.sax.SAXException is thrown when error occurs during config-digestion.
     */
    public void testReadSegmentDescription() throws IOException, EDIConfigurationException, SAXException {
        InputStream input = new ByteArrayInputStream(readStream(getClass().getResourceAsStream("edi-config-all-new-elements.xml")));
        Edimap edimap = EDIConfigDigester.digestConfig(input);

        Segment segment = (Segment)edimap.getSegments().getSegments().get(0).getSegments().get(0);
        String expected = "This segment is used for testing all new elements in v.1.2";
        assertEquals("Description in segment [" + segment.getDescription() + "] doesn't match expected value [" + expected + "].", segment.getDescription(), expected);
    }

    public void testCorrectParametersNoCustomType() throws IOException, SAXException, EDIConfigurationException {
        InputStream input = new ByteArrayInputStream(readStream(getClass().getResourceAsStream("edi-config-correct-no-custom-parameter.xml")));

        Edimap edimap = EDIConfigDigester.digestConfig(input);

        Segment segment = (Segment)edimap.getSegments().getSegments().get(0);
        Field field = segment.getFields().get(0);

        assertEquals("Number of parameters in list [" + field.getParameters().size() + "] doesn't match expected value [2].", field.getParameters().size(), 2);

        String expected = "format";
        String value = field.getParameters().get(0).getKey();
        assertEquals("Key in parameters [" + value + "] doesn't match expected value [" + expected + "].", value, expected);

        expected = "yyyyMMdd";
        value = field.getParameters().get(0).getValue();
        assertEquals("Value in parameters [" + value + "] doesn't match expected value [" + expected + "].", value, expected);

        expected = "param2";
        value = field.getParameters().get(1).getKey();
        assertEquals("Key in parameters [" + value + "] doesn't match expected value [" + expected + "].", value, expected);

        expected = "value2";
        value = field.getParameters().get(1).getValue(); 
        assertEquals("Value in parameters [" + value + "] doesn't match expected value [" + expected + "].", value, expected);

    }

    public void testIncorrectParametersNoCustomType() throws IOException, SAXException {
        InputStream input = new ByteArrayInputStream(readStream(getClass().getResourceAsStream("edi-config-incorrect-no-custom-parameter.xml")));
        
        try {
            EDIConfigDigester.digestConfig(input);
            assertTrue("EDIConfigDigester should fail for test configuration.", false);
        } catch (EDIConfigurationException e) {
            String expected = "Invalid use of paramaters in ValueNode. A parameter-entry should consist of a key-value-pair separated with the '='-character. Example: [parameters=\"key1=value1;key2=value2\"]";
            assertEquals("Message in exception [" + e.getMessage() + "] doesn't match expected value [" + expected + "].", e.getMessage(), expected);
        }
    }

    public void testIncorrectParametersNoCustomType_ClassName() throws IOException, SAXException {
        InputStream input = new ByteArrayInputStream(readStream(getClass().getResourceAsStream("edi-config-incorrect-no-custom-parameter2.xml")));

        try {
            EDIConfigDigester.digestConfig(input);
            assertTrue("EDIConfigDigester should fail for test configuration.", false);
        } catch (EDIConfigurationException e) {
            String expected = "When first parameter in list of parameters is not a key-value-pair the type of the ValueNode should be Custom.";
            assertEquals("Message in exception [" + e.getMessage() + "] doesn't match expected value [" + expected + "].", e.getMessage(), expected);
        }
    }

    public void testCorrectParametersCustomType() throws IOException, SAXException, EDIConfigurationException {
        InputStream input = new ByteArrayInputStream(readStream(getClass().getResourceAsStream("edi-config-correct-custom-parameter.xml")));

        Edimap edimap = EDIConfigDigester.digestConfig(input);

        Segment segment = (Segment)edimap.getSegments().getSegments().get(0);
        Field field = segment.getFields().get(0);

        assertEquals("Number of parameters in list [" + field.getParameters().size() + "] doesn't match expected value [2].", field.getParameters().size(), 2);

        String expected = "CustomClass";
        String value = field.getParameters().get(0).getValue();
        assertEquals("Value in parameters [" + value + "] doesn't match expected value [" + expected + "].", value, expected);

        expected = "param1";
        value = field.getParameters().get(1).getKey();
        assertEquals("Key in parameters [" + value + "] doesn't match expected value [" + expected + "].", value, expected);

        expected = "value1";
        value = field.getParameters().get(1).getValue();
        assertEquals("Value in parameters [" + value + "] doesn't match expected value [" + expected + "].", value, expected);

    }

    public void testIncorrectParametersCustomType_NoClassName() throws IOException, SAXException {
        InputStream input = new ByteArrayInputStream(readStream(getClass().getResourceAsStream("edi-config-incorrect-custom-parameter.xml")));

        try {
            EDIConfigDigester.digestConfig(input);
            assertTrue("EDIConfigDigester should fail for test configuration.", false);
        } catch (EDIConfigurationException e) {
            String expected = "When using the Custom type in ValueNode the custom class type must exist as the first element in parameters";
            assertEquals("Message in exception [" + e.getMessage() + "] doesn't match expected value [" + expected + "].", e.getMessage(), expected);
        }
    }
}
