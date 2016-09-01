package org.wcs.smart.i2.ui.views;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;

import org.eclipse.e4.tools.compat.parts.DIViewPart;
import org.eclipse.e4.ui.di.Focus;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

public class RecordsView {

	public static final String ID = "org.wcs.smart.i2.ui.view.records";
	
	@Inject
	private EPartService partService;

	public RecordsView() {
		super();
	}

	@PostConstruct
	public void createPartControl(Composite parent) {
		parent.setLayout(new GridLayout());
		
		Label l = new Label(parent, SWT.NONE);
		l.setText("Intelligence Records");
	}

	// @Optional
	// @Inject
	// private void
	// dbModified(@EventTopic(SmartPlugIn.E4_DATABASE_CHANGED_EVENT) Object
	// data){
	// }

	@Focus
	public void setFocus() {
	}

	@PreDestroy
	public void dispose() {
	}
	
	public static class RecordsViewWrapper extends DIViewPart<RecordsView>{
		public RecordsViewWrapper() {
			super(RecordsView.class);
		}
	}

}