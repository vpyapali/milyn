/*
	Milyn - Copyright (C) 2003

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

package org.milyn.css;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

import org.milyn.cdr.CDRDef;
import org.milyn.container.MockContainerRequest;
import org.milyn.container.MockContainerResourceLocator;
import org.milyn.magger.CSSProperty;
import org.milyn.util.SmooksUtil;
import org.milyn.xml.XmlUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import junit.framework.TestCase;

public class CssStyleScraperTest extends TestCase {

	MockContainerRequest request;
	
	/* (non-Javadoc)
	 * @see junit.framework.TestCase#setUp()
	 */
	protected void setUp() throws Exception {
		SmooksUtil smooksUtil = new SmooksUtil();
		request = smooksUtil.getRequest("device");
	}
		
	public void testProcessPageCSS() {
		assertTrue("Expected CSS to be processed - href only.", 
				isCSSProcessed("href='mycss.css'"));
		assertTrue("Expected CSS to be processed - href + type.", 
				isCSSProcessed("href='mycss.css' type='text/css'"));
		assertTrue("Expected CSS to be processed - href + rel.", 
				isCSSProcessed("href='mycss.css' rel='stylesheet'"));
		assertTrue("Expected CSS to be processed - href + rel.", 
				isCSSProcessed("href='mycss.css' rel='xxx stylesheet'"));
		request.uaContext.addProfile("screen");
		assertTrue("Expected CSS to be processed - href + media.", 
				isCSSProcessed("href='mycss.css' media='screen'"));
		
		assertFalse("Expected CSS not to be processed - href + invalid media.", 
				isCSSProcessed("href='mycss.css' media='audio'"));
		assertFalse("Expected CSS not to be processed - href + invalid type.", 
				isCSSProcessed("href='mycss.css' type='xxx'"));
		assertFalse("Expected CSS not to be processed - href + alternate stylesheet rel.", 
				isCSSProcessed("href='mycss.css' rel='alternate stylesheet'"));
	}
	
	public void test_link_href_resolution() {
		String requestUri = "http://www.milyn.org/myapp/aaa/mypage.html";
		
		assertEquals("http://www.milyn.org/xxx/yyy/mycss.css", 
				getResolvedUri("/xxx/yyy/mycss.css", requestUri));

		assertEquals("http://www.milyn.org/myapp/aaa/mycss.css", 
				getResolvedUri("mycss.css", requestUri));

		assertEquals("http://www.milyn.org/myapp/mycss.css", 
				getResolvedUri("../mycss.css", requestUri));

		assertEquals("http://www.milyn.org/mycss.css", 
				getResolvedUri("../../mycss.css", requestUri));

		assertEquals("http://www.milyn.org/../mycss.css", 
				getResolvedUri("../../../mycss.css", requestUri));
	}
	
	public void testInlineStyle() {
		Document doc = CssTestUtil.parseXMLString("<x><style>p {background-color: white}</style><p/></x>"); 
		Element style = (Element)XmlUtil.getNode(doc, "/x/style");
		Element paragraph = (Element)XmlUtil.getNode(doc, "/x/p");
		CDRDef cdrDef = new CDRDef("link", "device", "xxx");
		CssStyleScraper delivUnit = new CssStyleScraper(cdrDef);
		CssMockResLocator mockrl = new CssMockResLocator();
		
		request.requestURI = URI.create("http://www.milyn.org/myapp/aaa/mypage.html");
		request.context.containerResourceLocator = mockrl;
		
		delivUnit.visit(style, request);
		CssAccessor accessor = new CssAccessor(request);
		
		CSSProperty property = accessor.getProperty(paragraph, "background-color");
		assertNotNull("Expected CSS property.", property);
	}
	
	public String getResolvedUri(String href, String requestUri) {
		Document doc = CssTestUtil.parseXMLString("<x><link href='" + href + "' /></x>"); 
		Element link = (Element)XmlUtil.getNode(doc, "/x/link");
		CDRDef cdrDef = new CDRDef("link", "device", "xxx");
		CssStyleScraper delivUnit = new CssStyleScraper(cdrDef);
		CssMockResLocator mockrl = new CssMockResLocator();
		
		request.requestURI = URI.create(requestUri);
		request.context.containerResourceLocator = mockrl;
		
		delivUnit.visit(link, request);
		
		return mockrl.uri;
	}

	
	public boolean isCSSProcessed(String attribs) {
		Document doc = CssTestUtil.parseXMLString("<x><link " + attribs + " /></x>"); 
		Element link = (Element)XmlUtil.getNode(doc, "/x/link");
		CDRDef cdrDef = new CDRDef("link", "device", "xxx");
		CssStyleScraper delivUnit = new CssStyleScraper(cdrDef);
		CssMockResLocator mockrl = new CssMockResLocator();
		
		request.requestURI = URI.create("http://www.milyn.org/myapp/aaa/mypage.html");
		request.context.containerResourceLocator = mockrl;
		
		delivUnit.visit(link, request);
		
		return (mockrl.uri != null);
	}
	
	private class CssMockResLocator extends MockContainerResourceLocator {

		private InputStream stream = CssStyleScraperTest.class.getResourceAsStream("style1.css");
		private String uri;

		/* (non-Javadoc)
		 * @see org.milyn.container.MockContainerResourceLocator#getResource(java.lang.String)
		 */
		public InputStream getResource(String uri) throws IllegalArgumentException, IOException {
			this.uri = uri;
			return stream;
		}
		
	}
}