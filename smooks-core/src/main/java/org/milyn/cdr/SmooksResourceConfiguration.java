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

package org.milyn.cdr;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.StringTokenizer;
import java.util.Vector;

import org.milyn.delivery.ContentDeliveryConfig;
import org.milyn.dom.DomUtils;
import org.milyn.io.StreamUtils;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * Smooks Resource Definition.
 * <p/>
 * A <b>Content Deliver Resource</b> is anything that can be used by Smooks in the process of analysing or
 * manipulating/transforming a data stream e.g. a J2EE Servlet Response.  They could be pieces
 * of Java logic ({@link org.milyn.delivery.assemble.AssemblyUnit}, {@link org.milyn.delivery.process.ProcessingUnit}, 
 * {@link org.milyn.delivery.serialize.SerializationUnit}), some text or script resource, or perhaps
 * simply a configuration parameter (see {@link org.milyn.cdr.ParameterAccessor}).  One way Smooks allows 
 * definition of resource configurations is via <b>.cdrl</b> XML files.  An example of such a file is as 
 * follows (with an explanation below):
 * <pre>
 * &lt;?xml version='1.0'?&gt;
 * &lt;!DOCTYPE smooks-resource-list PUBLIC '-//MILYN//DTD SMOOKS 1.0//EN' 'http://milyn.codehaus.org/dtd/smooksres-list-1.0.dtd>
 * &lt;smooks-resource-list&gt;
 * 	&lt;!--	
 * 		Note: 
 * 		1. 	"wml11" is a browser profile.
 * 	--&gt;
 * 	&lt;smooks-resource useragent="wml11" selector="dtd" path="www.wapforum.org/DTD/wml_1_1.dtd" /&gt;
 * 	&lt;smooks-resource useragent="wml11" selector="table" path="{@link org.milyn.delivery.process.ProcessingUnit com.acme.transform.TableWML11}" /&gt;
 * &lt;/smooks-resource-list&gt;</pre>
 * <p/>
 * The .cdrl DTD can be seen at <a href="http://milyn.codehaus.org/dtd/smooksres-list-1.0.dtd">
 * http://milyn.codehaus.org/dtd/smooksres-list-1.0.dtd</a>
 * 
 * <h3 id="attribdefs">Attribute Definitions</h3>
 * <ul>
 * 		<li><b id="useragent">useragent</b>: A list of 1 or more browser/useragent target(s) to which this 
 * 			resource is to be applied.  Each entry ("useragent expression") in this list is seperated
 * 			by a comma.  Useragent expressions are represented by the {@link org.milyn.cdr.UseragentExpression}
 * 			class.  
 * 			<br/> 
 * 			Can be one of:
 * 			<ol>
 * 				<li>A browser "Common Name" as defined in the device recognition configuration (see <a href="http://milyn.org/Tinak">Milyn Tinak</a>).</li>
 * 				<li>A browser profile as defined in the device profiling configuration (see <a href="http://milyn.org/Tinak">Milyn Tinak</a>).</li>
 * 				<li>Astrix ("*") indicating a match for all useragents.  This is the default value if this
 *                  attribute is not specified.</li>
 * 			</ol>
 * 			See <a href="#res-targeting">Resource Targeting</a>.
 * 			<p/>
 * 			<b>AND</b> and <b>NOT</b> expressions are supported on the useragent attribute.
 * 			NOT expressions are specified in the "not:&lt;<i>profile-name</i>&gt;"
 * 			format. AND expressions are supported simply - by seperating the device/profile
 * 			names using "AND".  An example of the use of these expressions
 * 			in one useragent attribute value could be <i>useragent="html4 AND not:xforms"</i> -
 * 			target the resource at browsers/devices that have the "html4" profile but don't
 * 			have the "xforms" profile.
 * 			<p/>
 * 		</li>
 * 		<li><b id="selector">selector</b>: Selector string.  Used by Smooks to "lookup" a .cdrl resource.
 * 			<br/> 
 * 			Example values currently being used are:
 * 			<ol>
 * 				<li><u>Markup element names (e.g. table, tr, pre etc)</u>.  These selector types can be
 *              be contextual in a similar way to contextual selectors in CSS e.g. "td ol li" will target the
 *              resource (e.g. a {@link org.milyn.delivery.process.ProcessingUnit}) at all "li" elements nested
 *              inside an "ol" element, which is in turn nested inside a "td" element.
 * 				</li>
 * 				<li><u>The requesting browser's markup definition i.e. DTD</u>.  Currently Smooks only support
 * 					"Element Content Spec" based selectors, identified by the "xmldef:elcspec:" prefix.  Supported
 * 					values are "xmldef:elcspec:<b>empty</b>", "xmldef:elcspec:<b>not-empty</b>", "xmldef:elcspec:<b>any</b>", 
 * 					"xmldef:elcspec:<b>not-any</b>", "xmldef:elcspec:<b>mixed</b>", "xmldef:elcspec:<b>not-mixed</b>", 
 * 					"xmldef:elcspec:<b>pcdata</b>", "xmldef:elcspec:<b>not-pcdata</b>".
 * 					We hope to be able expand this to support more DTD based selection criteria.  See {@link org.milyn.dtd.DTDStore}.
 * 				</li>
 * 				<li><u>Astrix ("*") indicating a match for all markup elements</u>.  Note this doesn't mean match anything.  It's only
 * 					relevant to, and used by, markup element based selection. I hope this makes sense!
 * 				</li>
 * 				<li><u>Arbitrary strings</u>.  Examples of where selector is currently used in this mode are how Smooks
 * 					<a href="../delivery/doc-files/doctype.cdrl">applies DOCTYPE headers</a> and 
 * 					<a href="../delivery/doc-files/dtds.cdrl">targets DTDs</a>.  Content Delivery Units
 * 				</li>
 * 			</ol>
 * 			The first 3 of these are used by Smooks to select {@link org.milyn.delivery.ContentDeliveryUnit}s.
 * 			<br/>
 * 			See <a href="#res-targeting">Resource Targeting</a>.
 * 			<p/>
 * 		</li>
 * 		<li><b>path</b>: The path to the resource within the classpath or one of the loaded .cdrar files.
 * 			<p/>
 * 		</li>
 * 		<li><b id="namespace">namespace</b>: The XML namespace of the target for this resource.  This is used
 * 			to target {@link org.milyn.delivery.ContentDeliveryUnit}s at XML elements from a
 * 			specific XML namespace e.g. "http://www.w3.org/2002/xforms".  If not defined, the resource
 * 			is targeted at all namespces. 
 * 		</li>
 * </ul>
 * All of the &lt;smooks-resource&gt; attributes can be defaulted on the enclosing &lt;smooks-resource-list&gt; element.
 * Just prefix the attribute name with "default-".  Example:
 * <pre>
 * &lt;?xml version='1.0'?&gt;
 * &lt;!DOCTYPE smooks-resource-list PUBLIC '-//MILYN//DTD SMOOKS 1.0//EN' 'http://milyn.codehaus.org/dtd/smooksres-list-1.0.dtd>
 * &lt;smooks-resource-list default-useragent="value" default-selector="value" default-namespace="http://www.w3.org/2002/xforms"&gt;
 * 	&lt;smooks-resource path="value"/&gt;
 * &lt;/smooks-resource-list&gt;</pre>
 * 
 * <h3 id="res-targeting">Resource Targeting</h3>
 * Content Delivery Resources ({@link org.milyn.delivery.process.ProcessingUnit} etc) are targeted
 * using a combination of the <a href="#useragent">useragent</a>, <a href="#selector">selector</a> 
 * and <a href="#namespace">namespace</a> attributes (see above).
 * <p/>
 * Smooks does this at runtime by building (and caching) a table of resources per useragent type (e.g. requesting browser).
 * For example, when the {@link org.milyn.SmooksServletFilter} receives a request, it 
 * <ol>
 * 	<li>
 * 		Uses the device recognition and profiling information provided by
 * 		<a href="http://milyn.org/Tinak">Milyn Tinak</a> to iterate over the .cdrl configurations and select the definitions that apply to that browser type.
 * 		It evaluates this based on the <a href="#useragent">useragent</a> attribute value.  Once the table 
 * 		is built it is cached so it doesn't need to be rebuilt for future requests from this browser type. 
 * 	</li>
 * 	<li>
 * 		Smooks can then "lookup" resources based on the <a href="#selector">selector</a> attribute value.
 * 	</li>
 * </ol>
 * As you'll probably notice, the types of configurations that the .cdrl file permits can/will result in  
 * multiple resources being mapped to a browser under the same "selector" value i.e. if you request the resource
 * by selector "x", there may be 1+ matches.  Because of this Smooks sorts these matches based on what we call
 * the definitions "specificity".  
 * See {@link org.milyn.cdr.SmooksResourceConfigurationSortComparator}.
 * 
 * <h3>&lt;param&gt; Elements</h3>
 * As can be seen from the <a href="http://milyn.codehaus.org/dtd/smooksres-list-1.0.dtd">DTD</a>, the &lt;smooks-resource&gt; element can 
 * also define zero or more &lt;param&gt; elements. These elements allow runtime parameters to be passed to content delivery units.
 * This element defines a single mandatory attribute called "<b>name</b>".  The parameter value is inclosed in the
 * param element e.g.
 * <pre>
 * &lt;?xml version='1.0'?&gt;
 * &lt;!DOCTYPE smooks-resource-list PUBLIC '-//MILYN//DTD SMOOKS 1.0//EN' 'http://milyn.codehaus.org/dtd/smooksres-list-1.0.dtd>
 * &lt;smooks-resource-list default-useragent="value" default-selector="value" &gt;
 * 	&lt;smooks-resource path="value"&gt;
 * 		&lt;param name="paramname"&gt;paramval&lt;/param&gt;
 * 	&lt;/smooks-resource&gt;
 * &lt;/smooks-resource-list&gt;</pre>
 * <p/>
 * Complex parameter values can be defined and decoded via configured 
 * {@link org.milyn.cdr.ParameterDecoder}s and the 
 * {@link #getParameter(String)}.{@link Parameter#getValue(ContentDeliveryConfig) getValue(ContentDeliveryConfig)} 
 * method (see {@link org.milyn.cdr.TokenizedStringParameterDecoder} as an example).
 *   
 * @see SmooksResourceConfigurationSortComparator 
 * @author tfennelly
 */
public class SmooksResourceConfiguration {
	/**
	 * Document target on which the resource is to be applied.
	 */
	private String selector;
    /**
     * Element based selectors can be contextual ala CSS contextual selectors.
     * The are of the CSS contextual selector form i.e. "UL UL LI".  This String
     * array contains a parsed contextual selector.
     */
    private String[] contextualSelector;
	/**
	 * List of device/profile names on which the Content Delivery Resource is to be applied 
	 * for instances of selector.
	 */
	private String[] useragents;
	/**
	 * Useragent expresssions built from the useragents list.
	 */
	private UseragentExpression[] useragentExpressions;
	/**
	 * The path to the Content Delivery Resource within the cdrar.
	 */
	private String path;
	/**
	 * XML selector type definition prefix
	 */
	public static final String XML_DEF_PREFIX = "xmldef:".toLowerCase();
	/**
	 * Is this selector defininition an XML based definition.
	 */
	private boolean isXmlDef;
	/**
	 * SmooksResourceConfiguration parameters - String name and String value.
	 */
	private HashMap parameters;
	private int parameterCount;
	/**
	 * The XML namespace of the tag to which this config 
	 * should only be applied.
	 */
	private String namespaceURI;

    
    /**
     * Public constructor.
     * @param selector The selector definition.
     * @param path The cdrar path of the Content Delivery Resource.
     */
    public SmooksResourceConfiguration(String selector, String path) {
        this(selector, "*", path);
    }
    
	/**
	 * Public constructor.
	 * @param selector The selector definition.
	 * @param useragents The device/profile useragents - comma separated useragents.
	 * @param path The cdrar path of the Content Delivery Resource.
	 */
	public SmooksResourceConfiguration(String selector, String useragents, String path) {
        if(selector == null || selector.trim().equals("")) {
            throw new IllegalArgumentException("null or empty 'selector' arg in constructor call.");
        }
		if(useragents == null || useragents.trim().equals("")) {
            // Default the useragent to everything if not specified.
            useragents = "*";
		}
        this.selector = selector.toLowerCase();
        isXmlDef = selector.startsWith(XML_DEF_PREFIX);
        this.path = path;
		
        // Parse the selector in case it's a contextual selector of the CSS
        // form e.g. "TD UL LI"
        contextualSelector = this.selector.split(" +");
		parseUseragentExpressions(useragents);
	}
	
	/**
	 * Public constructor.
	 * @param selector The selector definition.
	 * @param namespaceURI The XML namespace URI of the element to which this config
	 * applies.
	 * @param useragents The device/profile useragents - comma separated useragents.
	 * @param path The cdrar path of the Content Delivery Resource.
	 */
	public SmooksResourceConfiguration(String selector, String namespaceURI, String useragents, String path) {
		this(selector, useragents, path);
		if(namespaceURI != null) {
            if(namespaceURI.equals("*")) {
                this.namespaceURI = null;
            } else {
                this.namespaceURI = namespaceURI.intern();
            }
		}
	}

	/**
     * Parse the useragent expressions for this configuration.
     * @param useragents The useragent expression from the resource configuration.
	 */
    private void parseUseragentExpressions(String useragents) {
        // Parse the device/profile useragents.  Seperation tokens: ',' '|' and ';'
        StringTokenizer tokenizer = new StringTokenizer(useragents.toLowerCase(), ",|;");
        if(tokenizer.countTokens() == 0) {
            throw new IllegalArgumentException("Empty device/profile useragents. [" + selector + "][" + path + "]");
        } else {
            this.useragents = new String[tokenizer.countTokens()];          
            useragentExpressions = new UseragentExpression[tokenizer.countTokens()];            
            for(int i = 0; tokenizer.hasMoreTokens(); i++) {
                String expression = tokenizer.nextToken();
                this.useragents[i] = expression;
                useragentExpressions[i] = new UseragentExpression(expression);
            }
        }
    }
    
	/**
	 * Get the selector definition for this SmooksResourceConfiguration.
	 * @return The selector definition.
	 */
	public String getSelector() {
		return selector;
	}

	/**
	 * The the XML namespace URI of the element to which this configuration
	 * applies.
	 * @return The XML namespace URI of the element to which this configuration
	 * applies, or null if not namespaced.
	 */
	public String getNamespaceURI() {
		return namespaceURI;
	}

	/**
	 * Get the device/profile useragents for this SmooksResourceConfiguration.
	 * @return The device/profile useragents.
	 */
	public UseragentExpression[] getUseragentExpressions() {
		return useragentExpressions;
	}
	
	/**
	 * Get the cdrar path of the Content Delivery Resource for this SmooksResourceConfiguration.
	 * @return The cdrar path.
	 */
	public String getPath() {
		return path;
	}

	/**
	 * Set the named SmooksResourceConfiguration parameter value (default type - String).
	 * <p/>
	 * Overwrites previous value of the same name.
	 * @param name Parameter name.
	 * @param value Parameter value.
	 */
	public void setParameter(String name, String value) {
		setParameter(new Parameter(name, value));
	}

	/**
	 * Set the named SmooksResourceConfiguration parameter value (with type).
	 * <p/>
	 * Overwrites previous value of the same name.
	 * @param name Parameter name.
	 * @param type Parameter type.
	 * @param value Parameter value.
	 */
	public void setParameter(String name, String type, String value) {
		setParameter(new Parameter(name, value, type));
	}

	public void setParameter(Parameter parameter) {
		if(parameters == null) {
			parameters = new LinkedHashMap();
		}
		Object exists = parameters.get(parameter.getName());
		
		if(exists == null) {
			parameters.put(parameter.getName(), parameter);
		} else if(exists instanceof Parameter) {
			Vector paramList = new Vector();			
			paramList.add(exists);
			paramList.add(parameter);
			parameters.put(parameter.getName(), paramList);
		} else if(exists instanceof List) {
			((List)exists).add(parameter);
		}
		parameterCount++;
	}

	/**
	 * Get the named SmooksResourceConfiguration {@link Parameter parameter}.
	 * <p/>
	 * If there is more than one of the named parameters defined, the first
	 * defined value is returned.  
	 * @param name Name of parameter to get. 
	 * @return Parameter value, or null if not set.
	 */
	public Parameter getParameter(String name) {
		if(parameters == null) {
			return null;
		}
		Object parameter = parameters.get(name);
		
		if(parameter instanceof List) {
			return (Parameter)((List)parameter).get(0);
		} else if(parameter instanceof Parameter) {
			return (Parameter)parameter;
		}
		
		return null;
	}

	/**
	 * Get the named SmooksResourceConfiguration {@link Parameter parameter} List.
	 * @param name Name of parameter to get. 
	 * @return {@link Parameter} value {@link List}, or null if not set.
	 */
	public List getParameters(String name) {
		if(parameters == null) {
			return null;
		}
		Object parameter = parameters.get(name);
		
		if(parameter instanceof List) {
			return (List)parameter;
		} else if(parameter instanceof Parameter) {
			Vector paramList = new Vector();			
			paramList.add(parameter);
			parameters.put(name, paramList);
			return paramList;
		}
		
		return null;
	}

	/**
	 * Get the named SmooksResourceConfiguration parameter.
	 * @param name Name of parameter to get. 
	 * @return Parameter value, or null if not set.
	 */
	public String getStringParameter(String name) {
		Parameter parameter = getParameter(name);
        
		return (parameter != null?parameter.value:null);
	}

	/**
	 * Get the named SmooksResourceConfiguration parameter.
	 * @param name Name of parameter to get. 
	 * @param defaultVal The default value to be returned if there are no 
	 * parameters on the this SmooksResourceConfiguration instance, or the parameter is not defined.
	 * @return Parameter value, or defaultVal if not defined.
	 */
	public String getStringParameter(String name, String defaultVal) {
        Parameter parameter = getParameter(name);
        
		return (parameter != null?parameter.value:defaultVal);
	}

	/**
	 * Get the named SmooksResourceConfiguration parameter as a boolean.
	 * @param name Name of parameter to get. 
	 * @param defaultVal The default value to be returned if there are no 
	 * parameters on the this SmooksResourceConfiguration instance, or the parameter is not defined.
	 * @return true if the parameter is set to true, defaultVal if not defined, otherwise false.
	 */
	public boolean getBoolParameter(String name, boolean defaultVal) {
		String paramVal;

		if(parameters == null) {
			return defaultVal;
		}
		
		paramVal = getStringParameter(name);
		if(paramVal == null) {
			return defaultVal;
		}
		paramVal = paramVal.trim();
		if(paramVal.equals("true")) {
			return true;
		} else if(paramVal.equals("false")) {
			return false;
		} else {
			return defaultVal;
		}
	}

	/**
	 * Get the SmooksResourceConfiguration parameter count.
	 * @return Number of parameters defined on this SmooksResourceConfiguration.
	 */
	public int getParameterCount() {
		return parameterCount;
	}
    
    /**
     * Remove the named parameter.
     * @param name The name of the parameter to be removed.
     */
    public void removeParameter(String name) {
        parameters.remove(name);
    }

	/**
	 * Is this selector defininition an XML based definition.
	 * <p/>
	 * I.e. is the selector attribute value prefixed with "xmldef:".
	 * @return True if this selector defininition is an XML based definition, otherwise false.
	 */
	public boolean isXmlDef() {
		return isXmlDef;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return "[" + Arrays.asList(useragents) +"][" + selector + "][" + path + "]";
	}
    
    /**
     * Get the resource as a byte array.
     * @return The resource as a byte array, or null if resource path
     * is null or the resource doesn't exist.
     * @throws IOException Failed to read the resource bytes.
     */
    public byte[] getBytes() throws IOException {
        byte[] bytes = null;
        
        if(path != null) {
            InputStream resStream = getClasspathResourceStream(path);
            
            if(resStream == null) {
                String filePath = ClasspathUtils.toFileName(path);
    
                resStream = getClasspathResourceStream(filePath);
                if(resStream == null) {
                    throw new IOException("Resource [" + path + "] not found in classpath.");
                }
            }
            
            bytes = StreamUtils.readStream(resStream);
        }
        
        return bytes;
    }

    private InputStream getClasspathResourceStream(String filePath) {
        if(!filePath.startsWith("/")) {
            // Make the path absolute if is already isn't.
            filePath = "/" + filePath;
        }            
        return getClass().getResourceAsStream(filePath);
    }
    
    /**
     * Is this resource a Java class resource.
     * @return True if this resource is a Java class resource, otherwise false.
     */
    public boolean isJavaResource() {
        String className;
        
        if(path == null) {
            return false;
        }
        
        className = ClasspathUtils.toClassName(path);
        try {
            Class.forName(className);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    /**
     * Is this resource configuration targets at the same namespace as the
     * specified elemnt.
     * @param element The element to check against.
     * @return True if this resource config is targeted at the element namespace,
     * or if the resource is not targeted at any namespace (i.e. not specified), 
     * otherwise false.
     */
    public boolean isTargetedAtElementNamespace(Element element) {
        // Check the namespace (if specified) of the config against the 
        // supplied element namespace.
        if(namespaceURI != null && !namespaceURI.equals(element.getNamespaceURI())) {
            return false;
        }
        
        return true;
    }

    /**
     * Is the resource selector contextual.
     * <p/>
     * See details about the "selector" attribute in the 
     * <a href="#attribdefs">Attribute Definitions</a> section.
     * @return True if the selector is contextual, otherwise false.
     */
    public boolean isSelectorContextual() {
        return (contextualSelector.length > 1);
    }
    
    /**
     * Is this resource configuration targeted at the specified element
     * in context.
     * <p/>
     * See details about the "selector" attribute in the 
     * <a href="#attribdefs">Attribute Definitions</a> section.
     * <p/>
     * Note this doesn't perform any namespace checking.
     * @param element The element to check against.
     * @return True if this resource configuration is targeted at the specified
     * element in context, otherwise false.
     */
    public boolean isTargetedAtElementContext(Element element) {
        Node currentNode = element;
        
        // Check the element name(s).
        for(int i = contextualSelector.length - 1; i >= 0; i--) {
            if(currentNode.getNodeType() != Node.ELEMENT_NODE) {
                return false;
            }
            
            Element currentElement = (Element)currentNode;
            String elementName = DomUtils.getName(currentElement);
            
            if(contextualSelector[i].equals("*")) {
                // match
            } else if(!contextualSelector[i].equalsIgnoreCase(elementName)) {
                return false;
            }
            
            // Go the next parent node...
            if(i > 0) {
                currentNode = currentNode.getParentNode();
            }
        }
        
        return true;
    }
}