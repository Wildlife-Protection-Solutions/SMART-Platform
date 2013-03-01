package org.wcs.smart.query.ui.definition;

import java.util.Arrays;

import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.query.internal.Messages;
import org.wcs.smart.query.model.Query;
import org.wcs.smart.query.parser.filter.ConservationAreaFilter;

public class ConservationAreaFilterPanel extends Composite implements SelectionListener{
	
	public static final String PANEL_TITLE = Messages.ConservationAreaFilterPanel_CaFilterPanelTitle;
	
	private SelectionListener selectionListener;
	
	public ConservationAreaFilterPanel(Composite parent){
		super(parent, SWT.NONE);
	}

	/**
	 * Sets the selection listener.  Only one selection listener
	 * can be registered at a time. 
	 * 
	 * @param selectionListener
	 */
	public void setSelectionListener(SelectionListener selectionListener){
		this.selectionListener = selectionListener;
	}
	
	public void initQuery(Query query){
		for (Control c : getChildren()){
			c.dispose();
		}
		
		setLayout(new GridLayout());
		for (ConservationArea ca : SmartDB.getConservationAreaConfiguration().getConservationAreas()){
			Button btnCa = new Button(this, SWT.CHECK);
			btnCa.setText(ca.getNameLabel());
			btnCa.setData(ca);
			btnCa.setSelection(false);
			for (byte[] sca : query.getConservationAreaFilterAsFilter().getConservationAreaFilterIds()){
				if (Arrays.equals(sca, ca.getUuid())){
					btnCa.setSelection(true);
				}
			}
			btnCa.addSelectionListener(this);
		}
		layout(true, true);
	}
	
	public ConservationAreaFilter getCaFilter(){
		if (isValid() != null) return null;
		ConservationAreaFilter filter = new ConservationAreaFilter();
		for (Control c : getChildren()){
			if (c instanceof Button && c.getData() instanceof ConservationArea && ((Button)c).getSelection()){
				filter.addConservationArea((ConservationArea)c.getData());
			}
		}
		return filter;
	}
	
	/**
	 * 
	 * @return <code>null</code> if filter is valid otherwise error message
	 */
	public String isValid(){
		for (Control c : getChildren()){
			if (c instanceof Button && c.getData() instanceof ConservationArea && ((Button)c).getSelection()){
				//at least one ca is selected which is all we care about
				return null;
			}
		}
		return Messages.ConservationAreaFilterPanel_Error_NoCas;
	}

	@Override
	public void widgetSelected(SelectionEvent e) {
		if (selectionListener != null){
			selectionListener.widgetSelected(e);
		}
	}

	@Override
	public void widgetDefaultSelected(SelectionEvent e) {
		
	}
}
