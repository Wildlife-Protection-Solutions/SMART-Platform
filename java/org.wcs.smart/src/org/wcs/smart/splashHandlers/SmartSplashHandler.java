/* --- NOT USED */ 
package org.wcs.smart.splashHandlers;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
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
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.splash.AbstractSplashHandler;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.startup.SmartStartUp;
import org.wcs.smart.ui.internal.startup.StartUpAdvancedDialog;
import org.wcs.smart.ui.internal.startup.StartUpDialog;

/**
 * @since 3.3
 * 
 */
public class SmartSplashHandler extends AbstractSplashHandler {
	private final static int F_LABEL_HORIZONTAL_INDENT = 175;

	private final static int F_BUTTON_WIDTH_HINT = 80;

	private final static int F_TEXT_WIDTH_HINT = 175;
	
	private final static int F_COLUMN_COUNT = 2;
	
	private Composite fCompositeLogin;
	
	private Text txtUserName;
	private Text txtPassword;
	
	private Button btnOk;
	private Button btnCancel;
	
	private boolean fAuthenticated;
	
	private ArrayList<Control> widgets = null;

	private Label progressLabel;

	private ProgressBar progressBar;
	
	private Shell parent;

	private ComboViewer cmvConservationArea;
	
	private Label lblAdvanced;
	
	/**
	 * 
	 */
	public SmartSplashHandler() {
		fCompositeLogin = null;
		txtUserName = null;
		txtPassword = null;
		btnOk = null;
		btnCancel = null;
		fAuthenticated = false;
		lblAdvanced = null;
		
		cmvConservationArea = null;
		
		widgets = new ArrayList<Control>();
	}
	
	
	
	private void enableControls(boolean enabled){
		for (Control w : widgets) {
			w.setEnabled(enabled);
		}
	}
	/*
	 * (non-Javadoc)
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
		
		Runnable r = new Runnable() {
			@Override
			public void run() {
				startup();
			}
		};
		r.run();
		
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
	 * 
	 */
	private void createUIListeners() {
		// Create the OK button listeners
		createUIListenersButtonOK();
		// Create the cancel button listeners
		createUIListenersButtonCancel();
	}

	/**
	 * 
	 */
	private void createUIListenersButtonCancel() {
		btnCancel.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleButtonCancelWidgetSelected();
			}
		});		
	}

	/**
	 * 
	 */
	private void handleButtonCancelWidgetSelected() {
		// Abort the loading of the RCP application
		getSplash().getDisplay().close();
		System.exit(0);		
	}
	
	/**
	 * 
	 */
	private void createUIListenersButtonOK() {
		btnOk.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleButtonOKWidgetSelected();
			}
		});				
	}

	/**
	 * 
	 */
	private void handleButtonOKWidgetSelected() {
		ConservationArea ca = (ConservationArea)((IStructuredSelection)cmvConservationArea.getSelection()).getFirstElement();
		String username = txtUserName.getText();
		String password = txtPassword.getText();
		
		progressLabel.setText("Validating User");
		
		// Aunthentication is successful if a user provides any username and
		// any password
		if ((username.length() > 0) &&
				(password.length() > 0)) {
			
			if (SmartStartUp.login(ca, username, password)){
				fAuthenticated = true;
			}else{
				progressLabel.setText("Authentication Failed");
			}
		} else {
			progressLabel.setText("Authentication Failed");
			MessageDialog.openError(
					getSplash(),
					"Authentication Failed",  //$NON-NLS-1$
					"A username and password must be specified to login.");  //$NON-NLS-1$
		}
	}
	
	private void handleButtonAdvancedSelected(){
		StartUpAdvancedDialog dialog = new StartUpAdvancedDialog(getSplash());
		dialog.open();
		
		//on close refresh startup screen
		startup();
	}
	
	/**
	 * 
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
		//progressLabel.setForeground(SWTResourceManager.getColor(SWT.COLOR_WHITE));
		progressLabel.setLayoutData(data);
		progressLabel.setVisible(true);// false

		
		Label lblLabel = new Label(fCompositeLogin, SWT.NONE);
		lblLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
		lblLabel.setText("Conservation Area:");
//		lblLabel.setForeground(SWTResourceManager.getColor(SWT.COLOR_WHITE)); 
		//widgets.add(lblLabel);
		
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
//		lblUserName.setForeground(SWTResourceManager.getColor(SWT.COLOR_WHITE));
		//widgets.add(lblUserName);
		
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
		lblAdvanced.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_DARK_BLUE));
		lblAdvanced.setCursor( new Cursor(getSplash().getDisplay(),  SWT.CURSOR_HAND));
		lblAdvanced.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseUp(MouseEvent e) {
				handleButtonAdvancedSelected();
			}
		});
		
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
		btnCancel.setText("Cancel"); 
		btnCancel.setLayoutData(new GridData(SWT.FILL, SWT.NONE, true, false));
		
		btnOk = new Button(composite_1, SWT.PUSH);
		btnOk.setText("Login"); 
		btnOk.setLayoutData(new GridData(SWT.FILL, SWT.NONE, true, false));
		
		widgets.add(btnOk);
		widgets.add(btnCancel);
		

		
//		progressBar = new ProgressBar(fCompositeLogin, SWT.NONE | SWT.INDETERMINATE);
//		GridData data = new GridData(SWT.RIGHT, SWT.CENTER, false, false, 2, 1);
//		progressBar.setLayoutData(data);
//		progressBar.setVisible(true); 
		
	
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
	
	
	private void startup(){
		progressLabel.setText("Loading Conservation Areas ...");
		getSplash().layout(true);
		List<ConservationArea> cas = SmartStartUp.getConservationAreas();
		
		cmvConservationArea.getCombo().removeAll();
		if (cas != null){
			if (cas.size() > 0){
				for (ConservationArea ca : cas) {
					cmvConservationArea.add(ca);
				}
				cmvConservationArea.getCombo().select(0);
				enableControls(true);
				progressLabel.setText("");
				
			}else{
				enableControls(false);
				(new StartUpDialog(parent)).open();
				startup();
			}
		}
	}
	
	
}