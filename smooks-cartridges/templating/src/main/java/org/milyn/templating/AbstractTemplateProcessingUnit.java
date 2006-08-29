package org.milyn.templating;

import java.io.IOException;

import javax.xml.transform.TransformerConfigurationException;

import org.milyn.cdr.SmooksResourceConfiguration;
import org.milyn.delivery.process.AbstractProcessingUnit;
import org.milyn.dom.DomUtils;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Abstract template processing unit.
 * <p/>
 * Defines abstract methods for loading the template in question, as well as convienience methods for
 * processing the template action against the templating result (replace, addto, insertbefore and insertafter).
 * <p/>
 * See implementations.
 * @author tfennelly
 */
public abstract class AbstractTemplateProcessingUnit extends AbstractProcessingUnit {

    private static final int REPLACE = 0;
    private static final int ADDTO = 1;
    private static final int INSERT_BEFORE = 2;
    private static final int INSERT_AFTER = 3;
    
    private boolean visitBefore = false;
    private int action = REPLACE;

    public AbstractTemplateProcessingUnit(SmooksResourceConfiguration config) throws IOException, TransformerConfigurationException {
		super(config);
        visitBefore = config.getBoolParameter("visitBefore", false);
        setAction(config);
        loadTemplate(config);
	}
	
	protected abstract void loadTemplate(SmooksResourceConfiguration config) throws IOException, TransformerConfigurationException;
	
    private void setAction(SmooksResourceConfiguration config) {
        String actionParam = config.getStringParameter("action");
        
        if("addto".equals(actionParam)) {
            action = ADDTO;
        } else if("insertbefore".equals(actionParam)) {
            action = INSERT_BEFORE;
        } else if("insertafter".equals(actionParam)) {
            action = INSERT_AFTER;
        } else {
            action = REPLACE;
        }
    }

	protected void processTemplateAction(Element element, Node templatingResult) {
		// REPLACE needs to be handled explicitly...
		if(action == REPLACE) {
            DomUtils.replaceNode(templatingResult, element);
        } else {
    		_processTemplateAction(element, templatingResult, action);
        }
	}

	protected void processTemplateAction(Element element, NodeList templatingResultNodeList) {
		// REPLACE needs to be handled explicitly...
		if(action == REPLACE) {
			processTemplateAction(element, templatingResultNodeList, INSERT_BEFORE);
			element.getParentNode().removeChild(element);
        } else {
			processTemplateAction(element, templatingResultNodeList, action);
        }
	}

	private void processTemplateAction(Element element, NodeList templatingResultNodeList, int action) {
		int count = templatingResultNodeList.getLength();
		
		// Iterate over the NodeList and process each Node against the action. 
		for(int i = 0; i < count; i++) {
			_processTemplateAction(element, templatingResultNodeList.item(i), action);
		}
	}

	private void _processTemplateAction(Element element, Node node, int action) {
		Node parent = element.getParentNode();
        switch (action) {
        case ADDTO:
            element.appendChild(node);
            break;
        case INSERT_BEFORE:
            parent.insertBefore(node, element);
            break;
        case INSERT_AFTER:
            Node nextSibling = element.getNextSibling();
            
            if(nextSibling == null) {
                // "element" is the last child of "parent" so just add to "parent".
                parent.appendChild(node);
            } else {
                // insert before the "nextSibling" - Node doesn't have an "insertAfter" operation!
                parent.insertBefore(node, nextSibling);
            }
            break;
        case REPLACE:
        default:
            // Don't perform any "replace" actions here!
            break;
        }
	}

    public boolean visitBefore() {
        return visitBefore;
    }
}
