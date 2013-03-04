package org.wcs.smart.ui;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.part.ViewPart;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.hibernate.SmartDB;

public class CrossCaView extends ViewPart {

	public static final String ID = "org.wcs.smart.crossCaView"; //$NON-NLS-1$
	
	private final FormToolkit toolkit = new FormToolkit(Display.getCurrent());
	private Label lblHeader = null;
	private Font boldFont = null;
	public CrossCaView() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public void createPartControl(Composite parent) {
		Composite main = toolkit.createComposite(parent);
		
		main.setLayout(new GridLayout(1, false));
		lblHeader = toolkit.createLabel(main, "Cross Conservation Area Analysis", SWT.NONE);
		
		FontData fd = lblHeader.getFont().getFontData()[0];
		fd.setStyle(SWT.BOLD);
		boldFont = new Font(Display.getCurrent(), fd);
		lblHeader.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		((GridData)lblHeader.getLayoutData()).widthHint = 100;
		lblHeader.setFont(boldFont);
		lblHeader.addDisposeListener(new DisposeListener() {
			@Override
			public void widgetDisposed(DisposeEvent e) {
				if (boldFont != null){
					boldFont.dispose();
					boldFont = null;
				}
			}
		});
		
		Label lblInfo = toolkit.createLabel(main, "Conservation areas for analysis:", SWT.NONE);
		
		
		if (SmartDB.getConservationAreaConfiguration() != null){
			for(ConservationArea ca : SmartDB.getConservationAreaConfiguration().getConservationAreas()){
				Label l = toolkit.createLabel(main, ca.getNameLabel());
				l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
				((GridData)l.getLayoutData()).horizontalIndent = 10;
			}
		}
		
		
		Label ll = toolkit.createLabel(main, "The above is a list of conservation areas you can access in any cross conservation analysis.", SWT.WRAP);
		ll.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		((GridData)ll.getLayoutData()).widthHint = 100;
	}

	@Override
	public void setFocus() {
		lblHeader.setFocus();
	}

}
