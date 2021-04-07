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
import java.util.Locale;
import java.util.UUID;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.preferences.ConfigurationScope;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
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
import org.eclipse.swt.graphics.Point;
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
import org.wcs.smart.util.UuidUtils;

/**
 * 
 * Smart Splash Screen internative handler.  
 * 
 * @since 3.3
 * 
 */
public class InteractiveSplashHandler extends AbstractSplashHandler {
	
	private static final String LAST_CA_KEY = "LAST_CA"; //$NON-NLS-1$
	private static final String CA_PREF_NODE = "org.wcs.smart.userLoginCaPref"; //$NON-NLS-1$
	
	
	private final static int F_COLUMN_COUNT = 2;
	
	private boolean fAuthenticated = false;

	/* ui components */
	private Shell parent = null;

	private Composite fCompositeLogin = null;
	
	// bug https://www.assembla.com/spaces/smart-cs/tickets/54#/activity/ticket:
	private Combo cmbUserName = null;
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
	    String version = System.getProperty("org.wcs.smart.version"); //$NON-NLS-1$
	    System.setProperty("org.wcs.smart.version.simple", version.substring(0, version.lastIndexOf('.'))); //$NON-NLS-1$
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

		UserLoginInfo[] userInfos = UserLoginInfo.readAllFromStore();
		if (userInfos.length > 0) {
			for (UserLoginInfo u : userInfos)
				cmbUserName.add(u.getUsername());
			cmbUserName.select(0);
			txtPassword.setFocus();
		} else {
			cmbUserName.setFocus();
		}
		//TODO: remove smart/smart before build
		//cmbUserName.setText("smart");
		//txtPassword.setText("smart"); //$NON-NLS-1$
		
