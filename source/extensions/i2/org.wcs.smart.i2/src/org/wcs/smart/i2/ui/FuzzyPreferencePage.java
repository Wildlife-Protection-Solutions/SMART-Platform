/*
 * Copyright (C) 2016 Wildlife Conservation Society
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
package org.wcs.smart.i2.ui;

import java.lang.reflect.InvocationTargetException;
import java.text.MessageFormat;
import java.util.Locale;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.hibernate.Session;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.i2.IntelAnalystUserLevel;
import org.wcs.smart.i2.Intelligence2PlugIn;
import org.wcs.smart.i2.search.SearchDataGenerator;
import org.wcs.smart.i2.search.SearchManager;

public class FuzzyPreferencePage extends PreferencePage implements
		IWorkbenchPreferencePage {

	public static final String ID = "org.wcs.smart.i2.fuzzymatching";
	
	public FuzzyPreferencePage() {
		this("Fuzzy String Matching");
	}

	public FuzzyPreferencePage(String title) {
		super(title);
	}

	public FuzzyPreferencePage(String title, ImageDescriptor image) {
		super(title, image);
	}

	@Override
	public void init(IWorkbench workbench) {
	
	}

	@Override
	protected Control createContents(Composite parent) {
		if (!SmartDB.getCurrentEmployee().supportsUser(IntelAnalystUserLevel.INSTANCE)){
			Label l = new Label(parent, SWT.NONE);
			l.setText(MessageFormat.format("Only {0} users can access this page.", IntelAnalystUserLevel.INSTANCE.getGuiName(Locale.getDefault())));
			return l;
		}
		Composite c = new Composite(parent, SWT.NONE);
		c.setLayout(new GridLayout());
		((GridLayout)c.getLayout()).marginWidth = 0;
		((GridLayout)c.getLayout()).marginHeight = 0;
		

		Button btn = new Button(c, SWT.PUSH);
		btn.setText("Generate Test Data");
		btn.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				ProgressMonitorDialog pmd = new ProgressMonitorDialog(getShell());
					try{
					pmd.run(true, true, new IRunnableWithProgress() {
						
						@Override
						public void run(IProgressMonitor monitor) throws InvocationTargetException,
								InterruptedException {
							Session s = HibernateManager.openSession();
							try{
//								s.beginTransaction();
//								SearchDataGenerator.generateData(s);
//								s.getTransaction().commit();
//								s.beginTransaction();
//								SearchDataGenerator.generateData2(s);
//								s.getTransaction().commit();
								
								s.beginTransaction();
								SearchDataGenerator.generateEntities(s, 500);
								s.getTransaction().commit();
							}catch (Exception ex){
								ex.printStackTrace();
							}finally{
								s.close();
							}
						}
					});
				}catch (Exception ex){
					Intelligence2PlugIn.displayLog("Error re-indexing fuzzy searching", ex);
				}
			}
		});
		
		
		Group g = new Group(c, SWT.NONE);
		g.setText("String Searching");
		g.setLayout(new GridLayout());
		g.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		Label l = new Label(g, SWT.WRAP);
		l.setText("Test search results.  Enter the string you are searching for and the string you expect it to match.  The results will tell you if it matches or not.");
		l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		((GridData)l.getLayoutData()).widthHint = 200;
		
		Composite c2 = new Composite(g, SWT.NONE);
		c2.setLayout(new GridLayout(2, false));
		c2.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		l = new Label(c2, SWT.NONE);
		l.setText("Search For:");
		Text txtSearchFor = new Text(c2, SWT.BORDER);
		txtSearchFor.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		l = new Label(c2, SWT.NONE);
		l.setText("Search In:");
		Text txtSearchIn = new Text(c2, SWT.BORDER);
		txtSearchIn.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		Button btnMatch = new Button(c2, SWT.PUSH);
		btnMatch.setLayoutData(new GridData(SWT.LEFT, SWT.FILL, false, false, 2, 1));
		btnMatch.setText("Match Strings");
		
		l = new Label(c2, SWT.NONE);
		l.setText("Results:");
		Text txtSearchResult = new Text(c2, SWT.BORDER);
		txtSearchResult.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		btnMatch.addSelectionListener(new SelectionAdapter(){
			@Override
			public void widgetSelected(SelectionEvent e) {
				Double value = SearchManager.INSTANCE.getRating(txtSearchFor.getText(), txtSearchIn.getText());
				if (value == null){
					txtSearchResult.setText("NO MATCH");
				}else{
					txtSearchResult.setText(MessageFormat.format("MATCH: {0}", value));
				}
				
			}
		});
		return c;
	}

}
