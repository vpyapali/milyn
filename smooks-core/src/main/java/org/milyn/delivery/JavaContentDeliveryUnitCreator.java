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

package org.milyn.delivery;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import org.milyn.cdr.ClasspathUtils;
import org.milyn.cdr.SmooksResourceConfiguration;

/**
 * Java ContentDeliveryUnit instance creator.
 * <p/>
 * Java-based ContentDeliveryUnit implementations should contain a public 
 * constructor that takes a SmooksResourceConfiguration instance as a parameter.
 * @see XslContentDeliveryUnitCreator 
 * @author tfennelly
 */
public class JavaContentDeliveryUnitCreator implements ContentDeliveryUnitCreator {

	/**
	 * Create a Java based ContentDeliveryUnit instance ie from a Java Class byte stream.
	 * <p/>
	 * @throws ClassNotFoundException 
	 * @see XslContentDeliveryUnitCreator 
	 */
	public synchronized ContentDeliveryUnit create(SmooksResourceConfiguration resourceConfig) throws InstantiationException {
		ContentDeliveryUnit deliveryUnit = null;
        Exception exception = null;
		
		try {
            String className = ClasspathUtils.toClassName(resourceConfig.getPath());
			Class classRuntime = Class.forName(className);
			Constructor constructor = classRuntime.getConstructor(new Class[] {SmooksResourceConfiguration.class});
			
			deliveryUnit = (ContentDeliveryUnit)constructor.newInstance(new Object[] {resourceConfig});
		} catch (NoSuchMethodException e) {
            exception = e;
        } catch (InstantiationException e) {
            exception = e;
        } catch (IllegalAccessException e) {
            exception = e;
        } catch (InvocationTargetException e) {
            exception = e;
        } catch (ClassNotFoundException e) {
            exception = e;
        } finally {
            // One of the above exception.
            if(exception != null) {
                IllegalStateException state = new IllegalStateException("Failed to create an instance of Java ContentDeliveryUnit [" + resourceConfig.getPath() + "].  See exception cause...");
                state.initCause(exception);
                throw state;
            }
        }
		
		return deliveryUnit;
	}
}