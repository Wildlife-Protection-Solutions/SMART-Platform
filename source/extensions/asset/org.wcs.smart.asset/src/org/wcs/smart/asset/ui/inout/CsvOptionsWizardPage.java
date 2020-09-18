/*
 * Copyright (C) 2020 Wildlife Conservation Society
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
package org.wcs.smart.asset.ui.inout;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.asset.internal.Messages;
import org.wcs.smart.export.dialog.DelimiterCombo;

/**
 * Page for collecting csv file details for importing asset data.
 * 
 * @author Emily
 * @since 7.0.0
 *
 */
public class CsvOptionsWizardPage extends WizardPage {

	public static final String PREFERENCE_DIR_KEY = CsvOptionsWizardPage.class.getCanonicalName() + ".dir";  //$NON-NLS-1$
	
	private DelimiterCombo cmbDelimiter;
	private ComboViewer cmbCharset;
	
	protected CsvOptionsWizardPage() {
		super("EXPORT_LOCATION_PAGE"); //$NON-NLS-1$
	}

	public void pageShown() {
		
	}
	
	
	@Override
	public IWizardPage getNextPage() {
		if (  ((AssetDataExportWizard)getWizard()).typePage.getTypes().contains(AssetDataExportWizard.Type.XML)) {
			return ((AssetDataExportWizard)getWizard()).xmlOpPage;
		}
		return null;
	}
	
	/**
	 * 
	 * @return the charset to use for exports
	 */
	public Charset getCharSet() {
		return (Charset) cmbCharset.getStructuredSelection().getFirstElement();
	}
	
	/**
	 * 
	 * @return the delimiter to use for exports
	 * @throws Exception
	 */
	public char getDelimiter() throws Exception {
		return cmbDelimiter.getDelimiter();
	}
	
	@Override
	public void createControl(Composite parent) {
		Composite main = new Composite(parent, SWT.NONE);
		main.setLayout(new GridLayout(2, false));
		main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		Label lblDelim = new Label(main, SWT.NONE);
		lblDelim.setText(Messages.CsvOptionsWizardPage_DelimLabel);
		lblDelim.setBackground(lblDelim.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
		
		cmbDelimiter = new DelimiterCombo(main, SWT.NONE);

		Label lblCharset = new Label(main, SWT.NONE);
		lblCharset.setText(Messages.CsvOptionsWizardPage_CharsetLabel);
		lblCharset.setToolTipText(Messages.CsvOptionsWizardPage_CharsetTooltip);
		lblCharset.setBackground(lblCharset.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
		
		cmbCharset = new ComboViewer(main, SWT.DROP_DOWN | SWT.READ_ONLY);
		cmbCharset.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		cmbCharset.setContentProvider(ArrayContentProvider.getInstance());
		cmbCharset.setLabelProvider(new LabelProvider() {
			public String getText(Object element) {
				return ((Charset)element).displayName();
			}
		});
		cmbCharset.setInput( Charset.availableCharsets().values() );
		Charset defaultcs = StandardCharsets.UTF_8;
		try {
			String cc = SmartPlugIn.getDefault().getDialogSettings().get(SmartPlugIn.DEFAULT_ENCODING_KEY);
			if (cc != null && !cc.isBlank()) defaultcs = Charset.forName(cc);
		}catch (Exception ex) {
			SmartPlugIn.log(ex.getMessage(), ex);
		}
		cmbCharset.setSelection(new StructuredSelection(defaultcs));
		cmbCharset.addSelectionChangedListener(e->{
			SmartPlugIn.getDefault().getDialogSettings().put(SmartPlugIn.DEFAULT_ENCODING_KEY, ((Charset)e.getStructuredSelection().getFirstElement()).name());
		});
		cmbCharset.getControl().setBackground(cmbCharset.getControl().getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
		
		setTitle(Messages.CsvOptionsWizardPage_Title);
		setMessage(Messages.CsvOptionsWizardPage_Message);
		setControl(main);
	}

}
