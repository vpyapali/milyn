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

package org.milyn;

import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.LinkedHashMap;

import org.milyn.cdr.SmooksResourceConfiguration;
import org.milyn.container.standalone.StandaloneContainerContext;
import org.milyn.container.standalone.StandaloneContainerRequest;
import org.milyn.container.standalone.StandaloneContainerSession;
import org.milyn.delivery.SmooksXML;
import org.milyn.device.ident.UnknownDeviceException;
import org.milyn.device.profile.BasicProfile;
import org.milyn.device.profile.DefaultProfileSet;
import org.milyn.device.profile.DefaultProfileStore;
import org.milyn.device.profile.ProfileSet;
import org.milyn.device.profile.ProfileStore;
import org.milyn.resource.ClasspathResourceLocator;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import sun.io.CharToByteConverter;

/**
 * Smooks standalone execution class.
 * <p/>
 * Allows {@link org.milyn.delivery.SmooksXML} to be executed in a "non-container" type environemt e.g.
 * from the commandline.  See {@link org.milyn.report.SmooksReportGenerator} as an example of a class
 * using SmooksStandalone.
 * <p/>
 * SmooksStandalone is manually configured by registering useragents 
 * ({@link #registerUseragent(String, String[])}) and resources 
 * ({@link #registerResource(SmooksResourceConfiguration)}, {@link #registerResources(String, InputStream)}).
 * @author tfennelly
 */
public class SmooksStandalone {

	private StandaloneContainerContext context;
	private StandaloneContainerRequest request;
	private String contentEncoding;
	
	/**
	 * Public Constructor.
	 * <p/>
	 * Allows a SmooksStandalone instance to be created and configured from
	 * code i.e. not configured from config files etc.
	 * @param contentEncoding Character encoding to be used when parsing content.  Null 
	 * defaults to "ISO-8859-1".
	 * @see #registerUseragent(String, String[])
	 * @see #registerResource(SmooksResourceConfiguration)
	 */
	public SmooksStandalone(String contentEncoding) {
		setEncoding(contentEncoding);
		context = new StandaloneContainerContext(new DefaultProfileStore(), new ClasspathResourceLocator());
	}

	/**
	 * Set the content encoding to be used when parsing content on this standalone instance. 
	 * @param contentEncoding Character encoding to be used when parsing content.  Null 
	 * defaults to "ISO-8859-1".
	 * @throws IllegalArgumentException Invalid encoding.
	 */
	private void setEncoding(String contentEncoding) throws IllegalArgumentException {
		contentEncoding = (contentEncoding == null)?"ISO-8859-1":contentEncoding;
		try {
			CharToByteConverter.getConverter(contentEncoding);
		} catch (UnsupportedEncodingException e) {
			IllegalArgumentException argE = new IllegalArgumentException("Invalid 'contentEncoding' arg [" + contentEncoding + "].  This encoding is not supported.");
			argE.initCause(e);
			throw argE;
		}
		this.contentEncoding = contentEncoding;
	}
	
	/**
	 * Process the content at the specified URI for the specified useragent.
	 * <p/>
	 * Calls {@link #process(URI, InputStream)} after opening an {@link InputStream}
	 * to the specified {@link URI}.
	 * @param useragent The useragent on behalf of whom the filtering
	 * is to be executed.
	 * @param requestURI URI of the content to be processed.
	 * @return The Smooks processed content DOM {@link Node}.
	 * @throws IOException Is a:<br/>
	 * - {@link MalformedURLException} If a protocol handler for the URL could not be found when
	 * constructing a {@link URL} instance from the supplied {@link URI}, or if 
	 * some other error occurred while constructing the URL.<br/>
	 * - {@link IOException} If unable to read content.
	 * @throws SmooksException Excepting processing content stream.
	 */
	public Node filter(String useragent, URI requestURI) throws SmooksException {
		try {
			return filter(useragent, requestURI, context.getResourceLocator().getResource(requestURI.toString()));
		} catch (IOException e) {
			throw new SmooksException("Error opening/reading stream at URI: " + requestURI, e);
		}
	}

