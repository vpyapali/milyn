/**
 * 
 */
package org.milyn.javabean.virtual;

import java.util.List;
import java.util.Map;

import org.milyn.container.ApplicationContext;
import org.milyn.javabean.virtual.javassist.JavassistVirtualBeanGenerator;


/**
 * Generates a class which implements a Map interface but can only
 * receive values from the configured bindings of a resource. This
 * should result in a very fast Map which can be used for virtual maps.
 * 
 * 
 * @author <a href="mailto:maurice.zeijen@smies.com">maurice.zeijen@smies.com</a>
 *
 */
public interface VirtualBeanGenerator {
	
	void initialize(ApplicationContext applicationContext);
	
	@SuppressWarnings("unchecked")
	Map<String, ?> generate(String name, List<String> keys);
	
	
	public static final String IMPLEMENTATION_CONTEXT_KEY = VirtualBeanGenerator.class.getName() + "#IMPLEMENTATION";

	public static class Factory {
		
		public static final String DEFAULT_IMPLEMENTATION = JavassistVirtualBeanGenerator.class.getName();

		public static final String INSTANCE_CONTEXT_KEY = Factory.class.getName() + "#INSTANCE";

		public static VirtualBeanGenerator create(ApplicationContext applicationContext) {
			
			VirtualBeanGenerator virtualBeanGenerator = (VirtualBeanGenerator) applicationContext.getAttribute(INSTANCE_CONTEXT_KEY);
			
			if(virtualBeanGenerator == null) {
				
				String mapGeneratorImplementation = (String) applicationContext.getAttribute(IMPLEMENTATION_CONTEXT_KEY);
				
				if(mapGeneratorImplementation == null) {
					mapGeneratorImplementation = DEFAULT_IMPLEMENTATION;
				}
				
				try {
					Class<?> mapGeneratorClass = applicationContext.getClass().getClassLoader().loadClass(mapGeneratorImplementation);
					
					virtualBeanGenerator = (VirtualBeanGenerator) mapGeneratorClass.newInstance();
					
					virtualBeanGenerator.initialize(applicationContext);
					
				} catch (ClassNotFoundException e) {
					throw new RuntimeException("Configured factory implementation class '" 
							+ mapGeneratorImplementation + "' not found", e);
				} catch (InstantiationException e) {
					throw new RuntimeException("Configured factory implementation class '" 
							+ mapGeneratorImplementation + "' could not be instantiated. " 
							+ "Make sure it has a parameterless public constructor.", e);
				} catch (IllegalAccessException e) {
					throw new RuntimeException("Configured factory implementation class '"
							+ mapGeneratorImplementation + "' could not be instantiated. " 
							+ "Make sure it has a parameterless public constructor.", e);
				}
				
				applicationContext.setAttribute(INSTANCE_CONTEXT_KEY, virtualBeanGenerator);
				
			}
			
			return virtualBeanGenerator;
			
			
		}	
		
		
	}
}