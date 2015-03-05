package org.wcs.smart.conversion.lookup;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.wcs.smart.conversion.model.CategoryMap;
import org.wcs.smart.conversion.model.MappedAttribute;
import org.wcs.smart.conversion.model.MappedCategory;
import org.wcs.smart.conversion.model.SmartMapping;

/**
 * @author elitvin
 * @since 3.0.0
 */
public class Ct2SmartLookup {

	private SmartMapping ct2Smart;
	private Map<String, MappedAttribute> i2attr = new HashMap<String, MappedAttribute>();

	public Ct2SmartLookup(SmartMapping ct2Smart) {
		this.ct2Smart = ct2Smart;
		for (MappedAttribute ct2a : ct2Smart.getMappedAttribute()) {
			i2attr.put(ct2a.getI(), ct2a);
		}
		
		for (MappedCategory c : ct2Smart.getMappedCategory()) {
			for (CategoryMap cmap : c.getCategoryMap()) {
				cmap.getAi();
			}
		}
	}

	public MappedAttribute findAttribute(String i) {
		return i2attr.get(i);
	}
	
	public MappedCategory findCategory(List<Ct2AttributeValuePair> data) {
		//brood force
		for (MappedCategory c : ct2Smart.getMappedCategory()) {
			if (c.getCategoryMap().size() != data.size())
				continue;
			boolean match = false;
			for (CategoryMap cmap : c.getCategoryMap()) {
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
		public MappedAttribute attribute;
		public String value;
	}
	
}
