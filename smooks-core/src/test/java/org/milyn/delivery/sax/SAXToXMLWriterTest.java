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
package org.milyn.delivery.sax;

import java.io.IOException;

import org.milyn.Smooks;
import org.milyn.SmooksException;
import org.milyn.container.ExecutionContext;
import org.milyn.delivery.sax.annotation.StreamResultWriter;
import org.milyn.payload.StringResult;
import org.milyn.payload.StringSource;

import junit.framework.TestCase;

/**
 * 
 * @author <a href="mailto:tom.fennelly@gmail.com">tom.fennelly@gmail.com</a>
 */
public class SAXToXMLWriterTest extends TestCase {

	public void test() {
		Smooks smooks = new Smooks();
		StringResult stringResult = new StringResult();
		
		smooks.addVisitor(new MyWrittingVisitor().setLeftWrapping("{{").setRightWrapping("}}"), "a");
		smooks.addVisitor(new MyWrittingVisitor().setLeftWrapping("((").setRightWrapping("))"), "b");
		smooks.filterSource(new StringSource("<a><b>sometext</b></a>"), stringResult);
		
		assertEquals("{{<a>((<b>sometext</b>))</a>}}", stringResult.getResult());
	}	
	
	@StreamResultWriter	
	private class MyWrittingVisitor implements SAXElementVisitor {

		private SAXToXMLWriter writer = new SAXToXMLWriter(this, true);
		private String leftWrapping;
		private String rightWrapping;
		
		public MyWrittingVisitor setLeftWrapping(String leftWrapping) {
			this.leftWrapping = leftWrapping;
			return this;
		}
		
		public MyWrittingVisitor setRightWrapping(String rightWrapping) {
			this.rightWrapping = rightWrapping;
			return this;
		}
		
		public void visitBefore(SAXElement element, ExecutionContext executionContext) throws SmooksException, IOException {
			writer.writeText(leftWrapping, element);
			writer.writeStartElement(element);
		}

		public void onChildElement(SAXElement element, SAXElement childElement, ExecutionContext executionContext) throws SmooksException, IOException {
		}

		public void onChildText(SAXElement element, SAXText childText, ExecutionContext executionContext) throws SmooksException, IOException {
			writer.writeText(childText, element);
		}		
		
		public void visitAfter(SAXElement element, ExecutionContext executionContext) throws SmooksException, IOException {
			writer.writeEndElement(element);
			writer.writeText(rightWrapping, element);
		}
	}
}
