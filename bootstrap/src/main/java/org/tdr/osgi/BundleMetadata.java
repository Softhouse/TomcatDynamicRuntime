package org.tdr.osgi;

import java.util.Dictionary;

import org.osgi.framework.Bundle;

public class BundleMetadata {

	private String name;
	private BundleVersion version;
	
	public BundleMetadata(Bundle bundle) {
		Dictionary<String, String> headers = bundle.getHeaders();
		this.name = headers.get("Bundle-Name");
		this.version = new BundleVersion(headers.get("Bundle-Version"));
		
	}
	
	public String getName() { return this.name; }
	public BundleVersion getVersion() { return this.version; }
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((version == null) ? 0 : version.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		BundleMetadata other = (BundleMetadata) obj;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (version == null) {
			if (other.version != null)
				return false;
		} else if (!version.equals(other.version))
			return false;
		return true;
	}
	
}