		doEventLoop();
	}
	
	/**
	 * 
	 */
	private void doEventLoop() {
		Shell splash = getSplash();
		while (fAuthenticated == false) {
			if (splash.isDisposed()) handleButtonCancelWidgetSelected();
			if (splash.getDisplay().readAndDispatch() == false) {
				if (splash.isDisposed()) handleButtonCancelWidgetSelected();
				splash.getDisplay().sleep();
			}
		}
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
		cmbUserName.addModifyListener(validator);
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
			ok = cmbUserName.isEnabled() && (cmbUserName.getText().length() > 0 && txtPassword.getText().length() > 0);
		}
		btnOk.setEnabled(ok);
	}


	/**
	 * Aborts the program when cancel button is selected
	 */
	private void handleButtonCancelWidgetSelected() {
		// Abort the loading of the RCP application
		if (!getSplash().isDisposed()) getSplash().getDisplay().close();
		try {
			HibernateManager.endSessionFactory(false, true);
		} catch (Exception e) {
		}
		System.exit(0);		
	}
	

	/**
	 * Performs the login task.
	 */
	private void handleButtonOKWidgetSelected() {
		enableControls(false);
		try {
			ConservationArea ca = (ConservationArea) ((IStructuredSelection) cmvConservationArea.getSelection()).getFirstElement();
			String username = cmbUserName.getText();
			String password = txtPassword.getText();

			progressLabel.setText(Messages.InteractiveSplashHandler_Progress_ValidatingUser);

			if ((username.length() > 0) && (password.length() > 0)) {
				if (SmartStartUp.login(ca, username, password)) {
					fAuthenticated = true;
					new UserLoginInfo(username).writeToStore();
					ConfigurationScope.INSTANCE.getNode(CA_PREF_NODE).put(LAST_CA_KEY, UuidUtils.uuidToString(ca.getUuid()));
					ConfigurationScope.INSTANCE.getNode(CA_PREF_NODE).flush();
				} else {
					progressLabel.setText(Messages.InteractiveSplashHandler_Error_AuthenticationFailure);
				}
			} else {
				progressLabel.setText(Messages.InteractiveSplashHandler_Error_AuthenticationFailure);
			}
		} catch (Exception ex) {
			
			SmartPlugIn.displayLog(Messages.InteractiveSplashHandler_Error_LoginFailed, ex);
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
		fCompositeLogin.setLayout(layout);		
		((GridLayout)fCompositeLogin.getLayout()).marginHeight = 0;
		((GridLayout)fCompositeLogin.getLayout()).marginWidth= 0;
		
		Composite right = new Composite(fCompositeLogin, SWT.NONE);
		right.setLayout(new GridLayout(2, false));
		((GridLayout)right.getLayout()).marginWidth = 0;
		((GridLayout)right.getLayout()).marginHeight = 0;
		right.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		Label lblSpacer = new Label(right, SWT.NONE);
		lblSpacer.setLayoutData(new GridData(SWT.RIGHT, SWT.FILL, false, false));
		int hh = (int)(getSplash().getBounds().height * 0.3);
		((GridData)lblSpacer.getLayoutData()).heightHint = hh;
		((GridData)lblSpacer.getLayoutData()).widthHint = 0;
		lblSpacer.setVisible(false);
		
		//version label
		Label lblVersion = new Label(right, SWT.NONE);
		lblVersion.setText(MessageFormat.format(Messages.InteractiveSplashHandler_VersionLabel, new Object[]{System.getProperty("org.wcs.smart.version.simple")})); //$NON-NLS-1$
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
		
		Composite spanner = new Composite(right, SWT.NONE);
		GridData data = new GridData(SWT.FILL, SWT.FILL, true, true);
		data.horizontalSpan = F_COLUMN_COUNT;
		spanner.setLayoutData(data);
		spanner.setVisible(false);

		progressLabel = new Label(right, SWT.RIGHT);
		progressLabel.setText("Progress Label"); //$NON-NLS-1$
		data = new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1);
		progressLabel.setLayoutData(data);
		progressLabel.setVisible(true);// false
		
		int padding = (int)(getSplash().getBounds().width * 0.25);
		
		Label lblLabel = new Label(right, SWT.NONE);
		lblLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
		lblLabel.setText(Messages.InteractiveSplashHandler_Ca_Label);
		((GridData)lblLabel.getLayoutData()).horizontalIndent = padding;
		
		Combo cmbConservationArea = new Combo(right, SWT.DROP_DOWN | SWT.READ_ONLY);
		cmbConservationArea.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		widgets.add(cmbConservationArea);
		
		cmvConservationArea = new ComboViewer(cmbConservationArea);
		cmvConservationArea.setContentProvider(ArrayContentProvider.getInstance());
		cmvConservationArea.setLabelProvider(new ConservationAreaLabelProvider());		
		
		Label lblUserName = new Label(right, SWT.NONE);
		lblUserName.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
		lblUserName.setText(Messages.InteractiveSplashHandler_Username_Label);
		((GridData)lblUserName.getLayoutData()).horizontalIndent = padding;

		// bug https://www.assembla.com/spaces/smart-cs/tickets/54#/activity/ticket:
		cmbUserName = new Combo(right, SWT.NONE);
		cmbUserName.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
		widgets.add(cmbUserName);
		

		Label lblPassword = new Label(right, SWT.NONE);
		lblPassword.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
		lblPassword.setText(Messages.InteractiveSplashHandler_Password_Label);
		((GridData)lblPassword.getLayoutData()).horizontalIndent = padding;

		txtPassword = new Text(right, SWT.PASSWORD | SWT.BORDER);
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

		
		lblAdvanced = new Link(right, SWT.NONE);
		lblAdvanced.setText("<a>" + Messages.InteractiveSplashHandler_Advanced_Label + "</a>"); //$NON-NLS-1$ //$NON-NLS-2$
		data = new GridData(SWT.RIGHT, SWT.CENTER, false, false, 2, 1);
		lblAdvanced.setLayoutData(data);
		lblAdvanced.setVisible(true);// false
		widgets.add(lblAdvanced);
		
		Composite bottom = new Composite(right, SWT.NONE);
		bottom.setLayout(new GridLayout(2, false));
		((GridLayout)bottom.getLayout()).marginWidth = 0;
		((GridLayout)bottom.getLayout()).marginHeight = 0;
		bottom.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		
		Label lblLang = new Label(bottom, SWT.NONE);
		lblLang.setText(Locale.getDefault().getDisplayName());
		lblLang.setLayoutData(new GridData(SWT.LEFT, SWT.BOTTOM, true, false));
		
		Composite composite_1 = new Composite(bottom, SWT.NONE);
		composite_1.setLayout(new GridLayout(2, true));
		composite_1.setLayoutData(new GridData(SWT.RIGHT, SWT.NONE, false, false));
		((GridLayout)composite_1.getLayout()).marginWidth = 0;
		
		btnCancel = new Button(composite_1, SWT.PUSH);
		btnCancel.setText(Messages.InteractiveSplashHandler_Exit_Button); 
		btnCancel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
				
		btnOk = new Button(composite_1, SWT.PUSH);
		btnOk.setText(Messages.InteractiveSplashHandler_Login_Button); 
		btnOk.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		
		int size = (int) (Math.max(btnOk.computeSize(SWT.DEFAULT, SWT.DEFAULT).x, btnCancel.computeSize(SWT.DEFAULT, SWT.DEFAULT).x) * 2.5);
		((GridData)btnCancel.getLayoutData()).widthHint = size;
		((GridData)btnOk.getLayoutData()).widthHint = size;
				
		//does different layout stuff for odd display zoom settings
		//if the login button is off the splash screen page then
		//move the layout around so it ends up on the page
		Listener l = new Listener(){
			@Override
			public void handleEvent(Event event) {
				Point btn = getSplash().toControl( btnOk.toDisplay(btnOk.getLocation()));
				if (btn.y + btnOk.getBounds().height > getSplash().getBounds().height) {
					((GridData)lblSpacer.getLayoutData()).heightHint = 0;
					FontData fd = lblVersion.getFont().getFontData()[0];
					fd.setHeight(fd.getHeight()-1);
					final Font versionFont2 = new Font(lblVersion.getDisplay(),fd);
					Font temp = lblVersion.getFont();
					lblVersion.setFont(versionFont2);
					temp.dispose();
					
					lblVersion.getParent().layout(true);
				}
				progressLabel.removeListener(SWT.Paint, this);
			}
			
		};
		progressLabel.addListener(SWT.Paint, l);
		
		widgets.add(btnOk);
		widgets.add(btnCancel);
	
		right.setTabList(new Control[]{cmvConservationArea.getControl(),cmbUserName, txtPassword, lblAdvanced, bottom});
		bottom.setTabList(new Control[]{composite_1});
		composite_1.setTabList(new Control[]{btnOk, btnCancel});
		enableControls(false);
		
	}		
	
	/**
	 * 
	 */
	private void configureUISplash() {
		// Configure layout
		FillLayout layout = new FillLayout(); 
		layout.marginHeight = 0;
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
					
					String cauuid = ConfigurationScope.INSTANCE.getNode(CA_PREF_NODE).get(LAST_CA_KEY, null);
					UUID caUuid = null;
					if (cauuid != null) {
						try {
							caUuid = UuidUtils.stringToUuid(cauuid);
						}catch (Exception ex) {
							
						}
					}
					
					final UUID thisUuid = caUuid;
					
					InteractiveSplashHandler.this.parent.getDisplay().syncExec(new Runnable(){
						@Override
						public void run() {
							if (cas != null && cas.size() > 0){
								cmvConservationArea.setInput(cas);
								cmvConservationArea.getCombo().select(0);
								if (thisUuid != null) {
									for (Object ca : cas) {
										if (ca instanceof ConservationArea && ((ConservationArea)ca).getUuid().equals(thisUuid)) {
											cmvConservationArea.setSelection(new StructuredSelection(ca));
											break;
										}
									}
								}
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