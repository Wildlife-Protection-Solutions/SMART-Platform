/*
 * Copyright (C) 2012 Wildlife Conservation Society
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Software, and to permit persons to whom the Software is furnished to do
 * so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.wcs.smart.connect.dataqueue.patrol.ui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.hibernate.Session;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.connect.dataqueue.model.DataQueueProcessingOption;
import org.wcs.smart.connect.dataqueue.patrol.PatrolDataQueueProcessorOption;
import org.wcs.smart.connect.dataqueue.patrol.internal.Messages;
import org.wcs.smart.connect.dataqueue.ui.IProcessingOptionPanel;
import org.wcs.smart.hibernate.SmartDB;

/**
 * Option panel for patrol processing options.
 * 
 * @author Emily
 *
 */
public class PatrolProcessorOptionPanel implements IProcessingOptionPanel {

	private Button btnIds;
	
	private List<IProcessingOptionPanel.ModifyListener> listeners = new ArrayList<ModifyListener>();
	
	public PatrolProcessorOptionPanel() {	
	}
	
	@Override
	public String getName() {
		return Messages.PatrolProcessorOptionPanel_OptionPanel;
	}

	@Override
	public void initValues(HashMap<String, DataQueueProcessingOption> options) {
		btnIds.setSelection(PatrolDataQueueProcessorOption.PATROL_GENERATE_IDS.getValueAsBoolean(options));
	}

	@Override
	public void update(Session session) {
		DataQueueProcessingOption.DataQueueProcessingOptionPk pk = new DataQueueProcessingOption.DataQueueProcessingOptionPk();
		pk.setConservationArea(SmartDB.getCurrentConservationArea().getUuid());
		pk.setOptionKey(((PatrolDataQueueProcessorOption)btnIds.getData()).name());
		
		DataQueueProcessingOption op = (DataQueueProcessingOption) session.get(DataQueueProcessingOption.class, pk);
		if (op == null){
			op = new DataQueueProcessingOption();
			op.setConservationArea(SmartDB.getCurrentConservationArea().getUuid());
			op.setOptionKey(((PatrolDataQueueProcessorOption)btnIds.getData()).name());
		}
		op.setValue(String.valueOf(btnIds.getSelection()));
		session.saveOrUpdate(op);
		
	}

	@Override
	public boolean isValid() {
		return true;
	}

	@Override
	public Composite createComposite(Composite parent) {
		Composite main = new Composite(parent, SWT.NONE);
		main.setLayout(new GridLayout());
		
		Composite warn = new Composite(main, SWT.NONE);
		warn.setLayout(new GridLayout(2, false));
		warn.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		Label lwarn = new Label(warn, SWT.NONE);
		lwarn.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.WARN_ICON));
		lwarn.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		
		Label l = new Label(warn, SWT.WRAP);
		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, false);
		gd.widthHint = 100;
		l.setLayoutData(gd);
		l.setText(DESKTOP_ONLY_MESSAGE);
		
		btnIds = new Button(main, SWT.CHECK);
		btnIds.setText(Messages.PatrolProcessorOptionPanel_PidOptionLabel);
		btnIds.setToolTipText(Messages.PatrolProcessorOptionPanel_PidOptionTooltip);
		btnIds.setData(PatrolDataQueueProcessorOption.PATROL_GENERATE_IDS);
		
		SelectionListener listener= new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				for (ModifyListener l : listeners){
					l.widgetChanged();
				}
			}
		};
		btnIds.addSelectionListener(listener);
		return main;
	}

	@Override
	public void addChangeListener(ModifyListener listener) {
		listeners.add(listener);
	}

}
