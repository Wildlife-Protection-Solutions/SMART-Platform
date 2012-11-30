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
package org.wcs.smart.query;

import java.util.Map;

import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.ISourceProviderListener;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.menus.WorkbenchWindowControlContribution;
import org.eclipse.ui.services.ISourceProviderService;
import org.wcs.smart.query.internal.Messages;
import org.wcs.smart.query.ui.SourceProvider;

/**
 * Contribution item for displaying query error reasons.
 * @author egouge
 * @since 1.0.0
 */
public class QueryErrorControlContribution extends
		WorkbenchWindowControlContribution {
	
	private Label lblError;
	private Label lblErrorImage;
	private Composite main;
	
	private SourceProvider provider = null;
	
	private ISourceProviderListener listener = new ISourceProviderListener() {

		@SuppressWarnings("rawtypes")
		@Override
		public void sourceChanged(int sourcePriority, Map sourceValuesByName) {
		}

		@Override
		public void sourceChanged(int sourcePriority, String sourceName,
				Object sourceValue) {
			if (lblError == null || lblErrorImage == null){
				return;
			}
			if (sourceName.equals(SourceProvider.QUERY_VALID)){
				initValues();
			}
		}
		
	};

	public QueryErrorControlContribution() {
		addListener();
	}
	
	public QueryErrorControlContribution(String id) {
		super(id);
		addListener();
	}

    public void dispose() {
    	super.dispose();
      	if (provider != null){
      		provider.removeSourceProviderListener(listener);
      	}
    }
    
    private void addListener(){
    	ISourceProviderService service = (ISourceProviderService)PlatformUI.getWorkbench().getService(ISourceProviderService.class);
		provider = (SourceProvider) service.getSourceProvider(SourceProvider.QUERY_VALID);
		provider.addSourceProviderListener(listener);	
    }
    
	@Override
	protected Control createControl(Composite parent) {
		main = new Composite(parent, SWT.NONE);
		GridLayout gl = new GridLayout(2, false);
		gl.marginHeight = gl.verticalSpacing = gl.marginWidth = 0;
		main.setLayout(gl);
		lblErrorImage = new Label(main, SWT.NONE);
		lblErrorImage.setImage(JFaceResources.getImageRegistry().get(QueryPlugIn.EXCLAMATION_ICON));
		lblError = new Label(main, SWT.NONE);
		lblError.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, true));
		main.setVisible(false);
		
		initValues();
		return main;
	}
	
	private void initValues() {
		Object x = provider.getCurrentState().get(SourceProvider.QUERY_VALID);
		boolean isValid = true;
		if (x instanceof Boolean){
			isValid = (Boolean)x;
		}
		if (main.isDisposed()){
			return;
		}
		if (!isValid) {
			main.setVisible(true);
			if (provider != null) {
				String tip = (String) provider.getCurrentState().get(
						SourceProvider.QUERY_ERROR_MESSAGE);
				if (tip != null) {
					lblError.setToolTipText(tip);
				} else {
					lblError.setToolTipText(""); //$NON-NLS-1$
				}
			}
			lblError.setText(Messages.QueryErrorControlContribution_QueryErrorText);
			main.layout();
		} else {
			main.setVisible(false);
		}

	}

}
