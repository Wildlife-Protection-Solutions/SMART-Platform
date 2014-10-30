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
package org.wcs.smart.splashHandlers;


import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.application.DisplayAccess;
import org.eclipse.ui.splash.AbstractSplashHandler;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.internal.Messages;
import org.wcs.smart.startup.SmartStartUp;
import org.wcs.smart.ui.ConservationAreaLabelProvider;
import org.wcs.smart.ui.internal.startup.InitializeDialog;
import org.wcs.smart.ui.internal.startup.StartUpAdvancedDialog;
import org.wcs.smart.ui.internal.startup.StartUpDialog;

/**
 * 
 * Smart Splash Screen internative handler.  
 * 
 * @since 3.3
 * 
 */
public class InteractiveSplashHandler extends AbstractSplashHandler {
	
	private final static int F_COLUMN_COUNT = 2;
	
	private boolean fAuthenticated = false;

	/* ui components */
	private Shell parent = null;

	private Composite fCompositeLogin = null;
	
	private Text txtUserName = null;
	private Text txtPassword  = null;
	
	private Button btnOk  = null;
	private Button btnCancel  = null;

	private ComboViewer cmvConservationArea  = null;
	private Label progressLabel  = null;
	private Link lblAdvanced = null;
	
	private Cursor appStartCursor;
	
	//widgets to enable/disable
	private ArrayList<Control> widgets = null;
	
	/**
	 * Creates a new splash handler 
	 */
	public InteractiveSplashHandler() {
		widgets = new ArrayList<Control>();
	}
	
	/*
	 * Enables or disables the controls in the widgets list
	 */
	private void enableControls(boolean enabled){
		for (Control w : widgets) {
			w.setEnabled(enabled);
		}
	}
	/**
	 * 
	 * @see org.eclipse.ui.splash.AbstractSplashHandler#init(org.eclipse.swt.widgets.Shell)
	 */
	public void init(final Shell splash) {
		this.parent = splash;
		// Store the shell
		super.init(splash);
		
		// Configure the shell layout
		configureUISplash();
		
		// Create UI
		createUI();		
		splash.getDisplay().readAndDispatch();
		
		// Create UI listeners
		createUIListeners();
		
		// Force the splash screen to layout
		splash.layout(true);
		
		// Keep the splash screen visible and prevent the RCP application from 
		// loading until the close button is clicked.
		splash.redraw();
		
		//run the startup script which initializes the splash screen elements
		startup();

		//auto-login for testing
//		txtUserName.setText("smart"); //$NON-NLS-1$
//		txtPassword.setText("smart"); //$NON-NLS-1$
		//handleButtonOKWidgetSelected();

		doEventLoop();
		
	}
	
	/**
	 * 
	 */
	private void doEventLoop() {
		Shell splash = getSplash();
		while (fAuthenticated == false) {
			if (splash.getDisplay().readAndDispatch() == false) {
				splash.getDisplay().sleep();
			}
		}
//		newFont.dispose();
	}

