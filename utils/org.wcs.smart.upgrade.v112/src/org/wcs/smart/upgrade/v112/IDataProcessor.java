package org.wcs.smart.upgrade.v112;

import org.wcs.smart.query.parser.filter.IFilter;
import org.wcs.smart.query.parser.internal.summary.IGroupBy;
import org.wcs.smart.query.parser.internal.summary.IValueItem;

public interface IDataProcessor {

	boolean processValueItem(IValueItem item, String oldKey, String newKey);
	
	boolean processFilter(IFilter filter, String oldKey, String newKey);
	
	boolean processGroupBy(IGroupBy filter, String oldKey, String newKey);
}
