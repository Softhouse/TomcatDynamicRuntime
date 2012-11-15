package org.tdr.runtime;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;
import java.net.URL;

import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;

public class ResourceFile extends ResourceNode {

	private static final Log log = LogFactory.getLog(ResourceFile.class);
		
	private String entryPathInBundle;
			
	ResourceFile(ResourceDirectory parent, String name, TDRBundle bundle) {
		super(parent, name, bundle);
		this.entryPathInBundle = this.getEntryPathInBundle();
	}
	
	public URL getUrl() {
		return this.bundle.getBundle().getEntry(this.entryPathInBundle);
	}

	@Override
	public void copyTo(String path) throws IOException {
		
		URL url = this.getUrl();
		
		// Verify that full path exists -> if not create it
		//
		File copyToPath = new File(path);
		if ( ! copyToPath.exists() ) {
			copyToPath.mkdirs();
		}
		
		String filePath = path + "/" + this.name;
		
		log.info("Copy resource to: " + filePath);
		
		FileOutputStream fos = new FileOutputStream(filePath);
		InputStream urlStream = url.openStream();
		byte buf[] = new byte[1028];
		
		while ( true ) { // TODO: There must exist some standard util for this kind of code??
			int len = urlStream.read(buf);
			if ( len > 0 ) {
				fos.write(buf, 0, len);
			}
			if ( len == -1 ) break;
		}
		fos.close();
	}
	
	private String getEntryPathInBundle() {
		return this.getFullPath().substring(1);
	}

	@Override
	public void removeFrom(String path) throws IOException {
		String filePath = path + "/" + this.name;
		log.info("Deleting file: " + filePath);
		File file = new File(filePath);
		file.delete();
	}

	@Override
	public void writeInfo(String path, Writer writer) throws IOException {
		String fileInfo = path + "/" + this.name + "\n";
		writer.write(fileInfo);
		
	}

}
