package org.tdr.spring.runtime;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.context.NoSuchMessageException;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.web.context.ConfigurableWebApplicationContext;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.XmlWebApplicationContext;

/**
 * Virtual Web Application Context.
 * 
 * @author nic
 */
public class VirtualWebApplicationContext implements WebApplicationContext, ConfigurableWebApplicationContext {

	private static final Log log = LogFactory.getLog(VirtualWebApplicationContext.class);
		
	private XmlWebApplicationContext baseApplicationContext;
	
	private List<GenericApplicationContext> applicationContexts = new ArrayList<GenericApplicationContext>();
	
	
	public VirtualWebApplicationContext(XmlWebApplicationContext baseApplicationContext) {
		this.baseApplicationContext = baseApplicationContext;
	}
	
	public void addApplicationContext(GenericApplicationContext context) {
		this.applicationContexts.add(context);
	}
	
	public void removeApplicationContext(GenericApplicationContext context) {
		this.applicationContexts.remove(context);
	}
	
	protected static Class<?>[] getParameterTypes(Object... args) {
		Class<?>[] parameterTypes = new Class<?>[args.length];
		for ( int i=0; i < args.length; i++ ) {
			parameterTypes[i] = args[i].getClass();
		}
		return parameterTypes;
	}
	
	protected static Method getMethod(String methodName, Object... args) throws NoSuchMethodException {
		return GenericApplicationContext.class.getMethod(methodName, getParameterTypes(args));
	}
	
	protected Object retrieve(String methodName, Object... args) {
		
		try {
			Method method = getMethod(methodName, args);
			for ( GenericApplicationContext context : applicationContexts ) {
				try {
					Object result = method.invoke(context, args);
					if ( result != null ) {
						return result;
					}
				}
				catch ( InvocationTargetException e ) {
					log.error("Error while invoking method: " + methodName, e);
					//throw e.getTargetException();
				}
				catch ( IllegalAccessException e ) {
					log.error("Error while invoking method: " + methodName, e);
				}
			}
		}
		catch ( NoSuchMethodException e ) {
		}
		return null;
	}
	
	protected boolean applyUntilTrue(String methodName, Object...args) {
		try {
			Method method = getMethod(methodName, args);
			for ( GenericApplicationContext context : applicationContexts ) {
				try {
					boolean result = (boolean) method.invoke(context, args);
					if ( result ) {
						return true;
					}
				}
				catch ( InvocationTargetException | IllegalAccessException e ) {
					log.error("Error while invoking method: " + methodName, e);
				}
			}
		}
		catch ( NoSuchMethodException e ) {
		}
		return false;
	}
	
	protected void apply(String methodName, Object... args) {
		try {
			Method method = getMethod(methodName, args);
			for ( GenericApplicationContext context : applicationContexts ) {
				try {
					method.invoke(context, args);
				}
				catch ( InvocationTargetException | IllegalAccessException e ) {
					log.error("Error while invoking method: " + methodName, e);
				}
			}
		}
		catch ( NoSuchMethodException e ) {
		}
	}

	public void publishEvent(ApplicationEvent event) {
		apply("publishEvent", event);
	}

	public BeanFactory getParentBeanFactory() {
		return baseApplicationContext.getParentBeanFactory();
	}

	public boolean containsLocalBean(String name) {
		return applyUntilTrue("containsLocalBean", name);
	}

	public String getMessage(String code, Object[] args, String defaultMessage, Locale locale) {
		return (String) retrieve("getMessage", code, args, defaultMessage, locale);
	}

	public Resource getResource(String location) {
		return (Resource) retrieve("getResource", location);
	}

	public String getMessage(String code, Object[] args, Locale locale) throws NoSuchMessageException {
		return (String) retrieve("getMessage", code, args, locale);
	}

	public boolean containsBeanDefinition(String beanName) {
		return baseApplicationContext.containsBeanDefinition(beanName);
	}

	public String getId() {
		return baseApplicationContext.getId();
	}

	public ClassLoader getClassLoader() {
		return baseApplicationContext.getClassLoader();
	}

	public Resource[] getResources(String locationPattern) throws IOException {
		return (Resource[]) retrieve("getResources", locationPattern);
	}

	public String getDisplayName() {
		return baseApplicationContext.getDisplayName();
	}

	public String getMessage(MessageSourceResolvable resolvable, Locale locale)
			throws NoSuchMessageException {
		return baseApplicationContext.getMessage(resolvable, locale);
	}

	public long getStartupDate() {
		return baseApplicationContext.getStartupDate();
	}

	public int getBeanDefinitionCount() {
		return baseApplicationContext.getBeanDefinitionCount();
	}

	public ApplicationContext getParent() {
		return baseApplicationContext.getParent();
	}

	public String[] getBeanDefinitionNames() {
		return baseApplicationContext.getBeanDefinitionNames();
	}

	public AutowireCapableBeanFactory getAutowireCapableBeanFactory()
			throws IllegalStateException {
		return baseApplicationContext.getAutowireCapableBeanFactory();
	}

	public String[] getBeanNamesForType(Class type) {
		return this.getBeanNamesForType(type, true, true);
	}
	
