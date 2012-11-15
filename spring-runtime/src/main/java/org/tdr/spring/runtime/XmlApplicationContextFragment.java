package org.tdr.spring.runtime;

import javax.servlet.ServletContext;

import org.springframework.context.support.GenericXmlApplicationContext;
import org.springframework.web.context.WebApplicationContext;

public class XmlApplicationContextFragment extends GenericXmlApplicationContext implements WebApplicationContext {

	private ServletContext servletContext;
	
	public XmlApplicationContextFragment(ServletContext servletContext) {
		this.servletContext = servletContext;
	}

	@Override
	public ServletContext getServletContext() {
		return this.servletContext;
	}
	

}
