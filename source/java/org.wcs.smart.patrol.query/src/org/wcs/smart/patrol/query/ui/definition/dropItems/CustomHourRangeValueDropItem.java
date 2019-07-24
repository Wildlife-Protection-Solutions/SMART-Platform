package org.wcs.smart.patrol.query.ui.definition.dropItems;

import java.util.List;
import java.util.Locale;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.wcs.smart.patrol.query.model.PatrolValueOption;
import org.wcs.smart.ui.TimePicker;

public class CustomHourRangeValueDropItem extends PatrolValueDropItem{

	private TimePicker sTime, eTime;
	private int[] initValues = null;
	
	public CustomHourRangeValueDropItem(PatrolValueOption item) {
		super(item);
	}

	
	@Override
	public String getValueQueryPart() {
		StringBuilder sb = new StringBuilder();
		sb.append("patrol:sum:"); //$NON-NLS-1$
		sb.append(item.getKeyPart());
		sb.append(":"); //$NON-NLS-1$
		sb.append(sTime.getTimeInSeconds());
		sb.append(":"); //$NON-NLS-1$
		sb.append(eTime.getTimeInSeconds());
		
		return sb.toString();
	}
	
	@Override
	protected void createValueComposite(Composite parent) {
		Composite part = new Composite(parent, SWT.NONE);
		part.setLayout(new GridLayout(3, false));
		((GridLayout)part.getLayout()).marginWidth = 0;
		((GridLayout)part.getLayout()).marginHeight = 0;
		((GridLayout)part.getLayout()).horizontalSpacing = 0;
		
		Label lbl = new Label(part, SWT.NONE);
		lbl.setText( formatStringForLabel(item.getGuiName(Locale.getDefault())));
		lbl.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 3, 1));
		
		sTime = new TimePicker(part, SWT.NONE);
		sTime.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		
		lbl = new Label(part, SWT.NONE);
		lbl.setText( " to " );
		
		eTime = new TimePicker(part, SWT.NONE);
		eTime.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		
		if (initValues != null) {
			sTime.setTimeInSeconds(initValues[0]);
			eTime.setTimeInSeconds(initValues[1]);
		}else {
			sTime.setTimeInSeconds(20*3600);
			eTime.setTimeInSeconds(6*3600);
		}

		sTime.addListener(e->queryChanged());
		eTime.addListener(e->queryChanged());
		
		initDrag(part);
	}
	
	/*
	 * Creates the ui element
	 */
	@Override
	protected void updateUi(){
		super.updateUi();
		
		main.layout();
		main.redraw();
	}
	
	/**
	 * Does nothing
	 * @see org.wcs.smart.query.ui.formulaDnd.AbstractValueDropItem#initializeValueData(java.lang.Object)
	 */
	@Override
	protected void initializeValueData(Object data) {
		if (data instanceof List<?>) {
			List<Integer> items = (List<Integer>)data;
			
			if (sTime == null) {
				initValues = new int[] {items.get(0), items.get(1)};
			}else {
				sTime.setTimeInSeconds(items.get(0));
				eTime.setTimeInSeconds(items.get(1));
			}
		}
	}
}
