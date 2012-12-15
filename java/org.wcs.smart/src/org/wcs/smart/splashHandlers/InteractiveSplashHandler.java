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


import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.application.DisplayAccess;
import org.eclipse.ui.splash.AbstractSplashHandler;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.internal.Messages;
import org.wcs.smart.startup.SmartStartUp;
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
	private Label lblAdvanced = null;
	
//	private Font newFont;
	
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
		lblAdvanced.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseUp(MouseEvent e) {
				handleButtonAdvancedSelected();
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
		btnOk.setEnabled((txtUserName.getText().length() > 0 && txtPassword.getText().length() > 0));
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
		}
	}
	
	/**
	 * Opens the advanced options dialog.  When the dialog is
	 * closed the login screen is refreshed.
	 */
	private void handleButtonAdvancedSelected(){
		StartUpAdvancedDialog dialog = new StartUpAdvancedDialog(getSplash());
		if (dialog.open() != InitializeDialog.CANCEL){
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
		fCompositeLogin.setLayout(layout);		
		
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
		
//		FontData[] fd = lblLabel.getFont().getFontData();
//		fd[0].setStyle(SWT.BOLD);
//		newFont = new Font(getSplash().getDisplay(), fd[0]);
//		lblLabel.setFont(newFont);
//		lblLabel.setForeground(getSplash().getDisplay().getSystemColor(SWT.COLOR_WHITE));
		
		Combo cmbConservationArea = new Combo(fCompositeLogin, SWT.DROP_DOWN | SWT.READ_ONLY);
		cmbConservationArea.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		widgets.add(cmbConservationArea);
		
		cmvConservationArea = new ComboViewer(cmbConservationArea);
		cmvConservationArea.setLabelProvider(new LabelProvider(){
			public String getText(Object element) {
				ConservationArea ca = ((ConservationArea)element);
				return ca.getId() + " - " + ca.getName(); //$NON-NLS-1$
			}
		});		
		
		Label lblUserName = new Label(fCompositeLogin, SWT.NONE);
		lblUserName.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
		lblUserName.setText(Messages.InteractiveSplashHandler_Username_Label);
//		lblUserName.setFont(newFont);
//		lblUserName.setForeground(getSplash().getDisplay().getSystemColor(SWT.COLOR_WHITE));
		
		txtUserName = new Text(fCompositeLogin, SWT.BORDER);
		txtUserName.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
		widgets.add(txtUserName);
		

		Label lblPassword = new Label(fCompositeLogin, SWT.NONE);
		lblPassword.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
		lblPassword.setText(Messages.InteractiveSplashHandler_Password_Label);
//		lblPassword.setFont(newFont);
//		lblPassword.setForeground(getSplash().getDisplay().getSystemColor(SWT.COLOR_WHITE));
		
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

		
		lblAdvanced = new Label(fCompositeLogin, SWT.NONE | SWT.READ_ONLY);
		lblAdvanced.setText(Messages.InteractiveSplashHandler_Advanced_Label);
		
		
		data = new GridData(SWT.RIGHT, SWT.CENTER, false, false, 2, 1);
		lblAdvanced.setLayoutData(data);
		lblAdvanced.setVisible(true);// false
//		lblAdvanced.setForeground(getSplash().getDisplay().getSystemColor(SWT.COLOR_WHITE));
		lblAdvanced.setCursor( new Cursor(getSplash().getDisplay(),  SWT.CURSOR_HAND));
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
			progressLabel.setText(Messages.InteractiveSplashHandler_Progress_LoadingCa);
			
			Runnable r = new Runnable(){

				@Override
				public void run() {
					DisplayAccess.accessDisplayDuringStartup();
					final List<ConservationArea> cas = new ArrayList<ConservationArea>();
					try{
						cas.addAll(SmartStartUp.getConservationAreas());
					}catch (final Exception ex){
						InteractiveSplashHandler.this.parent.getDisplay().asyncExec(new Runnable(){
							public void run(){
								SmartPlugIn.displayLogExit(ex.getMessage(), ex);
							}
						});
					}
					
					InteractiveSplashHandler.this.parent.getDisplay().asyncExec(new Runnable(){
						@Override
						public void run() {
							cmvConservationArea.getCombo().removeAll();

							if (cas != null && cas.size() > 0) {
								for (ConservationArea ca : cas) {
									cmvConservationArea.add(ca);
								}
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
					
				}};
				Thread t = new Thread(r);
				t.setContextClassLoader(Thread.currentThread().getContextClassLoader());
				t.start();
			

		} catch (Exception ex) {
			SmartPlugIn
					.displayLogExit(
							Messages.InteractiveSplashHandler_Error_Initialization,
							ex);
		}
	}
	
	 
	
}