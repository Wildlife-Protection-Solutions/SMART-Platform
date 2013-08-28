package org.wcs.smart.upgrade.v112;

import java.util.ArrayList;

public class FixDataModelTest {

	
	
	public static void main(String args[]) throws Exception{
		
		
		KeyItem ki1 = new KeyItem(null, "threat", null);
		KeyItem ki2 = new KeyItem(null, "556", null);
		KeyItem ki3 = new KeyItem(null, "threat.species", null);
		KeyItem ki4 = new KeyItem(null, "a556", null);
		KeyItem ki5 = new KeyItem(null, "threat_species", null);
		KeyItem ki6 = new KeyItem(null, "b556", null);
		KeyItem ki7 = new KeyItem(null, "and", null);
		KeyItem ki8 = new KeyItem(null, "or", null);
		KeyItem ki9 = new KeyItem(null, "aor", null);
		
		ArrayList<KeyItem> items = new ArrayList<KeyItem>();
		items.add(ki1);
		items.add(ki2);
		items.add(ki3);
		items.add(ki7);
		items.add(ki8);
		
		
		items.remove(ki1);
		if (!FixDataModel.validateKey(ki1.getOriginalKey(), items)){
			System.out.println("Error 1");
		}
		items.add(ki1);
		
		items.remove(ki2);
		if (FixDataModel.validateKey(ki2.getOriginalKey(), items)){
			System.out.println("Error 2");
		}
		String fixed = FixDataModel.fixKey(ki2.getOriginalKey(), items);
		System.out.println(fixed);
		if (!fixed.equals("a556")){
			System.out.println("Error 2A");
		}
		items.add(ki2);
		
		items.remove(ki3);
		if (FixDataModel.validateKey(ki3.getOriginalKey(), items)){
			System.out.println("Error 3");
		}
		fixed = FixDataModel.fixKey(ki3.getOriginalKey(), items);
		System.out.println(fixed);
		if (!fixed.equals("threat_species")){
			System.out.println("Error 3A");
		}
		
		items.add(ki3);
		
		
		
		items.add(ki4);
		items.add(ki5);
		items.add(ki6);
		
		items.remove(ki3);
		if (FixDataModel.validateKey(ki3.getOriginalKey(), items)){
			System.out.println("Error 4");
		}
		fixed = FixDataModel.fixKey(ki3.getOriginalKey(), items);
		System.out.println(fixed);
		if (!fixed.equals("athreat_species")){
			System.out.println("Error 4A");
		}
		items.add(ki3);
		
		items.remove(ki2);
		if (FixDataModel.validateKey(ki2.getOriginalKey(), items)){
			System.out.println("Error 5");
		}
		fixed = FixDataModel.fixKey(ki2.getOriginalKey(), items);
		System.out.println(fixed);
		if (!fixed.equals("c556")){
			System.out.println("Error 5A");
		}
		items.add(ki2);
		
		items.remove(ki7);
		if (FixDataModel.validateKey(ki7.getOriginalKey(), items)){
			System.out.println("Error 7");
		}
		fixed = FixDataModel.fixKey(ki7.getOriginalKey(), items);
		System.out.println(fixed);
		if (!fixed.equals("aand")){
			System.out.println("Error 7A");
		}
		items.add(ki7);
		
		items.remove(ki8);
		if (FixDataModel.validateKey(ki8.getOriginalKey(), items)){
			System.out.println("Error 8");
		}
		fixed = FixDataModel.fixKey(ki8.getOriginalKey(), items);
		System.out.println(fixed);
		if (!fixed.equals("aor")){
			System.out.println("Error 8A");
		}
		items.add(ki8);
		
		items.add(ki9);
		items.remove(ki8);
		if (FixDataModel.validateKey(ki8.getOriginalKey(), items)){
			System.out.println("Error 9");
		}
		fixed = FixDataModel.fixKey(ki8.getOriginalKey(), items);
		System.out.println(fixed);
		if (!fixed.equals("bor")){
			System.out.println("Error 9A");
		}
		items.add(ki8);
		
		
		
		
		if (FixDataModel.validateName("Mittendorf’s")){
			System.out.println("Error 20");
		}
		fixed = FixDataModel.fixName("Mittendorf’s");
		System.out.println(fixed);
		if (!fixed.equals("Mittendorf�s")){
			System.out.println("Error 20A");
		}
		
	}
}
