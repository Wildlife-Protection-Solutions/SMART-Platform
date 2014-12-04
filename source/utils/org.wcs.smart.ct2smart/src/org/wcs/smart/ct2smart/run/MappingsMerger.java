package org.wcs.smart.ct2smart.run;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.JAXBException;

import org.wcs.smart.ct2smart.matcher.FileUtil;
import org.wcs.smart.ct2smart.matcher.model.Ct2Attribute;
import org.wcs.smart.ct2smart.matcher.model.Ct2AttributeValue;
import org.wcs.smart.ct2smart.matcher.model.Ct2Smart;
import org.wcs.smart.ct2smart.patrol.Ct2SmartLookup;

public class MappingsMerger {

	public static void main(String[] args) throws JAXBException, IOException {
		System.out.println("Merging mappings...");
		String fromFileStr = args.length > 0 ? args[0] : "d:\\c_14-10-28\\test_data\\AFI_Matching_Jeff_V25.xml";
		String toFileStr   = args.length > 1 ? args[1] : "d:\\c_14-10-28\\test_data\\OKW_Matching_Empty.xml";
		Ct2Smart from = FileUtil.loadCt2Smart(new File(fromFileStr));
		Ct2Smart to = FileUtil.loadCt2Smart(new File(toFileStr));
		MappingsMerger merger = new MappingsMerger();
		merger.merge(from, to);
		FileUtil.write(new File("merged_mapping.xml"), to);
		System.out.println("Merge done");
	}
	
	public void merge(Ct2Smart from, Ct2Smart to) {
		Ct2SmartLookup lookup = new Ct2SmartLookup(from);
		for (Ct2Attribute aTo : to.getCt2Attribute()) {
			Ct2Attribute aFrom = lookup.findAttribute(aTo.getI());
			if (aFrom != null) {
				merge(aFrom, aTo);
			}
		}
	}
	
	public void merge(Ct2Attribute from, Ct2Attribute to) {
		//NOTE: N and I must be the same by design!!!
		to.setCategoryKey(from.getCategoryKey());
		to.setMapTo(from.getMapTo());
		to.setType(from.getType());
		
		to.getExtraAttribute().clear();
		to.getExtraAttribute().addAll(from.getExtraAttribute());
		
		Map<String, Ct2AttributeValue> vLookup = new HashMap<String, Ct2AttributeValue>();
		for (Ct2AttributeValue vFrom : from.getCt2AttributeValue()) {
			vLookup.put(vFrom.getI(), vFrom);
		}

		for (Ct2AttributeValue vTo : to.getCt2AttributeValue()) {
			Ct2AttributeValue vFrom = vLookup.get(vTo.getI());
			if (vFrom != null) {
				//NOTE: N and I must be the same by design!!!
				vTo.setMapTo(vFrom.getMapTo());
				vTo.setIgnore(vFrom.isIgnore());
			}
		}
	}
}
