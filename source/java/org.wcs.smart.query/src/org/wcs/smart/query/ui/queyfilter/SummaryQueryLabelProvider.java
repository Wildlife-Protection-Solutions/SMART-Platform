package org.wcs.smart.query.ui.queyfilter;

import org.eclipse.swt.graphics.Image;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.ca.datamodel.CategoryAttribute;
import org.wcs.smart.query.parser.internal.PatrolQueryOptions;
import org.wcs.smart.query.parser.internal.PatrolQueryOptions.DateGroupByOption;
import org.wcs.smart.query.parser.internal.PatrolQueryOptions.PatrolQueryOption;
import org.wcs.smart.query.parser.internal.PatrolQueryOptions.PatrolValueOption;

public class SummaryQueryLabelProvider extends QueryFilterLabelProvider {

	@Override
	public Image getImage(Object element) {
		if (element instanceof SummaryQueryContentProvider.RootNode) {
			return ((SummaryQueryContentProvider.RootNode) element).getImage();
		}else if (element instanceof DateGroupByOption){
			return PatrolQueryOptions.getImage((DateGroupByOption)element);
		}else if (element instanceof SummaryDmObject){
			return super.getImage(((SummaryDmObject) element).getObject());
		}
		return super.getImage(element);
	}

	@Override
	public String getText(Object element) {
		if (element instanceof PatrolValueOption) {
			return ((PatrolValueOption) element).getGuiName();
		} else if (element instanceof PatrolQueryOption) {
			return ((PatrolQueryOption) element).getGuiName();
		} else if (element instanceof SummaryQueryContentProvider.RootNode) {
			return ((SummaryQueryContentProvider.RootNode) element).getName();
		} else if (element instanceof DateGroupByOption) {
			return ((DateGroupByOption) element).getGuiName();
		} else if (element instanceof SummaryDmObject){
			SummaryDmObject obj = (SummaryDmObject)element;
			if (obj.isValue()){
				if (obj.getObject() instanceof Attribute){
					return ((Attribute)obj.getObject()).getName();
				}else if (obj.getObject() instanceof Category){
					return "Count '" + ((Category)obj.getObject()).getName() + "' ";
				}else if (obj.getObject() instanceof CategoryAttribute){
					return ((CategoryAttribute)obj.getObject()).getAttribute().getName();
				}
			}
		}
		return super.getText(element);

	}
}
