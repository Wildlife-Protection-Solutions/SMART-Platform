package org.wcs.smart.paws.ui;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.LocationEvent;
import org.eclipse.swt.browser.LocationListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.wcs.smart.ui.SmartStyledDialog;

public class LoginDialog extends SmartStyledDialog {

	private Browser browser ;
	private String code = null;
	
	protected LoginDialog(Shell parent) {
		super(parent);
		// TODO Auto-generated constructor stub
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
		
		// create a composite with standard margins and spacing
		Composite composite = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		composite.setLayout(layout);
		composite.setLayoutData(new GridData(GridData.FILL_BOTH));
		applyDialogFont(composite);
		
		//TODO: we probably don't want to do this
		Browser.clearSessions();
		
		browser = new Browser(composite, SWT.NONE);
		browser.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
//		String tenantid = "548d829f-38d0-42ac-9a2c-220944e5c275";
		String tenantid = "common";
//		browser.setUrl("https://login.microsoftonline.com/" + tenantid + "/oauth2/v2.0/authorize?client_id=fd200799-513a-4000-9f01-8cf6485771de" +
				browser.setUrl("https://login.microsoftonline.com/" + tenantid + "/oauth2/v2.0/authorize?client_id=fd200799-513a-4000-9f01-8cf6485771de" +
				"&response_type=code&response_mode=query&state=1234&" + 
				"&prompt=login" + 
				"&redirect_uri=https://login.microsoftonline.com/common/oauth2/nativeclient&scope=api://fd200799-513a-4000-9f01-8cf6485771de/Test");
		
		browser.addLocationListener(new LocationListener() {
			
			@Override
			public void changing(LocationEvent event) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void changed(LocationEvent event) {
				String thisurl = browser.getUrl();
				if (thisurl.startsWith("https://login.microsoftonline.com/common/oauth2/nativeclient")) {
					Map<String,String> parts = Collections.emptyMap();
					try {
						parts = parseUrl(thisurl);
					}catch (Exception ex) {
						//TODO:
						ex.printStackTrace();
						return;
					}
					
					if(parts.containsKey("state") && parts.get("state").equals("1234") && parts.containsKey("code") && !parts.get("code").isBlank()) {
						code = parts.get("code");
						LoginDialog.this.close();
					}
					
					System.out.println(thisurl);
					//TODO better error handling
//https://docs.microsoft.com/en-us/azure/active-directory/develop/v1-protocols-oauth-code
//https://login.microsoftonline.com/common/oauth2/nativeclient?
//code=OAQABAAIAAADCoMpjJXrxTq9VG9te-7FXhS9q9NmgUq2rBd1FccptCMlgftvIkg-p6Swz5IfpUHb1sm_0Q_m6SfA3BIlQpe_5qMlbA-Xo1TaW5c9QMFMObCrjcXnIqVsqH_PQ4iVMMfTrwsXriJ0wGrjrio8Bww_rNC5btaWcgGJbVrBh6YlOn0ss3yTzEGJpP_13bblsbbVm-d1YmdNDxwowWvY_yjZB4_XghrtBsotK9SIrfe9inthRwvp8OhubbPVFijR4dO6zFxmExskKyJ6YguUFezylpDYwcIdpR1_sxtIycRW-TQROtTuMm981Hyea2CTTdvgFMHTe9OaLy34e6VCZ3UF2uCvj3DTrQ1XYu49YrpAem2azezzMFWt6L2hZv5myY3PuP14CBwap8gHuFgAMZ9NPGdkmv7GbaH1W66TkXNkfw8LzpKqD4-OrO0QSSFTiBM3csRv9gWWfhMKMSO28h1M8WV7zjgd8ZLjIJpf0aT_s8PjBWZANSZ05JJ0r1ahMmF3Ko4pe1F5cKTee2OwejaTY3cWy5s_zrPUB5U8ir1LCcjV93zg21VP2c-2UKaT4K_PZq8dlj6FIu6krGjV1Ia-PMem35_J4_Su878BkEfxPwrDw9kfZCBv-sgA7-iRoFTZbF2HEmTMqCYGT8i2Q1ytUomfnnTzIA6Q82N3-YlfuZxwWvHJIw8UQc-p6RuQFM5ApFRYxaBwTpAJkewzYbGK3l1GTzDfOfCNx3Dh-vSmQ89NiDiFPJ19F0YiEK0UgJ2mnLqlzgQLPXy56wdETiwZTuyveCwA0VI7qekrNEbhnGZRW0QymNapYMsUwblJqFTXprtknqt5WP1ecFSvNZjsuCMXndwYmJrDXYUQ42PaxgJHiyl8s8LhK0gMvLdNgi76bt3ZhNWlnWEu96WB1KwBD6QcwR9ESOQTh-C57SdCwTFPo7Bd8Odsdt6f82CR5Ei2Qilj9gCi2xJzDYYonYd7RNr8ctBmhOykF6GyEMDj4FeIWp6JGdt4wjGjHbmqXNQ5rcheqYfTMfbL4wCxy2VsSORXeQpP2eu6AlANyMu3KqVkXgCuwV6wQWMjSWZ_IEaIRNpJqc__6zPpi43uLtq0vei-NIHWjaAp7NvBwVL_T_iEJkxvyM9PLo9M4HVuKz0MgAA
//&state=1234&session_state=dd627b71-da64-467a-a35f-e817a7031475
					
					
				}
			}
		});
		
		getShell().setText("Microsoft Azure Login");
		return composite;
	}
	
	private Map<String,String> parseUrl(String url) throws Exception{
		if (url == null || url.isBlank()) return Collections.emptyMap();
		
		String querypart = url.substring(url.indexOf('?') + 1);
		if (querypart.isBlank()) return Collections.emptyMap();
		
		String[] bits = querypart.split("&");
		HashMap<String,String> parts = new HashMap<>();
		for (String bit : bits) {
			int index = bit.indexOf('=');
			String key = index > 0 ? bit.substring(0,index) : bit;
			String value = index > 0 && bit.length() > index + 1 ? bit.substring(index+1) : "";
			if (parts.containsKey(key)) throw new Exception("multiple parameter parsing not supported");
			parts.put(key,value);
		}
		return parts;
	}
}
