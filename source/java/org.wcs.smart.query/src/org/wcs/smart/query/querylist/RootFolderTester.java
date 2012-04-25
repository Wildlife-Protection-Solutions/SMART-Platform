package org.wcs.smart.query.querylist;

import org.eclipse.core.expressions.PropertyTester;
import org.wcs.smart.query.model.QueryFolder;

public class RootFolderTester  extends PropertyTester {

	public RootFolderTester() {
	}

	@Override
	public boolean test(Object receiver, String property, Object[] args,
			Object expectedValue) {
		if (receiver instanceof QueryFolder){
			return ((QueryFolder) receiver).isRootFolder();
		}
		return false;
	}

}

