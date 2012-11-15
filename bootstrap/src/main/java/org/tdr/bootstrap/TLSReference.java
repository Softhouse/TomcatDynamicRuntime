package org.tdr.bootstrap;

import java.util.HashMap;

public class TLSReference {

	private static ThreadLocal<HashMap<String, Object>> tlsReferences = new ThreadLocal<HashMap<String, Object>>();
	
	static public Object get(String name) {
		
		return getMap().get(name);
	}
	
	static public void set(String name, Object object) {
		getMap().put(name, object);
	}
	
	static public void clear(String name) {
		getMap().remove(name);
	}
	
	static public void clear() {
		tlsReferences.remove();
	}
	
	static protected HashMap<String,Object> getMap() {
		HashMap<String,Object> map = tlsReferences.get();
		if ( map == null ) {
			map = new HashMap<String, Object>();
			tlsReferences.set(map);
		}
		return map;
	}
}