	/**
	 * Process the content at the specified {@link InputStream} for the specified useragent.
	 * <p/>
	 * So this version of the process method doesn't actually open a stream to the
	 * specified URI.  It uses the supplied stream.  The URI is supplied simply to
	 * namespace the stream and satisfy dependencies on the
	 * {@link StandaloneContainerRequest#getRequestURI()} method.
	 * <p/>
	 * The content of the buffer returned is totally dependent on the configured
	 * {@link org.milyn.delivery.process.ProcessingUnit} and {@link org.milyn.delivery.serialize.SerializationUnit}
	 * implementations. 
     * @param useragent The useragent on behalf of whom the filtering
     * is to be executed.
	 * @param stream Stream to be processed.  Will be closed before returning.
	 * @return The Smooks processed content DOM {@link Node}.
	 * @throws SmooksException Excepting processing content stream.
	 */
	public Node filter(String useragent, InputStream stream) throws SmooksException {
		return filter(useragent, null, stream);
	}

	/**
	 * Process the content at the specified {@link InputStream} for the specified useragent.
	 * <p/>
	 * The difference between this method and {@link #process(URI)} is simply that this implementation
	 * uses the supplied stream rather than attempting to open another stream from the requestURI
	 * parameter.
	 * <p/>
	 * The content of the buffer returned is totally dependent on the configured
	 * {@link org.milyn.delivery.process.ProcessingUnit} and {@link org.milyn.delivery.serialize.SerializationUnit}
	 * implementations.
	 * @param useragent The useragent on behalf of whom the transformation
	 * process is to be executed.
	 * @param requestURI The URI context of the content to be processed.  This arg needs to
	 * be supplied if any of the {@link org.milyn.delivery.ElementVisitor} implemenmtations
	 * use call {@link org.milyn.container.ContainerRequest#getRequestURI()}.
	 * @param stream Stream to be processed.  Will be closed before returning.
	 * @return The Smooks processed content DOM {@link Node}.
	 * @throws SmooksException Excepting processing content stream.
	 */
	public Node filter(String useragent, URI requestURI, InputStream stream) throws SmooksException {
		if(stream == null) {
			throw new IllegalArgumentException("null 'stream' arg in method call.");
		}
		StandaloneContainerSession session = context.getSession(useragent);
		Node node;
		SmooksXML smooks;
		
		request = new StandaloneContainerRequest(requestURI, new LinkedHashMap(), session);
		smooks = new SmooksXML(request);
		if(contentEncoding == null) {
			node = smooks.filter(new InputStreamReader(stream));
		} else {
			try {
				node = smooks.filter(new InputStreamReader(stream, contentEncoding));
			} catch (UnsupportedEncodingException e) {
				Error error = new Error("Unexpected exception.  Encoding has already been validated as being supported.");
				error.initCause(e);
				throw error;
			}
		}
		
		return node;
	}

	/**
	 * Process supplied {@link Document} for the specified useragent.
	 * <p/>
	 * The content of the buffer returned is totally dependent on the configured
	 * {@link org.milyn.delivery.process.ProcessingUnit} and {@link org.milyn.delivery.serialize.SerializationUnit}
	 * implementations.
     * @param useragent The useragent on behalf of whom the filtering
     * is to be executed.
	 * @param document Document to be processed.
	 * @return The Smooks processed content DOM {@link Node}.
	 * @throws SmooksException Excepting processing the document.
	 */
	public Node filter(String useragent, Document document) throws SmooksException {
		return filter(useragent, null, document);
	}

