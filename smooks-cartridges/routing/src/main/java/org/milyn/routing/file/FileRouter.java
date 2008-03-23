/*
 * Milyn - Copyright (C) 2006
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License (version 2.1) as published
 * by the Free Software Foundation.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.
 *
 * See the GNU Lesser General Public License for more details:
 * http://www.gnu.org/licenses/lgpl.txt
 */
package org.milyn.routing.file;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.milyn.SmooksException;
import org.milyn.cdr.annotation.ConfigParam;
import org.milyn.cdr.annotation.ConfigParam.Use;
import org.milyn.container.ExecutionContext;
import org.milyn.delivery.ExecutionLifecycleCleanable;
import org.milyn.delivery.annotation.Initialize;
import org.milyn.delivery.annotation.VisitAfterIf;
import org.milyn.delivery.annotation.VisitBeforeIf;
import org.milyn.delivery.dom.DOMElementVisitor;
import org.milyn.delivery.sax.SAXElement;
import org.milyn.delivery.sax.SAXElementVisitor;
import org.milyn.delivery.sax.SAXText;
import org.milyn.javabean.BeanAccessor;
import org.milyn.routing.file.io.ObjectOutputStrategy;
import org.milyn.routing.file.io.OutputStrategy;
import org.milyn.routing.file.io.OutputStrategyFactory;
import org.milyn.routing.file.naming.NamingStrategy;
import org.milyn.routing.file.naming.NamingStrategyException;
import org.milyn.routing.file.naming.TemplatedNamingStrategy;
import org.w3c.dom.Element;

/**
 * <p/>
 * FileRouter is a Visitor for DOM or SAX elements. It appends the content
 * to the configured destination file.
 * </p>
 * The name of the output file(s) is determined by a NamingStrategy, the
 * default being {@link TemplatedNamingStrategy}
 * </p>
 * As the number of files produced by a single transformation could be 
 * quite large the filename are not stored in memory.<br> 
 * Instead they are appended to a file. This is simple a list of the file names created
 * during the transformation. <br>
 * The name of this file containing the list of file will have the same
 * as the naming strategy with the ".lst" (list) suffix.
 * </p> 
 * Example configuration:
 * <pre>
 * &lt;resource-config selector="orderItems"&gt;
 *    &lt;resource&gt;org.milyn.routing.file.FileRouter&lt;/resource&gt;
 *    &lt;param name="beanId">beanId&lt;/param&gt;
 *    &lt;param name="destinationDirectory">dir&lt;/param&gt;
 *    &lt;param name="fileNamePattern">${orderid}-${customName}.txt&lt;/param&gt;
 * &lt;/resource-config&gt;
 * Optional parameters:
 *    &lt;param name="encoding"&gt;UTF-8&lt;/param&gt;
 * </pre>
 * @author <a href="mailto:daniel.bevenius@gmail.com">Daniel Bevenius</a>
 * @since 1.0
 *
 */
@VisitAfterIf(	condition = "!parameters.containsKey('visitBefore') || parameters.visitBefore.value != 'true'")
@VisitBeforeIf(	condition = "!parameters.containsKey('visitAfter') || parameters.visitAfter.value != 'true'")
public class FileRouter implements DOMElementVisitor, SAXElementVisitor, ExecutionLifecycleCleanable 
{
    public static final String ROUTE_TO_FILE_NAME_CONTEXT_KEY = FileRouter.class.getName() + "#routeToFileName:";
    public static final String LISTFILE_WRITER_CONTEXT_KEY = FileRouter.class.getName() + "#listFileWriter:";
    
	/*
	 * 	Log
	 */
    @SuppressWarnings( "unused" )
	private final Log log = LogFactory.getLog( FileRouter.class );
    
    /*
     * 	System line separator
     */
	private static final String LINE_SEPARATOR = System.getProperty( "line.separator" );

	/*
	 * 	Name of directory where files will be created.
	 */
	@ConfigParam ( name = "destinationDirectory", use = Use.REQUIRED )
	private String destDirName;

	/*
	 * 	File prefix for the created file
	 */
	@ConfigParam ( name = "fileNamePattern", use = Use.REQUIRED )
	private String fileNamePattern;

	/*
	 * 	BeanId is a key that is used to look up a bean
	 * 	in the execution context
	 */
    @ConfigParam( use = ConfigParam.Use.REQUIRED )
    private String beanId;

    /*
     *	Character encoding to be used when writing character
     * 	output
     */
    @ConfigParam( use = ConfigParam.Use.OPTIONAL, defaultVal = "UTF-8" )
	private String encoding;
    
    /*
     * 	File object of the destination directory
     */
    private File destinationDir;
    
	/*
	 * 	File name for the list file 
	 */
	@ConfigParam ( name = "listFileName", use = Use.REQUIRED )
	private String listFileName;

