package org.wcs.smart.ct2smart.patrol;

import java.util.HashMap;
import java.util.Map;

import org.wcs.smart.ct2smart.matcher.model.Ct2Attribute;
import org.wcs.smart.ct2smart.matcher.model.Ct2Smart;

public class Ct2SmartLookup {

	private Ct2Smart ct2Smart;
	private Map<String, Ct2Attribute> i2attr = new HashMap<String, Ct2Attribute>();

	public Ct2SmartLookup(Ct2Smart ct2Smart) {
		this.ct2Smart = ct2Smart;
		for (Ct2Attribute ct2a : ct2Smart.getCt2Attribute()) {
			i2attr.put(ct2a.getI(), ct2a);
		}
	}

	public Ct2Attribute findAttribute(String i) {
		return i2attr.get(i);
	}
	
}
