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
package org.milyn.routing.jms;

import static org.testng.AssertJUnit.*;

import com.mockrunner.mock.ejb.EJBMockObjectFactory;
import com.mockrunner.mock.jms.JMSMockObjectFactory;
import com.mockrunner.mock.jms.MockQueue;
import com.mockrunner.mock.jms.MockQueueConnectionFactory;
import org.milyn.cdr.SmooksConfigurationException;
import org.milyn.cdr.SmooksResourceConfiguration;
import org.milyn.cdr.annotation.Configurator;
import org.milyn.container.MockApplicationContext;
import org.milyn.container.MockExecutionContext;
import org.milyn.delivery.sax.SAXElement;
import org.milyn.routing.SmooksRoutingException;
import org.milyn.routing.util.RouterTestHelper;
import org.mockejb.jndi.MockContextFactory;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.xml.sax.SAXException;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.TextMessage;
import javax.naming.Context;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;

/**
 * Unit test for the JMSRouter class
 *
 * @author <a href="mailto:daniel.bevenius@gmail.com">Daniel Bevenius</a>
 *
 */
public class JMSRouterTest
{
	private String selector = "x";
	private SmooksResourceConfiguration config = new SmooksResourceConfiguration(selector, JMSRouter.class.getName());
	private String queueName = "queue/testQueue";

	private static MockQueue queue;
	private static MockQueueConnectionFactory connectionFactory;

	@Test( groups = "unit", expectedExceptions = SmooksConfigurationException.class)
	public void configureWithMissingDestinationType()
	{
        Configurator.configure( new JMSRouter(), config, new MockApplicationContext() );
	}

	@Test ( groups = "integration" )
	public void visitAfter_below_hwmark() throws ParserConfigurationException, JMSException, SAXException, IOException
	{
		queue.clear();
		final String beanId = "beanId";
		final TestBean bean = RouterTestHelper.createBean();

        final MockExecutionContext executionContext = RouterTestHelper.createExecutionContext( beanId, bean );

        config.setParameter( "destinationName", queueName );
        config.setParameter( "beanId", beanId );
        final JMSRouter router = new JMSRouter();
        Configurator.configure( router, config, new MockApplicationContext() );

        router.visitAfter( (SAXElement)null, executionContext );

        final Message message = queue.getMessage();
        assertTrue ( "Message in queue should have been of type TextMessage",
        		message instanceof TextMessage );

        final TextMessage textMessage = (TextMessage) message;
        assertEquals( "Content of bean was not the same as the content of the TextMessage",
        		bean.toString(), textMessage.getText() );
	}

    @Test ( groups = "unit" )
    public void visitAfter_above_hwmark_notimeout() throws ParserConfigurationException, JMSException, SAXException, IOException
    {
        final String beanId = "beanId";
        final TestBean bean = RouterTestHelper.createBean();

        final MockExecutionContext executionContext = RouterTestHelper.createExecutionContext( beanId, bean );

        config.setParameter( "destinationName", queueName );
        config.setParameter( "beanId", beanId );
        config.setParameter( "highWaterMark", "3" );
        config.setParameter( "highWaterMarkPollFrequency", "200" );
        final JMSRouter router = new JMSRouter();
        Configurator.configure( router, config, new MockApplicationContext() );

        int numMessages = 10;
        ConsumeThread consumeThread = new ConsumeThread(queue, numMessages);
        consumeThread.start();

        // wait for the thread to start...
        while(consumeThread.running) {
            JMSRouterTest.sleep(500);
        }

        // Fire the messages...
        for(int i = 0; i < numMessages; i++) {
            router.visitAfter( (SAXElement)null, executionContext );
        }

        // wait for the thread to finish...
        while(consumeThread.running) {
            JMSRouterTest.sleep(500);            
        }

        assertEquals(numMessages, consumeThread.numMessagesProcessed);
    }

