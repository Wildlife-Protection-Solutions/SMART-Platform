package org.wcs.smart.i2.ui.views.query.dropitem;

import java.util.Locale;

import org.eclipse.jface.viewers.LabelProvider;
import org.wcs.smart.i2.query.Operator;

public class OperatorLabelProvider extends LabelProvider{
	@Override
	public String getText(Object element){
		if (element instanceof Operator){
			return ((Operator) element).getLabel(Locale.getDefault());
		}
		return super.getText(element);
	}

}
