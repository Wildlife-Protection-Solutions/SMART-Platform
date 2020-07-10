package org.wcs.smart.patrol;

import java.text.Collator;
import java.util.Comparator;

import org.wcs.smart.patrol.model.PatrolLeg;

public class PatrolLegStartDateComparator implements Comparator<PatrolLeg> {
	
	public int compare(PatrolLeg o1, PatrolLeg o2) {
		int x =  o1.getStartDate().compareTo(o2.getStartDate());
		if (x == 0){
			return Collator.getInstance().compare(o1.getId(), o2.getId());
		}
		return x;
	}
	
}