    @Test ( groups = "unit" )
    public void visitAfter_above_hwmark_timeout() throws ParserConfigurationException, JMSException, SAXException, IOException
    {
        final String beanId = "beanId";
        final TestBean bean = RouterTestHelper.createBean();

        final MockExecutionContext executionContext = RouterTestHelper.createExecutionContext( beanId, bean );

        config.setParameter( "destinationName", queueName );
        config.setParameter( "beanId", beanId );
        config.setParameter( "highWaterMark", "3" );
        config.setParameter( "highWaterMarkTimeout", "3000" );
        config.setParameter( "highWaterMarkPollFrequency", "200" );
        final JMSRouter router = new JMSRouter();
        Configurator.configure( router, config, new MockApplicationContext() );

        router.visitAfter( (SAXElement)null, executionContext );
        router.visitAfter( (SAXElement)null, executionContext );
        router.visitAfter( (SAXElement)null, executionContext );

        try {
            router.visitAfter( (SAXElement)null, executionContext );
            fail("Expected SmooksRoutingException");
        } catch(SmooksRoutingException e) {
            assertEquals("Failed to route JMS message to Queue destination 'testQueue'. Timed out (3000 ms) waiting for queue length to drop below High Water Mark (3).  Consider increasing 'highWaterMark' and/or 'highWaterMarkTimeout' param values.", e.getMessage());
        }
    }

    @Test ( groups = "unit" )
	public void setJndiContextFactory()
	{
		final String contextFactory = "org.jnp.interfaces.NamingContextFactory";
		setManadatoryProperties( config );
        config.setParameter( "jndiContextFactory", contextFactory );
        final JMSRouter router = new JMSRouter();
        Configurator.configure( router, config, new MockApplicationContext() );

        assertEquals( "ContextFactory did not match the one set on the Router",
        		contextFactory, router.getJndiContectFactory() );
	}

    @Test ( groups = "unit" )
	public void setJndiProviderUrl()
	{
		final String providerUrl = "jnp://localhost:1099";
		setManadatoryProperties( config );
        config.setParameter( "jndiProviderUrl", providerUrl );
        final JMSRouter router = new JMSRouter();
        Configurator.configure( router, config, new MockApplicationContext() );

        assertEquals( "ProviderURL did not match the one set on the Router",
        		providerUrl, router.getJndiProviderUrl() );
	}

    @Test ( groups = "unit" )
	public void setJndiNamingFactoryUrl()
	{
		final String namingFactoryUrlPkgs = "org.jboss.naming:org.jnp.interfaces";

		setManadatoryProperties( config );
        config.setParameter( "jndiNamingFactoryUrl", namingFactoryUrlPkgs );
        final JMSRouter router = new JMSRouter();
        Configurator.configure( router, config, new MockApplicationContext() );

        assertEquals( "NamingFactoryUrlPkg did not match the one set on the Router",
        		namingFactoryUrlPkgs, router.getJndiNamingFactoryUrl() );
	}
	
	@BeforeClass ( groups = { "unit", "integration" })
	public static void setUpInitialContext() throws Exception
    {
        final EJBMockObjectFactory mockObjectFactory = new EJBMockObjectFactory();
        final Context context = mockObjectFactory.getContext();
        final JMSMockObjectFactory jmsObjectFactory = new JMSMockObjectFactory();

        connectionFactory = jmsObjectFactory.getMockQueueConnectionFactory();
        context.bind("ConnectionFactory",  connectionFactory);
        queue = jmsObjectFactory.getDestinationManager().createQueue("testQueue");
        context.bind("queue/testQueue", queue);
		MockContextFactory.setAsInitial();
    }

	private void setManadatoryProperties( final SmooksResourceConfiguration config )
	{
        config.setParameter( "destinationName", queueName );
        config.setParameter( "beanId", "bla" );
	}

    class ConsumeThread extends Thread {

        private boolean running = false;
        private int numMessagesProcessed;
        private int numMessagesToProcesses;
        private MockQueue queue;

        ConsumeThread(MockQueue queue, int numMessagesToProcesses) {
            this.queue = queue;
            this.numMessagesToProcesses = numMessagesToProcesses;
        }

        public void run() {
            running = true;

            while(numMessagesProcessed < numMessagesToProcesses) {
                JMSRouterTest.sleep(500);
                if(!queue.isEmpty()) {
                    queue.getMessage();
                    numMessagesProcessed++;
                }
            }
            
            running = false;
        }
    }

    private static void sleep(long duration) {
        try {
            Thread.sleep(duration);
        } catch (InterruptedException e) {
        }
    }
}