	/**
	 * Process supplied {@link Document} for the specified useragent.
	 * <p/>
	 * The content of the buffer returned is totally dependent on the configured
	 * {@link org.milyn.delivery.process.ProcessingUnit} and {@link org.milyn.delivery.serialize.SerializationUnit}
	 * implementations.
	 * @param useragent The useragent on behalf of whom the transformation
	 * process is to be executed.
	 * @param requestURI The URI context of the content to be processed.  This arg needs to
	 * be supplied if any of the {@link org.milyn.delivery.ElementVisitor} implemenmtations
	 * use call {@link org.milyn.container.ContainerRequest#getRequestURI()}.
	 * @param document Document to be processed.
	 * @return The Smooks processed content DOM {@link Node}.
	 * @throws SmooksException Excepting processing the document.
	 */
	public Node filter(String useragent, URI requestURI, Document document) throws SmooksException {
		if(document == null) {
			throw new IllegalArgumentException("null 'document' arg in method call.");
		}
		StandaloneContainerSession session = context.getSession(useragent);
		SmooksXML smooks;
		
		request = new StandaloneContainerRequest(requestURI, new LinkedHashMap(), session);
		smooks = new SmooksXML(request);

		return smooks.filter(document);
	}
	
	/**
	 * Process the content at the specified {@link InputStream} for the specified useragent
	 * and serialise into a String buffer.  See {@link #process(URI, InputStream)}.
	 * <p/>
	 * The content of the buffer returned is totally dependent on the configured
	 * {@link org.milyn.delivery.process.ProcessingUnit} and {@link org.milyn.delivery.serialize.SerializationUnit}
	 * implementations. 
	 * @param useragent The useragent on behalf of whom the transformation
	 * process is to be executed.
	 * @param stream Stream to be processed.  Will be closed before returning.
	 * @return The Smooks processed content buffer.
	 * @throws IOException Exception using or closing the supplied InputStream.
	 * @throws SmooksException Excepting processing content stream.
	 */
	public String filterAndSerialize(String useragent, InputStream stream) throws SmooksException {
		return filterAndSerialize(useragent, null, stream);
	}

	/**
	 * Process the content at the specified {@link InputStream} for the specified useragent
	 * and serialise into a String buffer.  See {@link #process(URI, InputStream)}.
	 * <p/>
	 * The content of the buffer returned is totally dependent on the configured
	 * {@link org.milyn.delivery.process.ProcessingUnit} and {@link org.milyn.delivery.serialize.SerializationUnit}
	 * implementations. 
	 * @param useragent The useragent on behalf of whom the transformation
	 * process is to be executed.
	 * @param requestURI The request URI to be associated with the transformation.  This
	 * is effectively the namespace of the content being transformed.
	 * @param stream Stream to be processed.  Will be closed before returning.
	 * @return The Smooks processed content buffer.
	 * @throws IOException Exception using or closing the supplied InputStream.
	 * @throws SmooksException Excepting processing content stream.
	 */
	public String filterAndSerialize(String useragent, URI requestURI, InputStream stream) throws SmooksException {
		String responseBuf = null;
		CharArrayWriter writer = new CharArrayWriter();
		try {
			Node node;

			node = filter(useragent, requestURI, stream);
			serialize(useragent, node, writer);
			responseBuf = writer.toString();
		} finally {
			if(stream != null) {
				try {
					stream.close();
				} catch (IOException e) {
					new SmooksException("Failed to close stream...", e);
				}
			}
			writer.close();
		}
		
		return responseBuf;
	}

