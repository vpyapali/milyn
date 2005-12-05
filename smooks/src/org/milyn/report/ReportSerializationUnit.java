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

package org.milyn.report;

import java.io.IOException;
import java.io.Writer;

import org.milyn.cdr.CDRDef;
import org.milyn.container.ContainerRequest;
import org.milyn.delivery.serialize.DefaultSerializationUnit;
import org.w3c.dom.CDATASection;
import org.w3c.dom.Comment;
import org.w3c.dom.Element;
import org.w3c.dom.EntityReference;
import org.w3c.dom.Node;
import org.w3c.dom.Text;

/**
 * Report SerializationUnit implementation.
 * <p/>
 * "HTML'ifies" all markup as it serializes.
 * @author tfennelly
 */
public class ReportSerializationUnit extends DefaultSerializationUnit {

	private boolean isEmpty = false;
	
	/**
	 * Public constructor.
	 * @param cdrDef
	 */
	public ReportSerializationUnit(CDRDef cdrDef) {
		super(cdrDef);
		isEmpty = cdrDef.getBoolParameter("isEmpty", false);
	}

	/* (non-Javadoc)
	 * @see org.milyn.serialize.SerializationUnit#writeElementStart(org.w3c.dom.Element, java.io.Writer)
	 */
	public void writeElementStart(Element element, Writer writer, ContainerRequest containerRequest) throws IOException {
		writer.write("&lt;");
		writer.write(element.getTagName());
		writeAttributes(element.getAttributes(), writer);
		if(!isEmpty) {
			writer.write("&gt;");
		}
	}
	
	/* (non-Javadoc)
	 * @see org.milyn.serialize.SerializationUnit#writeElementEnd(org.w3c.dom.Element, java.io.Writer)
	 */
	public void writeElementEnd(Element element, Writer writer, ContainerRequest containerRequest) throws IOException {
		if(!isEmpty) {
			writer.write("&lt;/");
			writer.write(element.getTagName());
			writer.write("&gt;");
		} else {
			writer.write("/&gt;");
		}
	}

	/* (non-Javadoc)
	 * @see org.milyn.serialize.SerializationUnit#writeElementText(org.w3c.dom.Text, java.io.Writer)
	 */
	public void writeElementText(Text text, Writer writer, ContainerRequest containerRequest) throws IOException {
		String string = text.getData();
		StringBuffer stringBuf = new StringBuffer(string.length());
		
		for(int i = 0; i < string.length(); i++) {
			char character = string.charAt(i);
			
			if(character == ' ') {
				stringBuf.append("&nbsp;");
			} else if(character == '\t') {
				stringBuf.append("&nbsp;&nbsp;&nbsp;&nbsp;");
			} else if(character == '\n') {
				stringBuf.append("<br/>");
			} else {
				stringBuf.append(character);
			}
		}
		writer.write(stringBuf.toString());
	}

	/* (non-Javadoc)
	 * @see org.milyn.serialize.SerializationUnit#writeElementComment(org.w3c.dom.Comment, java.io.Writer)
	 */
	public void writeElementComment(Comment comment, Writer writer, ContainerRequest containerRequest) throws IOException {
		writer.write("&lt;!--");
		writer.write(comment.getData());
		writer.write("--&gt;");
	}

	/* (non-Javadoc)
	 * @see org.milyn.serialize.SerializationUnit#writeElementEntityRef(org.w3c.dom.EntityReference, java.io.Writer)
	 */
	public void writeElementEntityRef(EntityReference entityRef, Writer writer, ContainerRequest containerRequest) throws IOException {
		writer.write("&amp;");
		writer.write(entityRef.getNodeName());
		writer.write(';');
	}

	/* (non-Javadoc)
	 * @see org.milyn.serialize.SerializationUnit#writeElementCDATA(org.w3c.dom.CDATASection, java.io.Writer)
	 */
	public void writeElementCDATA(CDATASection cdata, Writer writer, ContainerRequest containerRequest) throws IOException {
		writer.write("&lt:![CDATA[");
		writer.write(cdata.getData());
		writer.write("]]&gt;");
	}

	/* (non-Javadoc)
	 * @see org.milyn.serialize.SerializationUnit#writeElementNode(org.w3c.dom.Node, java.io.Writer)
	 */
	public void writeElementNode(Node node, Writer writer, ContainerRequest containerRequest) throws IOException {
		throw new IOException("writeElementNode not implemented yet. Node: " + node.getNodeValue() + ", node: [" + node + "]");
	}

	private static final String SHORT_DESC = "Report element SerializationUnit implementation.";
	/* (non-Javadoc)
	 * @see org.milyn.ContentDeliveryUnit#getShortDescription()
	 */
	public String getShortDescription() {
		return SHORT_DESC;
	}

	private static final String DETAIL_DESC = "Writes the report content such that the content is viewable on the page i.e. it's HTML'ified.";
	/* (non-Javadoc)
	 * @see org.milyn.ContentDeliveryUnit#getDetailDescription()
	 */
	public String getDetailDescription() {
		return DETAIL_DESC;
	}

	/* (non-Javadoc)
	 * @see org.milyn.serialize.SerializationUnit#writeChildElements()
	 */
	public boolean writeChildElements() {
		return true;
	}
}