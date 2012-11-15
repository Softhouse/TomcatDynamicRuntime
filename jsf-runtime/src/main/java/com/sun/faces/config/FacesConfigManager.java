package com.sun.faces.config;

import javax.faces.context.FacesContext;
import javax.servlet.ServletContext;

public final class FacesConfigManager {

	public static FacesContext createInitFacesContext(ServletContext servletContext) {
		return new InitFacesContext(servletContext);
	}
}
