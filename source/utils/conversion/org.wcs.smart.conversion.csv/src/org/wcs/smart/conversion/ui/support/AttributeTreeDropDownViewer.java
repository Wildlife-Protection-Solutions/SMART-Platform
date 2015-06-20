package org.wcs.smart.conversion.ui.support;

import org.eclipse.swt.widgets.Shell;
import org.wcs.smart.internal.ca.datamodel.xml.generate.AttributeType;

public class AttributeTreeDropDownViewer extends TreeDropDownViewer {

	public AttributeTreeDropDownViewer(Shell parent, AttributeTreeContentProvider contentProvider, LangColumnLabelProvider labelProvider) {
		super(parent, contentProvider, labelProvider);
	}

	public void setInput(AttributeType attribute) {
		getDmTreeViewer().setInput(attribute.getTrees());
		getDmTreeViewer().expandToLevel(1);
		getDmTreeViewer().refresh();
	}
	
}
