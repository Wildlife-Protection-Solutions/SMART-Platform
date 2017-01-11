package org.wcs.smart.i2.ui.editors.query;

import java.util.Collections;
import java.util.List;

import org.eclipse.swt.graphics.Image;
import org.wcs.smart.i2.model.IntelRecordObservationQuery;
import org.wcs.smart.i2.query.IResultItem;

public interface IQuerySourceFinder {

	public void openSource(IResultItem item);
	
	/**
	 * Optional; can return null;
	 * @return
	 */
	public Image getImage();
	
	/**
	 * required
	 * @return
	 */
	public String getName();
	
	public static List<IQuerySourceFinder> getQuerySources(IntelRecordObservationQuery query){
		if (query.getClass().equals(IntelRecordObservationQuery.class)){
			return Collections.singletonList(ObservationQuerySourceFinder.INSTANCE);
		}
		return Collections.emptyList();
	}
}
