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
import java.util.List;
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
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.i2.IntelAnalystUserLevel;
import org.wcs.smart.i2.Intelligence2PlugIn;
import org.wcs.smart.i2.model.IntelAttribute;
import org.wcs.smart.i2.model.IntelEntityType;
import org.wcs.smart.i2.model.IntelRelationshipType;
import org.wcs.smart.i2.search.SearchDataGenerator;
import org.wcs.smart.i2.search.SearchManager;
import org.wcs.smart.user.UserLevelManager;

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
		if (!UserLevelManager.INSTANCE.supportsUser(SmartDB.getCurrentEmployee(), IntelAnalystUserLevel.INSTANCE)){
			Label l = new Label(parent, SWT.NONE);
			l.setText(MessageFormat.format("Only {0} users can access this page.", IntelAnalystUserLevel.INSTANCE.getGuiName(Locale.getDefault())));
			return l;
		}
		Composite c = new Composite(parent, SWT.NONE);
		c.setLayout(new GridLayout());
		((GridLayout)c.getLayout()).marginWidth = 0;
		((GridLayout)c.getLayout()).marginHeight = 0;
		
		
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
		
		/* - SECTION BELOW GENERATES RANDOM TEST DATA -*/
		/*
		g = new Group(c, SWT.NONE);
		g.setText("Random Sample Data Generation");
		g.setLayout(new GridLayout());
		g.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		l = new Label(g, SWT.WRAP);
		l.setText("This generates a collection of sample data in your database.  This is only for TESTING.  It does not remove any existing data, but will add data with randomly generated attribute values. Restart application after generating data.");
		l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		((GridData)l.getLayoutData()).widthHint = 200;

		// ------------ ATTRIBUTES ----------------
		Group attg = new Group(g, SWT.NONE);
		attg.setLayout(new GridLayout(2, false));
		attg.setText("Attributes");
		attg.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		l = new Label(attg, SWT.NONE);
		l.setText("Number of Attributes:");
		
		final Text txtNumAttribute = new Text(attg, SWT.BORDER);
		txtNumAttribute.setText("1000");
		txtNumAttribute.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		l = new Label(attg, SWT.NONE);
		l.setText("Number of List Items Per Attribute:");
		
		final Text txtNumListItems = new Text(attg, SWT.BORDER);
		txtNumListItems.setText("50");
		txtNumListItems.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		Button btnAttributes = new Button(attg, SWT.PUSH);
		btnAttributes.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 2, 1));
		btnAttributes.setText("Generate Attributes...");
		btnAttributes.addSelectionListener(new SelectionAdapter(){
			@Override
			public void widgetSelected(SelectionEvent e) {
				final Integer v1 = Integer.valueOf(txtNumAttribute.getText());
				final Integer v2 = Integer.valueOf(txtNumListItems.getText());
				ProgressMonitorDialog pmd = new ProgressMonitorDialog(getShell());
				try{
				pmd.run(true, false, new IRunnableWithProgress() {
					
					@Override
					public void run(IProgressMonitor monitor) throws InvocationTargetException,
							InterruptedException {
						
						Session s = HibernateManager.openSession();
						s.beginTransaction();
						try{
							SearchDataGenerator.generateAttribute(v1, v2, monitor, s);
							s.getTransaction().commit();
						}catch (Exception ex){
							s.getTransaction().rollback();
							throw new InvocationTargetException(ex);
						}finally{
							s.close();
						}
						
					}
				});
				}catch (Exception ex){
					Intelligence2PlugIn.displayLog(ex.getMessage(), ex);
				}
			}
		});
		
		// ------------ ENTITY TYPES ----------------
		Group etg = new Group(g, SWT.NONE);
		etg.setLayout(new GridLayout(2, false));
		etg.setText("Entity Types");
		etg.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		l = new Label(etg, SWT.NONE);
		l.setText("Number of Entity Types:");
				
		final Text txtNumTypes = new Text(etg, SWT.BORDER);
		txtNumTypes.setText("100");
		txtNumTypes.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		l = new Label(etg, SWT.NONE);
		l.setText("Number of Attributes Per Entity Type:");
				
		final Text txtAttributePerTypes = new Text(etg, SWT.BORDER);
		txtAttributePerTypes.setText("50");
		txtAttributePerTypes.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		Button btnEntitiesTypes = new Button(etg, SWT.PUSH);
		btnEntitiesTypes.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 2, 1));
		btnEntitiesTypes.setText("Generate Entity Types...");
		btnEntitiesTypes.addSelectionListener(new SelectionAdapter(){
			@Override
			public void widgetSelected(SelectionEvent e) {
				final int v1 = Integer.valueOf(txtNumTypes.getText());
				final int v2 = Integer.valueOf(txtAttributePerTypes.getText());
				ProgressMonitorDialog pmd = new ProgressMonitorDialog(getShell());
				try{
				pmd.run(true, false, new IRunnableWithProgress() {
						
					@Override
					public void run(IProgressMonitor monitor) throws InvocationTargetException,
							InterruptedException {
						Session s = HibernateManager.openSession();
						s.beginTransaction();
						try{
							List<IntelAttribute> attributes = s.createCriteria(IntelAttribute.class).add(Restrictions.eq("conservationArea", SmartDB.getCurrentConservationArea())).list();
							SearchDataGenerator.generateEntityTypes(v1,v2, attributes, monitor, s);
							s.getTransaction().commit();
						}catch (Exception ex){
							s.getTransaction().rollback();
							throw new InvocationTargetException(ex);
						}finally{
							s.close();
						}			
					}
				});
			}catch (Exception ex){
				Intelligence2PlugIn.displayLog(ex.getMessage(), ex);
			}
		}
		});
				
		// ------------ RELATIONSHIPS ----------------
		Group rtg = new Group(g, SWT.NONE);
		rtg.setLayout(new GridLayout(2, false));
		rtg.setText("Relationship Types");
		rtg.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		l = new Label(rtg, SWT.NONE);
		l.setText("Number of Relationship Groups:");
		
		final Text txtNumRGroups = new Text(rtg, SWT.BORDER);
		txtNumRGroups.setText("50");
		txtNumRGroups.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		l = new Label(rtg, SWT.NONE);
		l.setText("Number of Relationship Types:");
		
		final Text txtNumRTypes = new Text(rtg, SWT.BORDER);
		txtNumRTypes.setText("200");
		txtNumRTypes.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		l = new Label(rtg, SWT.NONE);
		l.setText("Number of Attributes Per Relationship Types:");
		
		final Text txtNumListPerR = new Text(rtg, SWT.BORDER);
		txtNumListPerR.setText("50");
		txtNumListPerR.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		Button btnRelationships = new Button(rtg, SWT.PUSH);
		btnRelationships.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 2, 1));
		btnRelationships.setText("Generate Relationship Types...");
		btnRelationships.addSelectionListener(new SelectionAdapter(){
			@Override
			public void widgetSelected(SelectionEvent e) {
				final int v1 = Integer.valueOf(txtNumRGroups.getText());
				final int v2 = Integer.valueOf(txtNumRTypes.getText());
				final int v3 = Integer.valueOf(txtNumListPerR.getText());
				ProgressMonitorDialog pmd = new ProgressMonitorDialog(getShell());
				try{
				pmd.run(true, false, new IRunnableWithProgress() {
					
					@Override
					public void run(IProgressMonitor monitor) throws InvocationTargetException,
							InterruptedException {
						Session s = HibernateManager.openSession();
						s.beginTransaction();
						try{
							List<IntelAttribute> attributes = s.createCriteria(IntelAttribute.class).add(Restrictions.eq("conservationArea", SmartDB.getCurrentConservationArea())).list();
							List<IntelEntityType> types = s.createCriteria(IntelEntityType.class).add(Restrictions.eq("conservationArea", SmartDB.getCurrentConservationArea())).list();
							SearchDataGenerator.generateRelationshipTypes(v1, v2, v3, attributes, types, monitor, s);	
							s.getTransaction().commit();
						}catch (Exception ex){
							s.getTransaction().rollback();
							throw new InvocationTargetException(ex);
						}finally{
							s.close();
						}
						
					}
				});
				}catch (Exception ex){
					Intelligence2PlugIn.displayLog(ex.getMessage(), ex);
				}
			}
		});
		
		// ----------ENTITIES ----------------
		Group eg = new Group(g, SWT.NONE);
		eg.setLayout(new GridLayout(2, false));
		eg.setText("Entities");
		eg.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		l = new Label(eg, SWT.NONE);
		l.setText("Number of Entities:");
				
		final Text txtNumEntities = new Text(eg, SWT.BORDER);
		txtNumEntities.setText("500");
		txtNumEntities.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		Button btnEntities = new Button(eg, SWT.PUSH);
		btnEntities.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 2, 1));
		btnEntities.setText("Generate Entities...");
		btnEntities.addSelectionListener(new SelectionAdapter(){
			@Override
			public void widgetSelected(SelectionEvent e) {
				final int v1 = Integer.valueOf(txtNumEntities.getText());
				ProgressMonitorDialog pmd = new ProgressMonitorDialog(getShell());
				try{
				pmd.run(true, false, new IRunnableWithProgress() {
							
					@Override
					public void run(IProgressMonitor monitor) throws InvocationTargetException,
							InterruptedException {
						Session s = HibernateManager.openSession();
						s.beginTransaction();
						try{
							List<IntelRelationshipType> relationshipTypes = s.createCriteria(IntelRelationshipType.class).add(Restrictions.eq("conservationArea", SmartDB.getCurrentConservationArea())).list();
							List<IntelEntityType> types = s.createCriteria(IntelEntityType.class).add(Restrictions.eq("conservationArea", SmartDB.getCurrentConservationArea())).list();
							SearchDataGenerator.generateEntities(v1, types, relationshipTypes, monitor, s);		
							s.getTransaction().commit();
						}catch (Exception ex){
							s.getTransaction().rollback();
							throw new InvocationTargetException(ex);
						}finally{
							s.close();
						}
						
					}
				});
				}catch (Exception ex){
					Intelligence2PlugIn.displayLog(ex.getMessage(), ex);
				}
			}
		});	
		
		// ---------- RECORD  ----------------
		Group recordg = new Group(g, SWT.NONE);
		recordg.setLayout(new GridLayout(2, false));
		recordg.setText("Records");
		recordg.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		l = new Label(recordg, SWT.NONE);
		l.setText("Number of Record:");
						
		final Text txtNumRecords = new Text(recordg, SWT.BORDER);
		txtNumRecords.setText("500");
		txtNumRecords.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
				
		Button btnRecords = new Button(recordg, SWT.PUSH);
		btnRecords.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 2, 1));
		btnRecords.setText("Generate Record...");
		btnRecords.addSelectionListener(new SelectionAdapter(){
			@Override
			public void widgetSelected(SelectionEvent e) {
				final int v1 = Integer.valueOf(txtNumRecords.getText());
				ProgressMonitorDialog pmd = new ProgressMonitorDialog(getShell());
				try{
				pmd.run(true, false, new IRunnableWithProgress() {
							
					@Override
					public void run(IProgressMonitor monitor) throws InvocationTargetException,InterruptedException {
						Session s = HibernateManager.openSession();
						s.beginTransaction();
						try{
							SearchDataGenerator.generateRecords(v1, monitor, s);		
							s.getTransaction().commit();
						}catch (Exception ex){
							s.getTransaction().rollback();
							throw new InvocationTargetException(ex);
						}finally{
							s.close();
						}	
					}
				});
				}catch (Exception ex){
					Intelligence2PlugIn.displayLog(ex.getMessage(), ex);
				}
			}
		});	
		*/
		return c;
	}

}
