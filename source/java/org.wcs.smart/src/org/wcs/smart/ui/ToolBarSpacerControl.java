package org.wcs.smart.ui;

import javax.annotation.PostConstruct;

import org.eclipse.e4.ui.model.application.ui.menu.MToolControl;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;

public class ToolBarSpacerControl {
	
	@PostConstruct
	void createWidget(Composite parent, MToolControl tc) {
		Composite comp = new Composite(parent, SWT.NONE) {
			@Override
			public Point computeSize(int wHint, int hHint, boolean flushCache) {
				return new Point(0, 0);
			}
		};
		comp.setSize(0, 0);
	}
}