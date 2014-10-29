package org.wcs.smart.er.query.engine;

import org.wcs.smart.er.query.filter.SamplingUnitAttributeFilter;
import org.wcs.smart.er.query.filter.SamplingUnitFilter;
import org.wcs.smart.er.query.filter.SamplingUnitFilter.Source;
import org.wcs.smart.query.model.filter.IFilter;
import org.wcs.smart.query.model.filter.IFilterVisitor;

/**
 * Has track filter visitor.
 * 
 * @author Emily
 *
 */
public class HasTrackFilterVisitor implements IFilterVisitor{
	boolean hasTrack = false;

	@Override
	public void visit(IFilter filter) {
		if (hasTrack)
			return;
		if (filter instanceof SamplingUnitFilter) {
			if (((SamplingUnitFilter) filter).getSource() == Source.TRACK) {
				hasTrack = true;
			}
		} else if (filter instanceof SamplingUnitAttributeFilter) {
			if (((SamplingUnitAttributeFilter) filter).getSource() == Source.TRACK) {
				hasTrack = true;
			}
		}
	}
	
	public boolean hasTrack(){
		return this.hasTrack;
	}
}
