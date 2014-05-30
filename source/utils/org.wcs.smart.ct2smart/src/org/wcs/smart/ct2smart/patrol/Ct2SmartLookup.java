package org.wcs.smart.ct2smart.patrol;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.wcs.smart.ct2smart.matcher.model.Ct2Attribute;
import org.wcs.smart.ct2smart.matcher.model.Ct2Smart;
import org.wcs.smart.ct2smart.matcher.model.CtCategory;
import org.wcs.smart.ct2smart.matcher.model.CtCategoryMap;

public class Ct2SmartLookup {

	private Ct2Smart ct2Smart;
	private Map<String, Ct2Attribute> i2attr = new HashMap<String, Ct2Attribute>();

	public Ct2SmartLookup(Ct2Smart ct2Smart) {
		this.ct2Smart = ct2Smart;
		for (Ct2Attribute ct2a : ct2Smart.getCt2Attribute()) {
			i2attr.put(ct2a.getI(), ct2a);
		}
		
		for (CtCategory c : ct2Smart.getCtCategory()) {
			for (CtCategoryMap cmap : c.getCtCategoryMap()) {
				cmap.getAi();
			}
		}
	}

	public Ct2Attribute findAttribute(String i) {
		return i2attr.get(i);
	}
	
	public CtCategory findCategory(List<Ct2AttributeValuePair> data) {
		//brood force
		for (CtCategory c : ct2Smart.getCtCategory()) {
			if (c.getCtCategoryMap().size() != data.size())
				continue;
			boolean match = false;
			for (CtCategoryMap cmap : c.getCtCategoryMap()) {
				match = false;
				for (Ct2AttributeValuePair pair : data) {
					if (cmap.getAi().equals(pair.attribute.getI()) && cmap.getVi().equals(pair.value)) {
						match = true;
						break;
					}
				}
				if (!match) {
					break;
				}
			}
			if (match)
				return c;
		}
		return null;
	}

	
	
	public static class Ct2AttributeValuePair {
		public Ct2Attribute attribute;
		public String value;
	}
	
}
