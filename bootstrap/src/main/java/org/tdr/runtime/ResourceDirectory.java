package org.tdr.runtime;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.StringTokenizer;

import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;

// TODO: This class should be made fully immutable!!
// TODO: Make unit test for those classes!!!
// TODO: Ska denna klass representera både katalog i bundle och i bundle repo??

public class ResourceDirectory extends ResourceNode {

	private static final Log log = LogFactory.getLog(ResourceDirectory.class);
	
	private Collection<ResourceNode> items;
	
	private String mappedDirectoryName = null;
	
	ResourceDirectory(ResourceDirectory parent, String name, TDRBundle bundle) {
		super(parent, name, bundle);
	}
	
	ResourceDirectory(ResourceDirectory parent, String name, String mappedDirectoryName, TDRBundle bundle) {
		super(parent, name, bundle);
		this.mappedDirectoryName = mappedDirectoryName;
	}

	
	// TODO: This is not nice code!! MUTABLE!!!
	void setChildDirectory(ResourceDirectory child) {
		this.items = new ArrayList<ResourceNode>();
		this.items.add(child);
	}
	
	static public ResourceDirectory buildDirectories(String absolutePath, TDRBundle bundle) {
		StringTokenizer tokenizer = new StringTokenizer(absolutePath, "/");
		ResourceDirectory parent = null;
		ResourceDirectory top = null;
		ResourceDirectory current;
		
		while ( tokenizer.hasMoreTokens() ) {
			String name = tokenizer.nextToken();
			if ( name.contains("=") ) {
				StringTokenizer mappedNameTokenizer = new StringTokenizer(name, "=");
				name = mappedNameTokenizer.nextToken();
				String mappedName = mappedNameTokenizer.hasMoreTokens() ? mappedNameTokenizer.nextToken() : "";
				current = new ResourceDirectory(parent, name, mappedName, bundle);
			}
			else {
				current = new ResourceDirectory(parent, name, bundle);
			}
			if ( parent == null ) {
				top = current;
			}
			else {
				parent.setChildDirectory(current);
			}
			parent = current;
		}
		return top;
	}
	
	public Collection<ResourceNode> getItems() {
		if ( this.items == null ) {
			this.items = new ArrayList<ResourceNode>();
			String fullPath = this.getFullPath();
			Enumeration<String> entryPaths = this.bundle.getBundle().getEntryPaths(fullPath);
			if ( entryPaths != null ) {
				String basePath = fullPath.substring(1) + "/"; // Exclude leading slash		
				
				while ( entryPaths.hasMoreElements() ) {
					String entryPath = entryPaths.nextElement();
					String itemName = entryPath.replaceFirst(basePath, "");
					ResourceNode resourceNode;
					if ( itemName.endsWith("/") ) { 
						resourceNode = new ResourceDirectory(this, itemName.replace("/", ""), this.bundle);
					}
					else {
						resourceNode = new ResourceFile(this, itemName, this.bundle);
					}
					this.items.add(resourceNode);
				}
			}
		}
		return this.items;
	}

	@Override
	public void copyTo(String path) throws IOException {
		
		String extendedPath;
		if ( this.mappedDirectoryName != null ) {
			extendedPath = path + "/" + (this.mappedDirectoryName.equals("/") ? "" : this.mappedDirectoryName);
		}
		else {
			extendedPath = path + "/" + this.name;
		}
		for ( ResourceNode node : this.getItems() ) {
			node.copyTo(extendedPath);
		}	
	}

	@Override
	public void removeFrom(String path) throws IOException {
		
		String extendedPath;
		if ( this.mappedDirectoryName != null ) {
			extendedPath = path + "/" + (this.mappedDirectoryName.equals("/") ? "" : this.mappedDirectoryName);
		}
		else {
			extendedPath = path + "/" + this.name;
		}
		for ( ResourceNode node : this.getItems() ) {
			node.removeFrom(extendedPath);
		}
		File dir = new File(extendedPath);
		File[] dirContent = dir.listFiles();
		if ( dirContent != null && dirContent.length == 0 ) {
			dir.delete();
		}
	}

	@Override
	public void writeInfo(String path, Writer writer) throws IOException {
		String extendedPath;
		if ( this.mappedDirectoryName != null ) {
			extendedPath = path + "/" + (this.mappedDirectoryName.equals("/") ? "" : this.mappedDirectoryName);
		}
		else {
			extendedPath = path + "/" + this.name;
		}
		for ( ResourceNode node : this.getItems() ) {
			node.writeInfo(extendedPath, writer);
		}
		writer.write(extendedPath + "\n");
		
	}
}
