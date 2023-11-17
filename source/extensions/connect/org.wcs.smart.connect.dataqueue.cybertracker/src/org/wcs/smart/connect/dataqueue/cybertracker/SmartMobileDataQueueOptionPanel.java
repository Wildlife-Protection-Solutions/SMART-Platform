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
package org.wcs.smart.connect.dataqueue.cybertracker;

import java.util.ArrayList;
import java.util.Collection;

import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.hibernate.Session;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.ConservationAreaProperty;
import org.wcs.smart.common.control.SmartUiUtils;
import org.wcs.smart.connect.dataqueue.cybertracker.internal.Messages;
import org.wcs.smart.connect.model.ConnectServer;
import org.wcs.smart.connect.ui.server.configure.IServerOptionsPanel;

/**
 * Composite that contains all the options
 * for automated data queue processing
 * 
 * @author Emily
 *
 */
public class SmartMobileDataQueueOptionPanel implements IServerOptionsPanel{

	private Collection<ModifyListener> listeners;
	
	private Button btnEnableDesktopProcessing;
	
	public SmartMobileDataQueueOptionPanel() {
	}
	
	@Override
	public String getName(){
		return Messages.SmartMobileDataQueueOptionPanel_Name;
	}
	
	@Override
	public String getDescription(){
		return Messages.SmartMobileDataQueueOptionPanel_Description;
	}
	
	@Override
	public Composite createComposite(Composite parent, boolean isEditable){
		
		listeners = new ArrayList<ModifyListener>();
	
		Composite main = new Composite(parent, SWT.NONE);
		main.setLayout(new GridLayout());

		Label l = new Label(main, SWT.WRAP);
		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, false);
		gd.widthHint = 100;
		l.setLayoutData(gd);
		l.setText(Messages.SmartMobileDataQueueOptionPanel_SettingsInfo);
		
		SmartUiUtils.createSubHeaderLabel(main, Messages.SmartMobileDataQueueOptionPanel_OptionsSection);
		
		Composite s1 = new Composite(main, SWT.NONE);
		s1.setLayout(new GridLayout());
		s1.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		btnEnableDesktopProcessing = new Button(s1, SWT.CHECK);
		btnEnableDesktopProcessing.setText(Messages.SmartMobileDataQueueOptionPanel_ProcessingOp);
		btnEnableDesktopProcessing.setToolTipText(Messages.SmartMobileDataQueueOptionPanel_ProcessingOpTooltip);

		if (isEditable){
			Listener ml = e->fireChange(null);
			btnEnableDesktopProcessing.addListener(SWT.Selection, ml);			
		}else{
			btnEnableDesktopProcessing.setEnabled(false);		
		}
		return main;
	}
	
	public boolean isValid(){
		return true;
	}
	
	private void fireChange(ModifyEvent e){
		for(ModifyListener m : listeners){
			m.modifyText(e);
		}
	}
	@Override
	public void addChangeListener(ModifyListener listener){
		listeners.add(listener);
	}
	
	protected ControlDecoration createControlDecoration(Control widget){
		ControlDecoration cd = new ControlDecoration(widget, SWT.LEFT | SWT.TOP);
		cd.setImage(FieldDecorationRegistry.getDefault()
				.getFieldDecoration(FieldDecorationRegistry.DEC_ERROR).getImage());
		cd.setShowHover(true);
		return cd;
	}
	
	@Override
	public void initValues(ConnectServer server, Session session){
		
		if (server == null){
			btnEnableDesktopProcessing.setSelection(false);
			return;
		}
		
		ConservationAreaProperty prop = MobileProcessingEngine.INSTANCE.getOption(MobileProcessingEngine.Option.SMART_MOBILE_DESKTOP_PROCESSING, session);
		if (prop == null) {
			btnEnableDesktopProcessing.setSelection(false);
		}else {			
			btnEnableDesktopProcessing.setSelection(Boolean.parseBoolean(prop.getValue()));
		}
		
	}
	
	@Override
	public void updateServer(ConnectServer server, Session session){
		ConservationAreaProperty prop = MobileProcessingEngine.INSTANCE.getOption(MobileProcessingEngine.Option.SMART_MOBILE_DESKTOP_PROCESSING, session); 
		if (prop == null) {
			prop = MobileProcessingEngine.INSTANCE.createOption(MobileProcessingEngine.Option.SMART_MOBILE_DESKTOP_PROCESSING, session);
		}
		if (btnEnableDesktopProcessing.getSelection()) {
			prop.setValue(Boolean.TRUE.toString());
		}else {
			prop.setValue(Boolean.FALSE.toString());
		}		
		
		MobileProcessingEngine.INSTANCE.clearCachedValues();
	}

	@Override
	public void afterSave(ConnectServer server){
		
	}
	
	@Override
	public boolean isSupported(ConservationArea ca) {
		return !ca.getIsCcaa();
	}
}

