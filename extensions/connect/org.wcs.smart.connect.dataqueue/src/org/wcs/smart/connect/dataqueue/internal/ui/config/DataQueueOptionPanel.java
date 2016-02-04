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
package org.wcs.smart.connect.dataqueue.internal.ui.config;

import java.util.ArrayList;
import java.util.Collection;

import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.wcs.smart.connect.dataqueue.internal.process.AutoProcessingManager;
import org.wcs.smart.connect.dataqueue.model.DataQueueServerOptions;
import org.wcs.smart.connect.model.ConnectServer;
import org.wcs.smart.connect.ui.server.configure.IServerOptionsPanel;

/**
 * Composite that contains all the options
 * for automated data queue processing
 * 
 * @author Emily
 *
 */
public class DataQueueOptionPanel implements IServerOptionsPanel{

	private Collection<ModifyListener> listeners;
	
	private boolean isEditable = true;
	
	private Button btnCheckStartUp;
	private Button btnAutoCheck;
	
	private Button opStartUpAutoStart;
	private Button opStartUpPrompt;
	
	private Button opAutoAutoStart;
	private Button opAutoPrompt;
	private Button btnPromptUser;
	
	private Label lblMinutes, lblMinutes2;
	private Text txtMinutes;
	private ControlDecoration cdMinutes;
	
	public DataQueueOptionPanel() {
	}
	
	@Override
	public String getName(){
		return "Data Queue Options";
	}
	
	@Override
	public Composite createComposite(Composite parent, boolean isEditable){
		this.isEditable = isEditable;
		listeners = new ArrayList<ModifyListener>();
	
		Composite main = new Composite(parent, SWT.NONE);
		main.setLayout(new GridLayout());
		
		Group g1 = new Group(main, SWT.DEFAULT);
		g1.setText("Start Up Options");
		g1.setLayout(new GridLayout());
		g1.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		btnCheckStartUp = new Button(g1, SWT.CHECK);
		btnCheckStartUp.setText("Check for new items on start up");
		btnCheckStartUp.setToolTipText("communicates with connect on start up and checks for new items in the data queue.");

		Composite startupOp = new Composite(g1, SWT.NONE);
		GridLayout gl = new GridLayout();
		gl.marginHeight = 0;
		gl.marginWidth = 0;
		startupOp.setLayout(gl);
		startupOp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		((GridData)startupOp.getLayoutData()).horizontalIndent = 10;
		opStartUpAutoStart = new Button(startupOp, SWT.RADIO);
		opStartUpAutoStart.setText("Automatically start downloading and processing new files.");
		opStartUpAutoStart.setToolTipText("will automatically start downloading and processing any new files");
		
		opStartUpPrompt = new Button(startupOp, SWT.RADIO);
		opStartUpPrompt.setText("Prompt user to process new files");
		opStartUpPrompt.setToolTipText("will prompt the user if new files are found");
		
		Group g2 = new Group(main, SWT.DEFAULT);
		g2.setText("Automatic Processing");
		g2.setLayout(new GridLayout());
		g2.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		btnAutoCheck = new Button(g2, SWT.CHECK);
		btnAutoCheck.setText("Periodically check SMART Connect for new files");
		btnAutoCheck.setToolTipText("communitcates with connect at a fixed interval and checks for new files");
		
		Composite autoOp = new Composite(g2, SWT.NONE);
		gl = new GridLayout();
		gl.marginHeight = 0;
		gl.marginWidth = 0;
		autoOp.setLayout(gl);
		autoOp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		((GridData)autoOp.getLayoutData()).horizontalIndent = 10;
		
		Composite minComp = new Composite(autoOp, SWT.NONE);
		gl = new GridLayout(3, false);
		gl.marginWidth = 0;
		gl.marginHeight = 0;
		minComp.setLayout(gl);
		lblMinutes = new Label(minComp, SWT.NONE);
		lblMinutes.setText("Check for new files every");
		txtMinutes = new Text(minComp, SWT.BORDER);
		txtMinutes.setText("0000"); //$NON-NLS-1$
		txtMinutes.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		((GridData)txtMinutes.getLayoutData()).widthHint = 50;
		lblMinutes2 = new Label(minComp, SWT.NONE);
		lblMinutes2.setText("minutes");
		minComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		((GridData)minComp.getLayoutData()).horizontalIndent = 10;
		cdMinutes = createControlDecoration(txtMinutes);
		cdMinutes.setDescriptionText("Invalid minutes values");
		txtMinutes.addModifyListener(new ModifyListener(){
			@Override
			public void modifyText(ModifyEvent e) {
				try{
					int x = Integer.parseInt(txtMinutes.getText());
					if (x < 0){
						throw new Exception("Minutes must be a valid integer");
					}
					cdMinutes.hide();
				}catch (Exception ex){
					cdMinutes.show();
				}
				fireChange(e);
			}
			
		});
		
		opAutoAutoStart = new Button(autoOp, SWT.RADIO);
		opAutoAutoStart.setText("Automatically start downloading and processing new files.");
		opAutoAutoStart.setToolTipText("will automatically start downloading and processing any new files");
		
		opAutoPrompt = new Button(autoOp, SWT.RADIO);
		opAutoPrompt.setText("Prompt user to process new files");
		opAutoPrompt.setToolTipText("will prompt the user if new files are found");
		
		Group g3 = new Group(main, SWT.DEFAULT);
		g3.setText("General");
		g3.setLayout(new GridLayout());
		g3.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		btnPromptUser = new Button(g3, SWT.CHECK);
		btnPromptUser.setText("Prompt for SMART Connect username/password if credentials not saved");
		btnPromptUser.setToolTipText("if not selected and credentials are not saved all processing will be cancelled");
		
		if (isEditable){
			SelectionListener ml = new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					updateEnabled();
					fireChange(null);
				}
			};
			btnAutoCheck.addSelectionListener(ml);
			btnCheckStartUp.addSelectionListener(ml);
			
