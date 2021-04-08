/*
 * Copyright (C) 2021 Wildlife Conservation Society
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
package org.wcs.smart.ui.internal.preference;

import java.text.MessageFormat;
import java.util.List;

import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.hibernate.Session;
import org.wcs.smart.IdGeneratorContribution;
import org.wcs.smart.IdGeneratorManager;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.internal.Messages;

/**
 * Preference page for specifying patterns for generating system identifiers (patrol ids etc) 
 * 
 * @author Emily
 *
 */
public class IDTokenPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {

	public static final String ID = "org.wcs.smart.preference.IdTokens"; //$NON-NLS-1$

	private List<IdGeneratorContribution> contributions;
	
	public IDTokenPreferencePage() {
	}

	public IDTokenPreferencePage(String title) {
		super(title);
	}

	public IDTokenPreferencePage(String title, ImageDescriptor image) {
		super(title, image);
	}

	@Override
	public void init(IWorkbench workbench) {
		contributions = IdGeneratorManager.INSTANCE.getContributions();
	}

	@Override
	protected Control createContents(Composite parent) {
		Composite c = new Composite(parent, SWT.NONE);
		c.setLayout(new GridLayout());
		((GridLayout)c.getLayout()).marginWidth = 0;
		((GridLayout)c.getLayout()).marginHeight = 0;
		
		Text info = new Text(c, SWT.BORDER | SWT.MULTI);
		info.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		info.setEditable(false);
		StringBuilder sb = new StringBuilder();
		sb.append(Messages.IDTokenPreferencePage_AllTokens + "\n"); //$NON-NLS-1$
		for (IdGeneratorManager.Token token : IdGeneratorManager.SHARED) {
			sb.append(token.token);
			sb.append(" - "); //$NON-NLS-1$
			sb.append(token.getDescription());
			sb.append("\n"); //$NON-NLS-1$
		}
		sb.deleteCharAt(sb.length() - 1);
		info.setText(sb.toString());
		
		
		for (IdGeneratorContribution item : contributions) {
			Composite part = item.createComposite(c);
			part.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		}
		
		try(Session session = HibernateManager.openSession()){
			for (IdGeneratorContribution item : contributions) item.initComponent(session);	
		}
		
		return c;
	}

	@Override
	protected void performDefaults() {
		
	}
	
	@Override
	public boolean performOk() {
		try(Session session = HibernateManager.openSession()){
			session.beginTransaction();
			try {
				for (IdGeneratorContribution item : contributions) {
					if (!item.save(session)) {
						session.getTransaction().rollback();
						SmartPlugIn.displayError(MessageFormat.format(Messages.IDTokenPreferencePage_SaveError, getTitle()), null);
						return false;
					}
				}
				session.getTransaction().commit();
			}catch (Exception ex) {
				session.getTransaction().rollback();
				SmartPlugIn.displayLog(ex.getMessage(), ex);
				return false;
			}
		}
		return true;
	}
}
