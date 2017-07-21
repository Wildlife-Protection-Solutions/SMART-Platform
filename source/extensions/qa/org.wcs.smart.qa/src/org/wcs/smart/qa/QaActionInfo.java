package org.wcs.smart.qa;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;
import org.wcs.smart.qa.model.IQaAction;
import org.wcs.smart.qa.routine.IgnoreAction;

public class QaActionInfo {

	public final static QaActionInfo IGNORE_INSTANCE = new QaActionInfo(IgnoreAction.INSTANCE, null);
	
	private IQaAction action;
	private ImageDescriptor actionImage;
	
	public QaActionInfo(IQaAction action, ImageDescriptor actionImage) {
		this.action = action;
		this.actionImage = actionImage;
	}
	
	public IQaAction getAction() {
		return this.action;
	}
	
	public ImageDescriptor getImage() {
		return this.actionImage;
	}
}
