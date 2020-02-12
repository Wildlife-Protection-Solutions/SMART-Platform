/*
 * Copyright (C) 2019 Wildlife Conservation Society
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
package org.wcs.smart.paws.ui;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.LocationEvent;
import org.eclipse.swt.browser.LocationListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.hibernate.Session;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.paws.PawsPlugIn;
import org.wcs.smart.paws.internal.Messages;
import org.wcs.smart.paws.model.PawsWorkspace;
import org.wcs.smart.ui.SmartStyledDialog;

/**
 * Dialog for logging into Azure AD and getting authorization code
 * @author Emily
 *
 */
public class LoginDialog extends SmartStyledDialog {

	private Browser browser ;
	private String code = null;
	
	public LoginDialog(Shell parent) {
		super(parent);
	}
	
	@Override
	public Point getInitialSize() {
		return new Point(580, 540);
	}

	
	@Override
	protected Control createContents(Composite parent) {
		// create the top level composite for the dialog
		Composite composite = new Composite(parent, 0);
		GridLayout layout = new GridLayout();
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		layout.verticalSpacing = 0;
		composite.setLayout(layout);
		composite.setLayoutData(new GridData(GridData.FILL_BOTH));
		applyDialogFont(composite);
		// initialize the dialog units
		initializeDialogUnits(composite);
		// create the dialog area and button bar
		dialogArea = createDialogArea(composite);

		return composite;
	}
	
	public String getAuthorizationCode() {
		return this.code;
	}
	
	@Override
	protected Control createDialogArea(Composite parent) {
		
		getShell().setText(Messages.LoginDialog_MsLogin);
		
		// create a composite with standard margins and spacing
		Composite composite = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		composite.setLayout(layout);
		composite.setLayoutData(new GridData(GridData.FILL_BOTH));
		applyDialogFont(composite);
		
		PawsWorkspace ws = null;
		try(Session session = HibernateManager.openSession()){
			ws = QueryFactory.buildQuery(session, PawsWorkspace.class,
					new Object[] {"conservationArea", SmartDB.getCurrentConservationArea()}).uniqueResult(); //$NON-NLS-1$
		}
		if (ws == null || ws.getUrl() == null || ws.getUrl().isBlank()) {
			Label l = new Label(composite, SWT.WRAP);
			l.setText(Messages.LoginDialog_NoWorkspace);
			return composite;
		}
		
		Browser.clearSessions();
		
		browser = new Browser(composite, SWT.NONE);
		browser.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		String state = UUID.randomUUID().toString();
		String redirectUri = "https://login.microsoftonline.com/common/oauth2/nativeclient"; //$NON-NLS-1$
		
		StringBuilder sb = new StringBuilder();
		sb.append(ws.getUrl());
		sb.append("/"); //$NON-NLS-1$
		sb.append("authorize"); //$NON-NLS-1$
		sb.append("?"); //$NON-NLS-1$
		sb.append("client_id=" + ws.getClientId()); //$NON-NLS-1$
		sb.append("&response_type=code"); //$NON-NLS-1$
		sb.append("&response_mode=query"); //$NON-NLS-1$
		sb.append("&prompt=login"); //$NON-NLS-1$
		sb.append("&state=" + state); //$NON-NLS-1$
		sb.append("&redirect_uri=" + redirectUri); //$NON-NLS-1$
		
		browser.setUrl(sb.toString());
		
		browser.addLocationListener(new LocationListener() {
			
			private boolean doChange = true;
			
			@Override
			public void changing(LocationEvent event) { }
			
			@Override
			public void changed(LocationEvent event) {
				if (!doChange) return;
				doChange = false;
				try {
					String thisurl = browser.getUrl();
					if (thisurl.startsWith(redirectUri)) {
						Map<String,String> parts = Collections.emptyMap();
						try {
							parts = parseUrl(thisurl);
						}catch (Exception ex) {
							PawsPlugIn.displayLog("Login Error", ex); //$NON-NLS-1$
							browser.setUrl(sb.toString());
							return;
						}
						
						if(parts.containsKey("state") && parts.get("state").equals(state)  //$NON-NLS-1$ //$NON-NLS-2$
								&& parts.containsKey("code") && !parts.get("code").isBlank()) { //$NON-NLS-1$ //$NON-NLS-2$
							code = parts.get("code"); //$NON-NLS-1$
							LoginDialog.this.close();
						}else {
							//login failed
						}
					}
				}finally {
					doChange = true;
				}
			}
		});
		
		
		return composite;
	}
	
	private Map<String,String> parseUrl(String url) throws Exception{
		if (url == null || url.isBlank()) return Collections.emptyMap();
		
		String querypart = url.substring(url.indexOf('?') + 1);
		if (querypart.isBlank()) return Collections.emptyMap();
		
		String[] bits = querypart.split("&"); //$NON-NLS-1$
		HashMap<String,String> parts = new HashMap<>();
		for (String bit : bits) {
			int index = bit.indexOf('=');
			String key = index > 0 ? bit.substring(0,index) : bit;
			String value = index > 0 && bit.length() > index + 1 ? bit.substring(index+1) : ""; //$NON-NLS-1$
			if (parts.containsKey(key)) throw new Exception(Messages.LoginDialog_parameterparseerror);
			parts.put(key,value);
		}
		return parts;
	}
}