	/**
	 * Creates the ui listeners 
	 */
	private void createUIListeners() {
		btnCancel.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleButtonCancelWidgetSelected();
			}
		});	
		btnOk.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleButtonOKWidgetSelected();
			}
		});		
		lblAdvanced.addListener(SWT.Selection, new Listener() {
			@Override
			public void handleEvent(Event event) {
				handleButtonAdvancedSelected();
			}
		});

		
		cmvConservationArea.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				validateUi();
			}
		});
		
		ModifyListener validator = new ModifyListener() {	
			@Override
			public void modifyText(ModifyEvent e) {
				validateUi();
			}

		};
		txtUserName.addModifyListener(validator);
		txtPassword.addModifyListener(validator);
	}

	/*
	 * validates the ui components, enabling and disabling required components.
	 */
	private void validateUi(){
		boolean ok = !cmvConservationArea.getSelection().isEmpty();
		if (ok){
			if (!(((IStructuredSelection)cmvConservationArea.getSelection()).getFirstElement() instanceof ConservationArea)){
				ok = false;
			}
		}
		if (ok){
			ok = txtUserName.isEnabled() && (txtUserName.getText().length() > 0 && txtPassword.getText().length() > 0);
		}
		btnOk.setEnabled(ok);
	}


	/**
	 * Aborts the program when cancel button is selected
	 */
	private void handleButtonCancelWidgetSelected() {
		// Abort the loading of the RCP application
		getSplash().getDisplay().close();
		HibernateManager.endSessionFactory(false);
		System.exit(0);		
	}
	

	/**
	 * Performs the login task.
	 */
	private void handleButtonOKWidgetSelected() {
		enableControls(false);
		try {
			ConservationArea ca = (ConservationArea) ((IStructuredSelection) cmvConservationArea.getSelection()).getFirstElement();
			String username = txtUserName.getText();
			String password = txtPassword.getText();

			progressLabel.setText(Messages.InteractiveSplashHandler_Progress_ValidatingUser);

			if ((username.length() > 0) && (password.length() > 0)) {
				if (SmartStartUp.login(ca, username, password)) {
					fAuthenticated = true;
				} else {
					progressLabel.setText(Messages.InteractiveSplashHandler_Error_AuthenticationFailure);
				}
			} else {
				progressLabel.setText(Messages.InteractiveSplashHandler_Error_AuthenticationFailure);
			}
		} catch (Exception ex) {
			
			SmartPlugIn.displayLog(null, Messages.InteractiveSplashHandler_Error_LoginFailed, ex);
		}finally{
			if (!fAuthenticated){
				enableControls(true);
			}
		}
	
	}
	
	/**
	 * Opens the advanced options dialog.  When the dialog is
	 * closed the login screen is refreshed.
	 */
	private void handleButtonAdvancedSelected(){
		StartUpAdvancedDialog dialog = new StartUpAdvancedDialog(getSplash());
		int returnCode = dialog.open();
		if (returnCode == InitializeDialog.RESTART){
			//restart
			PlatformUI.getWorkbench().restart();
		}else if (returnCode != InitializeDialog.CANCEL){
			//on close refresh startup screen
			startup();
		}
	}
	
	/**
	 * Creates ui components
	 */
	private void createUI() {
		
		fCompositeLogin = new Composite(getSplash(), SWT.BORDER);
		GridLayout layout = new GridLayout(F_COLUMN_COUNT, false);
		layout.marginLeft = 135;
		layout.marginRight = 5;
		layout.marginTop = 90;
		fCompositeLogin.setLayout(layout);		
		
		//version label
		Label lblVersion = new Label(fCompositeLogin, SWT.RIGHT);
		lblVersion.setText(MessageFormat.format(Messages.InteractiveSplashHandler_VersionLabel, new Object[]{System.getProperty("org.wcs.smart.version")})); //$NON-NLS-1$
		lblVersion.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, false, 2, 1));
		final Color blue = new Color(lblVersion.getDisplay(), 50, 74,115);
		lblVersion.setForeground(blue);
		
		FontData fd = lblVersion.getFont().getFontData()[0];
		fd.setHeight(fd.getHeight()+2);
		fd.setStyle(SWT.BOLD);
		final Font versionFont = new Font(lblVersion.getDisplay(),fd);
		
		lblVersion.setFont(versionFont);
		lblVersion.addDisposeListener(new DisposeListener() {
			@Override
			public void widgetDisposed(DisposeEvent e) {
				//ensure items are disposed of properly
				versionFont.dispose();
				blue.dispose();
			}
		});
		
		Composite spanner = new Composite(fCompositeLogin, SWT.NONE);
		GridData data = new GridData(SWT.FILL, SWT.FILL, true, true);
		data.horizontalSpan = F_COLUMN_COUNT;
		spanner.setLayoutData(data);

		
		progressLabel = new Label(fCompositeLogin, SWT.RIGHT);
		progressLabel.setText("Progress Label"); //$NON-NLS-1$
		data = new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1);
		progressLabel.setLayoutData(data);
		progressLabel.setVisible(true);// false
		
		Label lblLabel = new Label(fCompositeLogin, SWT.NONE);
		lblLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
		lblLabel.setText(Messages.InteractiveSplashHandler_Ca_Label);
		
		Combo cmbConservationArea = new Combo(fCompositeLogin, SWT.DROP_DOWN | SWT.READ_ONLY);
		cmbConservationArea.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		widgets.add(cmbConservationArea);
		
		cmvConservationArea = new ComboViewer(cmbConservationArea);
		cmvConservationArea.setContentProvider(ArrayContentProvider.getInstance());
		cmvConservationArea.setLabelProvider(new ConservationAreaLabelProvider());		
		
		Label lblUserName = new Label(fCompositeLogin, SWT.NONE);
		lblUserName.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
		lblUserName.setText(Messages.InteractiveSplashHandler_Username_Label);

		txtUserName = new Text(fCompositeLogin, SWT.BORDER);
		txtUserName.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
		widgets.add(txtUserName);
		

		Label lblPassword = new Label(fCompositeLogin, SWT.NONE);
		lblPassword.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
		lblPassword.setText(Messages.InteractiveSplashHandler_Password_Label);

		txtPassword = new Text(fCompositeLogin, SWT.PASSWORD | SWT.BORDER);
		txtPassword.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
		txtPassword.addKeyListener(new KeyAdapter() {
			@Override
			public void keyReleased(KeyEvent e) {
				if (e.character == SWT.CR || e.character == SWT.LF){
					handleButtonOKWidgetSelected();
				}
			}
		});
		widgets.add(txtPassword);

		
		lblAdvanced = new Link(fCompositeLogin, SWT.NONE);
		lblAdvanced.setText("<a>" + Messages.InteractiveSplashHandler_Advanced_Label + "</a>"); //$NON-NLS-1$ //$NON-NLS-2$
		data = new GridData(SWT.RIGHT, SWT.CENTER, false, false, 2, 1);
		lblAdvanced.setLayoutData(data);
		lblAdvanced.setVisible(true);// false
		widgets.add(lblAdvanced);
		
		Label label = new Label(fCompositeLogin, SWT.NONE);
		label.setVisible(false);
		
		Composite composite_1 = new Composite(fCompositeLogin, SWT.NONE);
		GridLayout gl = new GridLayout(2, true);
		gl.marginLeft = 40;
		gl.marginRight = 0;
		gl.makeColumnsEqualWidth = true;
		composite_1.setLayout(gl);
		composite_1.setLayoutData(new GridData(SWT.FILL, SWT.NONE, true, false));
		
		
		btnCancel = new Button(composite_1, SWT.PUSH);
		btnCancel.setText(Messages.InteractiveSplashHandler_Exit_Button); 
		btnCancel.setLayoutData(new GridData(SWT.FILL, SWT.NONE, true, false));
		
		btnOk = new Button(composite_1, SWT.PUSH);
		btnOk.setText(Messages.InteractiveSplashHandler_Login_Button); 
		btnOk.setLayoutData(new GridData(SWT.FILL, SWT.NONE, true, false));
		
		widgets.add(btnOk);
		widgets.add(btnCancel);
	
		fCompositeLogin.setTabList(new Control[]{cmvConservationArea.getControl(),txtUserName, txtPassword, composite_1, lblAdvanced});
		composite_1.setTabList(new Control[]{btnOk, btnCancel});
		enableControls(false);
		
	}		
	

	/**
	 * 
	 */
	private void configureUISplash() {
		// Configure layout
		FillLayout layout = new FillLayout(); 
		getSplash().setLayout(layout);
		
		// Force shell to inherit the splash background
		getSplash().setBackgroundMode(SWT.INHERIT_DEFAULT);

	}
	
	/**
	 * Initializes the start-up screen data.  This includes
	 * loading the conservation areas.
	 */
	private void startup(){
		try {
			progressLabel.setText(Messages.InteractiveSplashHandler_intiDbProcess);
			Job job = new Job(Messages.InteractiveSplashHandler_Progress_LoadingCa) {
				
				@Override
				protected IStatus run(IProgressMonitor monitor) {
					DisplayAccess.accessDisplayDuringStartup();
					try{
					
						InteractiveSplashHandler.this.parent.getDisplay().syncExec(new Runnable(){
						@Override
						public void run() {
							
							//setup cursor
							appStartCursor = new Cursor(InteractiveSplashHandler.this.parent.getDisplay(), 
									SWT.CURSOR_APPSTARTING);
							
							InteractiveSplashHandler.this.parent.getShell().setCursor(appStartCursor);
							
							//disable controls while we reload conservation area
							enableControls(false);
							progressLabel.setText(Messages.InteractiveSplashHandler_intiDbProcess);
						}
					});
					
					SmartPlugIn.initializeDatabase();
					
					
					InteractiveSplashHandler.this.parent.getDisplay().syncExec(new Runnable(){
						@Override
						public void run() {
							progressLabel.setText(Messages.InteractiveSplashHandler_Progress_LoadingCa);
						}
					});
					
					final List<Object> cas = new ArrayList<Object>();
					try{
						cas.addAll(SmartStartUp.getConservationAreas(true));
					}catch (final Exception ex){
						InteractiveSplashHandler.this.parent.getDisplay().syncExec(new Runnable(){
							public void run(){
								SmartPlugIn.displayLogExit(ex.getMessage(), ex);
							}
						});
					}
					
					InteractiveSplashHandler.this.parent.getDisplay().syncExec(new Runnable(){
						@Override
						public void run() {
							if (cas != null && cas.size() > 0){
								cmvConservationArea.setInput(cas);
								cmvConservationArea.getCombo().select(0);
								enableControls(true);
								progressLabel.setText(""); //$NON-NLS-1$
								
								validateUi();
							} else {
								// no conservation areas
								enableControls(false);
								(new StartUpDialog(parent)).open();
								startup();
							}
							
						}});
					}finally{
						//revert cursor
						InteractiveSplashHandler.this.parent.getDisplay().syncExec(new Runnable(){
							public void run(){
								InteractiveSplashHandler.this.parent.getShell().setCursor(null);
								if (appStartCursor != null){
									appStartCursor.dispose();
								}
							}
						});
						
					}
					return Status.OK_STATUS;
				}
			};
			
			//this job won't be run until after the plugin starts anywyas
			//so we don't need this rule
//			job.setRule(SmartPlugIn.PLUGIN_START_MUTEX);
			job.schedule(50);
		
		} catch (Exception ex) {
			SmartPlugIn.displayLogExit(Messages.InteractiveSplashHandler_Error_Initialization, ex);
		}
	}
	
	 
	
}