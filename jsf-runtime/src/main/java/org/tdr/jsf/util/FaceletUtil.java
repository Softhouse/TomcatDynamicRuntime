package org.tdr.jsf.util;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import javax.faces.component.UIComponent;

import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;

import com.sun.facelets.FaceletContext;
import com.sun.facelets.impl.DefaultFaceletFactory;

public abstract class FaceletUtil
{
	static private Log log = LogFactory.getLog(FaceletUtil.class);
	
	/**
	 * Include facelet from a content string.
	 * 
	 * @param ctx
	 * @param parent
	 * @param contentStr
	 * @throws IOException
	 */
	static public void includeFacelet(FaceletContext ctx, UIComponent parent, String contentStr) throws IOException
	{
		String alias = "facelet" + contentStr.hashCode() + "/" + contentStr.length() + ".stream";
		ByteArrayInputStream is = new ByteArrayInputStream(contentStr.getBytes("UTF-8"));
		DefaultFaceletFactory.setFaceletInputStream(is);
		ctx.includeFacelet(parent, alias);
		DefaultFaceletFactory.setFaceletInputStream(null);

	}
}