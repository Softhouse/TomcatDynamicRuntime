package org.apache.catalina.core;

import java.lang.reflect.InvocationTargetException;

import javax.naming.NamingException;
import javax.servlet.ServletException;

import org.apache.catalina.Context;
import org.apache.catalina.deploy.FilterDef;

public final class CatalinaManager {

	public static ApplicationFilterConfig createFilterConfig(Context context, FilterDef filterDef) 
			throws ClassCastException, ClassNotFoundException, IllegalAccessException, InstantiationException, 
				   ServletException, InvocationTargetException, NamingException {
		return new ApplicationFilterConfig(context, filterDef);
	}
	
	public static void releaseFilterConfig(ApplicationFilterConfig filterConfig) {
		filterConfig.release();
	}
}
