package org.wcs.smart;

import java.util.Arrays;

import org.eclipse.core.expressions.PropertyTester;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.hibernate.SmartDB;

public class CaPropertyTester extends PropertyTester {

	/**
	 * Tests the current conservation area to determine if
	 * it is a single CA or the default CrossCa identifiers
	 * 
	 * @return <code>true</code> if ca references a single ca, 
	 * <code>false</code> if references "general" ca
	 */
	@Override
	public boolean test(Object receiver, String property, Object[] args,
			Object expectedValue) {
		return !SmartDB.isMultipleAnalysis();
	}

}
