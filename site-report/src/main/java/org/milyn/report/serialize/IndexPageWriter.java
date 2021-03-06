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

import java.io.Writer;

/**
 * Writer for the browser list page of the test report.
 * @author tfennelly
 */
public class IndexPageWriter extends AbstractPageWriter {
	
	public IndexPageWriter(Writer writer) {
		super(writer);
		write(getClass().getResourceAsStream("index-header.html"));
	}
	
	/**
	 * Add a link to the named browser PageList index page.
	 * @param name Browser name.
	 */
	public void addBrowser(String name, String friendlyName) {
		write("<div class='pagelink'>");
		write("<a href='" + name + "/" + ReportPageWriterFactory.BROWSER_PAGELIST_INDEXPAGE + "'>");
		write(friendlyName);
		write("</a>");
		write("</div>\r\n");
	}
}
