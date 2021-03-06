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

package org.milyn.report.serialize;

import java.io.IOException;
import java.io.Writer;

import org.milyn.cdr.CDRDef;
import org.milyn.container.ContainerRequest;
import org.milyn.delivery.serialize.DefaultSerializationUnit;
import org.w3c.dom.Element;

/**
 * nodereport element serialization unit.
 * @author tfennelly
 */
public class NodeReportSerializationUnit extends DefaultSerializationUnit {

	public NodeReportSerializationUnit(CDRDef unitDef) {
		super(unitDef);
	}

	public void writeElementStart(Element element, Writer writer, ContainerRequest containerRequest) throws IOException {
		String title;
		String nextId;
		String id;
		
		title = element.getAttribute("img-title");
		element.removeAttribute("img-title");
		id = element.getAttribute("id");
		element.removeAttribute("id");
		nextId = element.getAttribute("next-id");
		element.removeAttribute("next-id");
		
		writer.write("<span id='");
		writer.write(id);
		writer.write("' />");
		writer.write("<a");
		writeAttributes(element.getAttributes(), writer);
		writer.write((int)'>');
		writer.write("<img src='../warn.gif' title='");
		writer.write(title);
		writer.write("' border='0'/>");
		writer.write("</a>");
		
		// Add a "Next" link if there's another report on this page.
		if(nextId != null) {
			PageReportWriter.writeNextReportItemHref(writer, nextId);
		}
	}

	public void writeElementEnd(Element element, Writer writer, ContainerRequest containerRequest) throws IOException {
	}
}
