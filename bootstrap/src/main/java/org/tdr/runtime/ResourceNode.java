package org.tdr.runtime;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;

public abstract class ResourceNode {

	protected ResourceDirectory parent;
	protected String name;
	protected TDRBundle bundle;
	
	protected ResourceNode(ResourceDirectory parent, String name, TDRBundle bundle) {
		this.parent = parent;
		this.name = name;
		this.bundle = bundle;
	}
	
	public String getFullPath() {
		String fullPath = "";
		if ( this.parent != null ) {
			fullPath = this.parent.getFullPath();
		}
		fullPath += "/" + this.name;
		return fullPath;
	}
	
	public ResourceDirectory getParent()  { return this.parent; }
	
	public String getName() { return this.name; }
	
	public abstract void copyTo(String path) throws IOException;
	
	public abstract void removeFrom(String path) throws IOException;
	
	public abstract void writeInfo(String path, Writer writer) throws IOException;
}