	/**
	 * Serialise the supplied node based on the specified useragents serialisation
	 * configuration.
	 * @param useragent The useragent on behalf of whom the serialisation
	 * process is to be executed.
	 * @param node Node to be serialised.
	 * @param writer Serialisation output writer.
	 * @throws IOException Unable to write to output writer.
	 * @throws SmooksException Unable to serialise due to bad Smooks environment.  Check cause.
	 */
	public void serialize(String useragent, Node node, Writer writer) throws SmooksException {
		SmooksXML smooks;
		StandaloneContainerRequest serRequest = getLastRequest();
		
		if(node == null) {
			throw new IllegalArgumentException("null 'node' arg in method call.");
		}
		if(writer == null) {
			throw new IllegalArgumentException("null 'writer' arg in method call.");
		}
		
		if(serRequest == null) {
			StandaloneContainerSession session = context.getSession(useragent);
			serRequest = new StandaloneContainerRequest(null, new LinkedHashMap(), session);
		}
		smooks = new SmooksXML(request);
		try {
			smooks.serialize(node, writer);
		} catch (IOException e) {
			throw new SmooksException("Serialisation failed...", e);
		}
	}

	/**
	 * Get the current {@link StandaloneContainerSession} associated with
	 * specified useragent.
	 * @param useragent The useragent whose session instance is required.
	 * @return Session instance.
	 */
	public StandaloneContainerSession getSession(String useragent) {
		return context.getSession(useragent);
	}

	/**
	 * Get the last request {@link #processAndSerialize(URI, InputStream) processed} on this
	 * instance.
	 * @return The last request {@link #processAndSerialize(URI, InputStream) processed} on this
	 * instance, or null if none have yet been processed.
	 */
	public StandaloneContainerRequest getLastRequest() {
		return request;
	}

	/**
	 * Manually register the specified useragent.
	 * @param useragent The useragent name.
	 * @param profiles List of profiles with which the useragent is to be associated.
	 */
	public void registerUseragent(String useragent, String[] profiles) {
		if(useragent == null) {
			throw new IllegalArgumentException("null 'useragent' arg in method call.");
		}
		
		ProfileStore profileStore = context.getProfileStore();
		try {
			profileStore.getProfileSet(useragent);
		} catch(UnknownDeviceException e) {
			profileStore.addProfileSet(useragent, new DefaultProfileSet());
		}
		
		// now register the profiles...
		registerProfiles(useragent, profiles);
	}

	/**
	 * Manually register a set of profiles for the specified useragent.
	 * @param useragent The useragent name.
	 * @param profiles List of profiles with which the useragent is to be associated.
	 */
	private void registerProfiles(String useragent, String[] profiles) {
		if(useragent == null) {
			throw new IllegalArgumentException("null 'useragent' arg in method call.");
		}
		if(profiles == null || profiles.length == 0) {
			throw new IllegalArgumentException("null or empty 'profiles' array arg in method call.");
		}

		ProfileStore profileStore = context.getProfileStore();
		ProfileSet profileSet = profileStore.getProfileSet(useragent);
		
		if(profileSet == null) {
			throw new IllegalStateException("Call to registerProfiles() before the useragent [" + useragent + "] has been registered via registerUseragent.");
		}
		
		for(int i = 0; i < profiles.length; i++) {
			if(profiles[i] == null) {
				throw new IllegalArgumentException("null 'profiles' arg array element at index " + i);
			}			
			profileSet.addProfile(new BasicProfile(profiles[i]));
		}
	}

	/**
	 * Register a {@link SmooksResourceConfiguration} on this {@link SmooksStandalone} instance.
	 * @param resourceConfig The Content Delivery Resource definition to be  registered.
	 */
	public void registerResource(SmooksResourceConfiguration resourceConfig) {
		if(resourceConfig == null) {
			throw new IllegalArgumentException("null 'resourceConfig' arg in method call.");
		}
        context.getStore().registerResource(resourceConfig);
	}

    /**
     * Register the set of resources specified in the supplied XML configuration
     * stream.
     * @param name The name of the resource set.
     * @param resourceConfigStream XML resource configuration stream.
     * @throws SAXException Error parsing the resource stream.
     * @throws IOException Error reading resource stream.
     * @see SmooksResourceConfiguration
     */
    public void registerResources(String name, InputStream resourceConfigStream) throws SAXException, IOException {
        context.getStore().registerResources(name, resourceConfigStream);
    }    
}