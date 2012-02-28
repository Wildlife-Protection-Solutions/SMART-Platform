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
import org.eclipse.swt.events.KeyListener;
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
import org.eclipse.ui.splash.AbstractSplashHandler;
import org.eclipse.wb.swt.ResourceManager;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.startup.SmartStartUp;
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
	//	txtUserName.setText("egouge");
	//	txtPassword.setText("smart");
	//	handleButtonOKWidgetSelected();

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
		
		KeyListener validator = new KeyAdapter() {	
			@Override
			public void keyReleased(KeyEvent e) {
				validateUi();
			}

		};
		txtUserName.addKeyListener(validator);
		txtPassword.addKeyListener(validator);
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

			progressLabel.setText("Validating User");

			if ((username.length() > 0) && (password.length() > 0)) {
				if (SmartStartUp.login(ca, username, password)) {
					fAuthenticated = true;
				} else {
					progressLabel.setText("Authentication Failure");
				}
			} else {
				progressLabel.setText("Authentication Failure");
			}
		} catch (Exception ex) {
			SmartPlugIn.displayLog(null, "Error logging in user.  Please ensure a conservation area is selected and correct credentials are supplied.", ex);
		}
	}
	
	/**
	 * Opens the advanced options dialog.  When the dialog is
	 * closed the login screen is refreshed.
	 */
	private void handleButtonAdvancedSelected(){
		StartUpAdvancedDialog dialog = new StartUpAdvancedDialog(getSplash());
		dialog.open();
		
		//on close refresh startup screen
		startup();
	}
	
	/**
	 * Creates ui components
	 */
	private void createUI() {
		
		fCompositeLogin = new Composite(getSplash(), SWT.BORDER);
		GridLayout layout = new GridLayout(F_COLUMN_COUNT, false);
		layout.marginLeft = 50;
		layout.marginRight = 50;
		fCompositeLogin.setLayout(layout);		
		
		Composite spanner = new Composite(fCompositeLogin, SWT.NONE);
		GridData data = new GridData(SWT.FILL, SWT.FILL, true, true);
		data.horizontalSpan = F_COLUMN_COUNT;
		spanner.setLayoutData(data);
		
		
		progressLabel = new Label(fCompositeLogin, SWT.RIGHT);
		progressLabel.setText("Progress Label");
		data = new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1);
		progressLabel.setLayoutData(data);
		progressLabel.setVisible(true);// false

		
		Label lblLabel = new Label(fCompositeLogin, SWT.NONE);
		lblLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
		lblLabel.setText("Conservation Area:");

		
		Combo cmbConservationArea = new Combo(fCompositeLogin, SWT.DROP_DOWN | SWT.READ_ONLY);
		cmbConservationArea.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		widgets.add(cmbConservationArea);
		
		cmvConservationArea = new ComboViewer(cmbConservationArea);
		cmvConservationArea.setLabelProvider(new LabelProvider(){
			public String getText(Object element) {
				ConservationArea ca = ((ConservationArea)element);
				return ca.getId() + " - " + ca.getName();
			}
		});		
		
		Label lblUserName = new Label(fCompositeLogin, SWT.NONE);
		lblUserName.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
		lblUserName.setText("User Name:");
		
		txtUserName = new Text(fCompositeLogin, SWT.BORDER);
		txtUserName.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
		widgets.add(txtUserName);
		

		Label lblPassword = new Label(fCompositeLogin, SWT.NONE);
		lblPassword.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
		lblPassword.setText("Password");

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
		lblAdvanced.setText("Advanced...");
		data = new GridData(SWT.RIGHT, SWT.CENTER, false, false, 2, 1);
		lblAdvanced.setLayoutData(data);
		lblAdvanced.setVisible(true);// false
		lblAdvanced.setForeground(ResourceManager.getColor(SWT.COLOR_DARK_BLUE));
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
		btnCancel.setText("Exit"); 
		btnCancel.setLayoutData(new GridData(SWT.FILL, SWT.NONE, true, false));
		
		btnOk = new Button(composite_1, SWT.PUSH);
		btnOk.setText("Login"); 
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
			progressLabel.setText("Loading Conservation Areas ...");
			List<ConservationArea> cas = SmartStartUp.getConservationAreas();
			cmvConservationArea.getCombo().removeAll();

			if (cas != null && cas.size() > 0) {
				for (ConservationArea ca : cas) {
					cmvConservationArea.add(ca);
				}
				cmvConservationArea.getCombo().select(0);
				enableControls(true);
				progressLabel.setText("");
				
				validateUi();
			} else {
				// no conservation areas
				enableControls(false);
				(new StartUpDialog(parent)).open();
				startup();
			}

		} catch (Exception ex) {
			SmartPlugIn
					.displayLogExit(
							"Error initializing application.  Please see error log for more details.",
							ex);
		}
	}
	
	
}