			ml = new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					fireChange(null);
				}
			};
			opAutoAutoStart.addSelectionListener(ml);
			opAutoPrompt.addSelectionListener(ml);
			opStartUpAutoStart.addSelectionListener(ml);
			opStartUpPrompt.addSelectionListener(ml);
			btnPromptUser.addSelectionListener(ml);
			
		}else{
			btnCheckStartUp.setEnabled(false);
			btnAutoCheck.setEnabled(false);
			txtMinutes.setEnabled(false);
			lblMinutes.setEnabled(false);
			lblMinutes2.setEnabled(false);
			opAutoAutoStart.setEnabled(false);
			opAutoPrompt.setEnabled(false);
			opStartUpAutoStart.setEnabled(false);
			opStartUpPrompt.setEnabled(false);
			btnPromptUser.setEnabled(false);
		}
		return main;
	}
	
	public boolean isValid(){
		return !cdMinutes.isVisible();
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
	
	protected void updateEnabled(){
		if (!isEditable) return;
		
		boolean enabled = btnAutoCheck.getSelection();
		
		lblMinutes.setEnabled(enabled);
		txtMinutes.setEnabled(enabled);
		lblMinutes2.setEnabled(enabled);
		opAutoAutoStart.setEnabled(enabled);
		opAutoPrompt.setEnabled(enabled);
		
		enabled = btnCheckStartUp.getSelection();
		opStartUpAutoStart.setEnabled(enabled);
		opStartUpPrompt.setEnabled(enabled);
		
		btnPromptUser.setEnabled(true);
	}
	
	@Override
	public void initValues(ConnectServer server){
		if (server == null){
			btnCheckStartUp.setSelection(DataQueueServerOptions.CHECK_ONSTARTUP.getDefaultValueAsBoolean());
			opStartUpAutoStart.setSelection(DataQueueServerOptions.STARTUP_AUTOPROCESS.getDefaultValueAsBoolean());
			opStartUpPrompt.setSelection(DataQueueServerOptions.STARTUP_PROMPT.getDefaultValueAsBoolean());
			
			btnAutoCheck.setSelection(DataQueueServerOptions.AUTO_CHECK.getDefaultValueAsBoolean());
			opAutoAutoStart.setSelection(DataQueueServerOptions.AUTO_AUTOPROCESS.getDefaultValueAsBoolean());
			opAutoPrompt.setSelection(DataQueueServerOptions.AUTO_PROMPT.getDefaultValueAsBoolean());
			btnPromptUser.setSelection(DataQueueServerOptions.PROMPT_USER.getDefaultValueAsBoolean());
			
			txtMinutes.setText(DataQueueServerOptions.AUTO_MINUTES.getDefaultValueAsString());

			updateEnabled();
			return;
		}
		btnCheckStartUp.setSelection(DataQueueServerOptions.CHECK_ONSTARTUP.getBooleanValue(server));
		opStartUpAutoStart.setSelection(DataQueueServerOptions.STARTUP_AUTOPROCESS.getBooleanValue(server));
		opStartUpPrompt.setSelection(DataQueueServerOptions.STARTUP_PROMPT.getBooleanValue(server));
		
		btnAutoCheck.setSelection(DataQueueServerOptions.AUTO_CHECK.getBooleanValue(server));
		opAutoAutoStart.setSelection(DataQueueServerOptions.AUTO_AUTOPROCESS.getBooleanValue(server));
		opAutoPrompt.setSelection(DataQueueServerOptions.AUTO_PROMPT.getBooleanValue(server));
		btnPromptUser.setSelection(DataQueueServerOptions.PROMPT_USER.getBooleanValue(server));
		
		txtMinutes.setText(String.valueOf(DataQueueServerOptions.AUTO_MINUTES.getIntegerValue(server)));
		
		updateEnabled();
	}
	
	@Override
	public void updateServer(ConnectServer server){
		server.setOption(DataQueueServerOptions.CHECK_ONSTARTUP.name(), ((Boolean)btnCheckStartUp.getSelection()).toString());
		server.setOption(DataQueueServerOptions.STARTUP_AUTOPROCESS.name(), ((Boolean)opStartUpAutoStart.getSelection()).toString());
		server.setOption(DataQueueServerOptions.STARTUP_PROMPT.name(), ((Boolean)opStartUpPrompt.getSelection()).toString());
		server.setOption(DataQueueServerOptions.AUTO_CHECK.name(), ((Boolean)btnAutoCheck.getSelection()).toString());
		server.setOption(DataQueueServerOptions.AUTO_AUTOPROCESS.name(), ((Boolean)opAutoAutoStart.getSelection()).toString());
		server.setOption(DataQueueServerOptions.AUTO_PROMPT.name(), ((Boolean)opAutoPrompt.getSelection()).toString());
		
		
		String minutes = DataQueueServerOptions.AUTO_MINUTES.getDefaultValueAsString();
		try{
			int tmp = Integer.parseInt(txtMinutes.getText());
			if (tmp >= 0){
				minutes = String.valueOf(tmp);
			}
		}catch(Exception ex){
			
		}
		server.setOption(DataQueueServerOptions.AUTO_MINUTES.name(), minutes);
	}

	@Override
	public void afterSave(ConnectServer server){
		if (DataQueueServerOptions.AUTO_CHECK.getBooleanValue(server)){
			AutoProcessingManager.INSTANCE.enableAutoProcessing(DataQueueServerOptions.AUTO_MINUTES.getIntegerValue(server));
		}
	}
}