	public String[] getBeanNamesForType(Class type, boolean includeNonSingletons, boolean allowEagerInit) {
		ArrayList<String> mergedBeanNames = new ArrayList<String>();
		for ( GenericApplicationContext context : this.applicationContexts ) {
			String[] beanNames = context.getBeanNamesForType(type);
			if ( beanNames != null && beanNames.length > 0 ) {
				for ( String beanName : beanNames ) {
					mergedBeanNames.add(beanName);
				}
			}
		}
		return mergedBeanNames.toArray(new String[0]);
	}

	public ServletContext getServletContext() {
		return baseApplicationContext.getServletContext();
	}

	public Object getBean(String name) throws BeansException {
		return retrieve("getBean", name);
	}

	public <T> T getBean(String name, Class<T> requiredType) throws BeansException {
		return (T) retrieve("getBean", name, requiredType);
	}

	public <T> Map<String, T> getBeansOfType(Class<T> type) throws BeansException {
		return (Map<String,T>) retrieve("getBeansOfType", type);
	}

	public <T> T getBean(Class<T> requiredType) throws BeansException {
		return (T) retrieve("getBean", requiredType);
	}

	public Object getBean(String name, Object... args) throws BeansException {
		return retrieve("getBean", name, args);
	}

	public <T> Map<String, T> getBeansOfType(Class<T> type, boolean includeNonSingletons, boolean allowEagerInit)
			throws BeansException {
		Map<String,T> mergedBeansMap = new HashMap<String,T>();
		for ( GenericApplicationContext context : this.applicationContexts ) {
			Map<String, T> beansMap = context.getBeansOfType(type, includeNonSingletons, allowEagerInit);
			if ( ! beansMap.isEmpty() ) {
				mergedBeansMap.putAll(beansMap);
			}
		}
		return mergedBeansMap;
	}

	public boolean containsBean(String name) {
		return applyUntilTrue("containtsBean", name);
	}

	public boolean isSingleton(String name)
			throws NoSuchBeanDefinitionException {
		return applyUntilTrue("isSingleton", name);
	}

	public boolean isPrototype(String name)
			throws NoSuchBeanDefinitionException {
		return applyUntilTrue("isPrototype", name);
	}

	public Map<String, Object> getBeansWithAnnotation(
			Class<? extends Annotation> annotationType) throws BeansException {
		return (Map<String,Object>) retrieve("getBeansWithAnnotation", annotationType);
	}

	public boolean isTypeMatch(String name, Class targetType)
			throws NoSuchBeanDefinitionException {
		return applyUntilTrue("isTypeMatch", name, targetType);
	}

	public <A extends Annotation> A findAnnotationOnBean(String beanName, Class<A> annotationType) {
		return (A) retrieve("findAnnotationOnBean", beanName, annotationType);
	}

	public Class<?> getType(String name) throws NoSuchBeanDefinitionException {
		return (Class<?>) retrieve("getType", name);
	}

	public String[] getAliases(String name) {
		return (String[]) retrieve("getAliases", name);
	}

	@Override
	public void setId(String id) {
		this.baseApplicationContext.setId(id);
	}

	@Override
	public void setParent(ApplicationContext parent) {
		this.baseApplicationContext.setParent(parent);
	}

	@Override
	public void addBeanFactoryPostProcessor(BeanFactoryPostProcessor beanFactoryPostProcessor) {
		this.baseApplicationContext.addBeanFactoryPostProcessor(beanFactoryPostProcessor);
	}

	@Override
	public void addApplicationListener(ApplicationListener listener) {
		this.baseApplicationContext.addApplicationListener(listener);
	}

	@Override
	public void refresh() throws BeansException, IllegalStateException {
		this.baseApplicationContext.refresh();
	}

	@Override
	public void registerShutdownHook() {
		this.baseApplicationContext.registerShutdownHook();
	}

	@Override
	public void close() {
		apply("close");
		this.baseApplicationContext.close();
	}

	@Override
	public boolean isActive() {
		return this.baseApplicationContext.isActive();
	}

	@Override
	public ConfigurableListableBeanFactory getBeanFactory() throws IllegalStateException {
		return this.baseApplicationContext.getBeanFactory();
	}

	@Override
	public void start() {
		this.baseApplicationContext.start();
		apply("start");
	}

	@Override
	public void stop() {
		apply("stop");
		this.baseApplicationContext.stop();
	}

	@Override
	public boolean isRunning() {
		return this.baseApplicationContext.isRunning();
	}

	@Override
	public void setServletContext(ServletContext servletContext) {
		this.baseApplicationContext.setServletContext(servletContext);
		
	}

	@Override
	public void setServletConfig(ServletConfig servletConfig) {
		this.baseApplicationContext.setServletConfig(servletConfig);
	}

	@Override
	public ServletConfig getServletConfig() {
		return this.baseApplicationContext.getServletConfig();
	}

	@Override
	public void setNamespace(String namespace) {
		this.baseApplicationContext.setNamespace(namespace);
	}

	@Override
	public String getNamespace() {
		return this.baseApplicationContext.getNamespace();
	}

	@Override
	public void setConfigLocation(String configLocation) {
		this.baseApplicationContext.setConfigLocation(configLocation);
	}

	@Override
	public void setConfigLocations(String[] configLocations) {
		this.baseApplicationContext.setConfigLocations(configLocations);
	}

	@Override
	public String[] getConfigLocations() {
		return this.baseApplicationContext.getConfigLocations();
	}

}
