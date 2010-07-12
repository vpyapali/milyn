/*
	Milyn - Copyright (C) 2006 - 2010

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
package org.milyn.delivery;

import org.milyn.SmooksException;
import org.milyn.container.ExecutionContext;
import org.milyn.delivery.replay.EndElementEvent;
import org.milyn.delivery.replay.SAXEventReplay;
import org.milyn.delivery.replay.StartElementEvent;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.ext.DefaultHandler2;

import java.util.Stack;

/**
 * Abstract SAX Content Handler.
 * 
 * @author <a href="mailto:tom.fennelly@gmail.com">tom.fennelly@gmail.com</a>
 */
public abstract class SmooksContentHandler extends DefaultHandler2 implements SAXEventReplay {

    private ExecutionContext executionContext;
    private SmooksContentHandler parentContentHandler;
    private SAXEventReplay lastEvent = null;
    private StartElementEvent startEvent = new StartElementEvent();
    private EndElementEvent endEvent = new EndElementEvent();
    private int depth = 0;

    public SmooksContentHandler(ExecutionContext executionContext, SmooksContentHandler parentContentHandler) {
        this.executionContext = executionContext;
        this.parentContentHandler = parentContentHandler;
        attachHandler();
    }

    @Override
    public final void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        if(depth == 0 && parentContentHandler != null) {
            // Replay the last sax event from the parent handler on this sax handler...
            parentContentHandler.replay(this);
        }

        startEvent.set(uri, localName, qName, attributes);
        lastEvent = startEvent;

        depth++;
        startElement(startEvent);
    }

    public abstract void startElement(StartElementEvent startEvent) throws SAXException;

    @Override
    public final void endElement(String uri, String localName, String qName) throws SAXException {
        endEvent.set(uri, localName, qName);
        lastEvent = endEvent;

        endElement(endEvent);
        depth--;

        if(depth == 0 && parentContentHandler != null) {
            // Replay the last sax event from this handler onto the parent handler ...
            this.replay(parentContentHandler);
            // Reinstate the parent handler on the XMLReader so all events are
            // forwarded to it again ...
            XMLReader xmlReader = AbstractParser.getXMLReader(executionContext);
            xmlReader.setContentHandler(parentContentHandler);
        }
    }

    public abstract void endElement(EndElementEvent endEvent) throws SAXException;

    public void replay(org.xml.sax.ContentHandler handler) throws SmooksException {
        if(lastEvent != null) {
            lastEvent.replay(handler);
        }
    }

    private void attachHandler() {
        executionContext.setAttribute(DefaultHandler2.class, this);
    }

    public static SmooksContentHandler getHandler(ExecutionContext executionContext) {
        return (SmooksContentHandler) executionContext.getAttribute(DefaultHandler2.class);
    }

    public void detachHandler() {
        executionContext.removeAttribute(DefaultHandler2.class);
    }
}
