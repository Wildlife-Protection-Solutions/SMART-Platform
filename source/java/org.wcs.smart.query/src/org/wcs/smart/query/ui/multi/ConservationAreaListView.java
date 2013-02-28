package org.wcs.smart.query.ui.multi;

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

public class ConservationAreaListView extends ViewPart {

	public static final String ID = "org.wcs.smart.query.conservationAreaList";
	
	private final FormToolkit toolkit = new FormToolkit(Display.getCurrent());
	private Label lblDefault = null;
	private Font boldFont = null;
	public ConservationAreaListView() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public void createPartControl(Composite parent) {
		Composite main = toolkit.createComposite(parent);
		
		main.setLayout(new GridLayout(1, false));
		lblDefault = toolkit.createLabel(main, "Conservation Areas:", SWT.NONE);
		
		FontData fd = lblDefault.getFont().getFontData()[0];
		fd.setStyle(SWT.BOLD);
		boldFont = new Font(Display.getCurrent(), fd);
		
		lblDefault.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		((GridData)lblDefault.getLayoutData()).widthHint = 100;
		lblDefault.setFont(boldFont);
		lblDefault.addDisposeListener(new DisposeListener() {
			@Override
			public void widgetDisposed(DisposeEvent e) {
				if (boldFont != null){
					boldFont.dispose();
					boldFont = null;
				}
			}
		});
		if (SmartDB.getSelectedConservationAreas() != null){
			for(ConservationArea ca : SmartDB.getSelectedConservationAreas()){
				Label l = toolkit.createLabel(main, ca.getName() + " (" + ca.getId() + ")");
				l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
				((GridData)l.getLayoutData()).horizontalIndent = 10;
			}
		}
		
		
		Label ll = toolkit.createLabel(main, "The above is a list of conservation areas you can access in your queries. By default, query results will include each of these conservation areas.  Queries can be filtered futher by using the 'Conservation Area Filter' option in the query definition", SWT.WRAP);
		ll.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		((GridData)ll.getLayoutData()).widthHint = 100;
	}

	@Override
	public void setFocus() {
		lblDefault.setFocus();
	}

}
