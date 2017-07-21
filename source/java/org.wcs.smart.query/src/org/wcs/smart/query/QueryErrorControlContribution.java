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
import org.wcs.smart.query.ui.QuerySourceProvider;

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
	
	private QuerySourceProvider provider = null;
	
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
			if (sourceName.equals(QuerySourceProvider.QUERY_DATE_VALID) || sourceName.equals(QuerySourceProvider.QUERY_VALID)){
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
		provider = (QuerySourceProvider) service.getSourceProvider(QuerySourceProvider.QUERY_VALID);
		provider.addSourceProviderListener(listener);	
    }
    
	@Override
	protected Control createControl(Composite parent) {
		//see bug https://bugs.eclipse.org/bugs/show_bug.cgi?id=471313
		parent.getParent().setRedraw(true);
		
		main = new Composite(parent, SWT.NONE);
		GridLayout gl = new GridLayout(2, false);
		gl.marginHeight = gl.verticalSpacing = gl.marginWidth = 0;
		main.setLayout(gl);
		lblErrorImage = new Label(main, SWT.NONE);
		lblErrorImage.setImage(QueryPlugIn.getDefault().getImageRegistry().get(QueryPlugIn.EXCLAMATION_ICON));
		lblError = new Label(main, SWT.NONE);
		lblError.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, true));
		lblError.setText(Messages.QueryErrorControlContribution_QueryErrorText);
		main.setVisible(false);
		
		initValues();
		return main;
	}
	
	private void initValues() {
		if (main.isDisposed()){
			return;
		}
		
		String errorMessage = null;
		//check query date valid
		Object x = provider.getCurrentState().get(QuerySourceProvider.QUERY_DATE_VALID);
		if (x instanceof String && x != null){
			errorMessage = (String)x;
		}
		
		if (errorMessage == null){
			x = provider.getCurrentState().get(QuerySourceProvider.QUERY_VALID);
			
			if (x != null){
				errorMessage = x.toString();
			}
			
		}
		
		if (errorMessage != null) {
			main.setVisible(true);
			lblError.setToolTipText(errorMessage);
			lblError.setText(Messages.QueryErrorControlContribution_QueryErrorText);
			main.layout();
		} else {
			main.setVisible(false);
		}

	}

}
