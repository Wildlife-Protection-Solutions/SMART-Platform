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
package org.wcs.smart.connect.ui.server.configure;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.wcs.smart.connect.internal.Messages;
import org.wcs.smart.connect.model.ConnectServer;
import org.wcs.smart.connect.model.ConnectServerOption;

/**
 * Composite for inputting server options.
 * 
 * @author Emily
 *
 */
public class ServerOptionsPanel extends Composite {

	private static ConnectServerOption.Option[] OPTION_KEYS = new ConnectServerOption.Option[]{
		ConnectServerOption.Option.MAX_PROCESSING_WAIT_TIME,
		ConnectServerOption.Option.MAX_RETRY_DOWNLOAD,
		ConnectServerOption.Option.MAX_RETRY_UPLOAD,
		ConnectServerOption.Option.RETY_WAIT_TIME,
	};
	
	private static final String CD_KEY = "cd"; //$NON-NLS-1$
	private static final String VALID_KEY = "valid"; //$NON-NLS-1$
	
	private HashMap<ConnectServerOption.Option, Text> optionCntrls;
	private Collection<ModifyListener> listeners;
	
	private boolean isEditable = true;
	
	public ServerOptionsPanel(Composite parent) {
		this(parent, true);
	}
	
	public ServerOptionsPanel(Composite parent, boolean isEditable) {
		super(parent, SWT.NONE);
		this.isEditable = isEditable;
		listeners = new ArrayList<ModifyListener>();
		createControl();
	}
	
	private void createControl(){
		setLayout(new GridLayout(2, false));
		
		ModifyListener ml = new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				Text txt = (Text)e.getSource();
				try{
					Long.parseLong( txt.getText());
					txt.setData(VALID_KEY, true);
					((ControlDecoration)txt.getData(CD_KEY)).hide();
				}catch (Exception ex){
					//invalid number
					txt.setData(VALID_KEY, false);
					((ControlDecoration)txt.getData(CD_KEY)).show();
					((ControlDecoration)txt.getData(CD_KEY)).setDescriptionText(Messages.ServerOptionsPanel_InvalidNumber);
				}
				fireChange(e);
			}
		};
		optionCntrls = new HashMap<ConnectServerOption.Option, Text>();
		for (ConnectServerOption.Option op : OPTION_KEYS){
			Label l = new Label(this, SWT.NONE);
			l.setText(ServerOptionLabelProvider.INSTANCE.getOptionLabel(op) +":"); //$NON-NLS-1$
			l.setToolTipText(ServerOptionLabelProvider.INSTANCE.getOptionTooltip(op));
			
			Text txt = new Text(this, SWT.BORDER);
			txt.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
			((GridData)txt.getLayoutData()).widthHint = 60;
			txt.setData(VALID_KEY, false);
			
			if (!isEditable){
				txt.setEnabled(false);
			}
			ControlDecoration cd = createControlDecoration(txt);
			txt.setData(CD_KEY, cd);
			
			txt.addModifyListener(ml);
			optionCntrls.put(op, txt);
		}
	}
	
	protected ControlDecoration createControlDecoration(Control widget){
		ControlDecoration cd = new ControlDecoration(widget, SWT.LEFT | SWT.TOP);
		cd.setImage(FieldDecorationRegistry.getDefault()
				.getFieldDecoration(FieldDecorationRegistry.DEC_ERROR).getImage());
		cd.setShowHover(true);
		return cd;
	}
	
	public boolean isValid(){
		for (Text t : optionCntrls.values()){
			if (!((boolean)t.getData(VALID_KEY))){
				return false;
			}
		}
		return true;
	}
	
	private void fireChange(ModifyEvent e){
		for(ModifyListener m : listeners){
			m.modifyText(e);
		}
	}
	public void addChangeListener(ModifyListener listener){
		listeners.add(listener);
	}
	
	public void initValues(ConnectServer server){
		if (server == null){
			for (Text l : optionCntrls.values()){
				l.setText(Messages.ServerOptionsPanel_NotApplicable);
				((ControlDecoration)l.getData(CD_KEY)).hide();	
			}
			return;
		}
		for (ConnectServerOption.Option op : OPTION_KEYS){
			String value = ServerOptionLabelProvider.INSTANCE.getValueInDisplayUnits(op, server);
			optionCntrls.get(op).setText(value);
		}
	}
	
	public void updateServer(ConnectServer server){
		for (ConnectServerOption.Option op : OPTION_KEYS){
			Text ctr = optionCntrls.get(op);
			
			Long l = Long.parseLong(ctr.getText());
			if (op == ConnectServerOption.Option.MAX_PROCESSING_WAIT_TIME){
				//convert seconds to milliseconds
				l = l * 1000;
			}
			server.setOption(op, String.valueOf(l));
		}
	}

}