    /*
     * Naming strategy for generating the file pattern for output files.
     */
    private NamingStrategy namingStrategy = new TemplatedNamingStrategy();
	private String generatedFileName;


	@Initialize
	public void initialize()
	{
		destinationDir = new File ( destDirName );
    	if ( !destinationDir.exists() || !destinationDir.isDirectory() )
    	{
    		throw new SmooksException ( "Destination directory [" + destDirName + "] does not exist or is not a directory.");
    	}
	}

	//	Vistor methods

	public void visitAfter( final Element element, final ExecutionContext executionContext ) throws SmooksException
	{
		visit( executionContext );
	}

	public void visitBefore( final Element element, final ExecutionContext executionContext ) throws SmooksException
	{
		visit( executionContext );
	}

	public void visitAfter( final SAXElement saxElement, final ExecutionContext executionContext ) throws SmooksException, IOException
	{
		visit( executionContext );
	}

	public void visitBefore( final SAXElement saxElement, final ExecutionContext executionContext ) throws SmooksException, IOException
	{
		visit( executionContext );
	}

	public void onChildElement( final SAXElement saxElement, final SAXElement arg1, final ExecutionContext executionContext ) throws SmooksException, IOException
	{
		//	NoOp
	}

	public void onChildText( final SAXElement saxElement, final SAXText saxText, final ExecutionContext executionContext ) throws SmooksException, IOException
	{
		//	NoOp
	}
	
	public void executeExecutionLifecycleCleanup( ExecutionContext executionContext )
	{
		//	close output strategy
		OutputStrategy outputStrategy = (ObjectOutputStrategy) executionContext.getAttribute( ROUTE_TO_FILE_NAME_CONTEXT_KEY + generatedFileName );
		if ( outputStrategy != null )
		{
			outputStrategy.close();
		}
		
		//	close list file writer
		FileWriter writer = (FileWriter) executionContext.getAttribute( LISTFILE_WRITER_CONTEXT_KEY + listFileName );
		if ( writer != null )
		{
			try
			{
				writer.close();
			} 
			catch (IOException e)
			{
				log.error( "IOException while closing writer: " + e  );
			}
		}
	}

	//	protected
	protected String generateFilePattern( final Object object )
	{
    	try
		{
			return namingStrategy.generateFileName( fileNamePattern, object );
		} 
    	catch (NamingStrategyException e)
		{
    		throw new SmooksException( e.getMessage(), e );
		}
	}
	
	//	private
	

	/**
	 * 	Extracts the bean identified by beanId and append that object
	 * 	to the destination file.
	 *
	 * 	@param executionContext
	 *  @throws SmooksException	if the bean cannot be found in the ExecutionContext
	 */
	private void visit( final ExecutionContext executionContext ) throws SmooksException
	{
        final Object bean = BeanAccessor.getBean( executionContext, beanId );
        if ( bean == null )
        {
        	throw new SmooksException( "A bean with id [" + beanId + "] was not found in the executionContext");
        }
        
		generatedFileName = destinationDir.getAbsolutePath() + File.separator + generateFilePattern( bean );
		
		OutputStrategy outputStrategy = (ObjectOutputStrategy) executionContext.getAttribute( ROUTE_TO_FILE_NAME_CONTEXT_KEY + generatedFileName );
		try
		{
			if ( outputStrategy == null )
			{
        		outputStrategy = OutputStrategyFactory.getInstance().createStrategy( generatedFileName, bean );
        		executionContext.setAttribute( ROUTE_TO_FILE_NAME_CONTEXT_KEY + generatedFileName , outputStrategy );
			}
			
			outputStrategy.write( bean, encoding );
			outputStrategy.flush();
    		addFileToFileList( generatedFileName, executionContext );
		}
		catch (IOException e)
		{
    		final String errorMsg = "IOException while trying to append to file [" + destinationDir + "/" + fileNamePattern + "]";
    		throw new SmooksException( errorMsg, e );
		}
		finally
		{
			if ( outputStrategy != null )
				outputStrategy.close();
		}
	}
	
	private void addFileToFileList( final String transformedFileName, final ExecutionContext executionContext ) throws IOException
	{
		final String listFilePath = destDirName + File.separator + this.listFileName;
		FileWriter writer = (FileWriter) executionContext.getAttribute( LISTFILE_WRITER_CONTEXT_KEY + listFilePath );
		if ( writer == null )
		{
			writer = new FileWriter( listFilePath, true );
			//	set the Writer object in the execution context.
			executionContext.setAttribute( LISTFILE_WRITER_CONTEXT_KEY + listFilePath, writer );
			
			//	set the list file name in the execution context
			FileListAccessor.setFileName( listFilePath, executionContext );
		}
		
		log.debug( "writing to filelist file [" + listFilePath + "] fileName [" + transformedFileName + "]" );
		writer.write( transformedFileName + LINE_SEPARATOR );
		writer.flush();
	}

	
	

}
