package org.tdr.bootstrap;

import java.util.HashMap;

public final class GlobalReference {

	private static HashMap<String, Object> references = new HashMap<String, Object>();
	
	static public Object get(String name) {
		return references.get(name);
	}
	
	static public void set(String name, Object object) {
		references.put(name, object);
	}
}
