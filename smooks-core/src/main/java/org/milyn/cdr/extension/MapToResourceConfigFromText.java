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
package org.milyn.cdr.extension;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.milyn.SmooksException;
import org.milyn.xml.DomUtils;
import org.milyn.cdr.SmooksResourceConfiguration;
import org.milyn.container.ExecutionContext;
import org.milyn.delivery.dom.DOMVisitBefore;
import org.w3c.dom.Element;

import java.util.EmptyStackException;

/**
 * Map a property value onto the current {@link org.milyn.cdr.SmooksResourceConfiguration} based on an
 * elements text content.
 * <p/>
 * The value is set on the {@link org.milyn.cdr.SmooksResourceConfiguration} returned from the top
 * of the {@link ExtensionContext#getResourceStack() ExtensionContext resourece stack}.
 *
 * @author <a href="mailto:tom.fennelly@gmail.com">tom.fennelly@gmail.com</a>
 */
public class MapToResourceConfigFromText extends MapToResourceConfig implements DOMVisitBefore {

    private static Log logger = LogFactory.getLog(MapToResourceConfigFromText.class);

    public void visitBefore(Element element, ExecutionContext executionContext) throws SmooksException {
        SmooksResourceConfiguration config;
        String value = DomUtils.getAllText(element, false);

        try {
            config = ExtensionContext.getExtensionContext(executionContext).getResourceStack().peek();
        } catch (EmptyStackException e) {
            throw new SmooksException("No SmooksResourceConfiguration available in ExtensionContext stack.  Unable to set SmooksResourceConfiguration property '" + getMapTo() + "' with element text value.");
        }

        if (value == null) {
            logger.debug("Not setting property '" + getMapTo() + "' on resource configuration.  Element '" + DomUtils.getName(element) + "' text value is null.");
            return;
        } else {
            logger.debug("Setting property '" + getMapTo() + "' on resource configuration to a value of '" + value + "'.");
        }

        setProperty(config, value, executionContext);
    }
}