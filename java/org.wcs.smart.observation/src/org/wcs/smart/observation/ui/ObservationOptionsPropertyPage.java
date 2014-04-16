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
package org.wcs.smart.observation.ui;

import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.hibernate.Session;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.Projection;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.observation.ObservationHibernateManager;
import org.wcs.smart.observation.ObservationPlugIn;
import org.wcs.smart.observation.internal.Messages;
import org.wcs.smart.observation.model.ObservationOptions;
import org.wcs.smart.ui.properties.AbstractPropertyJHeaderDialog;

/**
 * Property page for editing patrol options
 * 
 * @author Emily
 * @since 1.0.0
 */
public class ObservationOptionsPropertyPage extends AbstractPropertyJHeaderDialog {

	private ObservationOptions patrolOption = null;
	private Text txtEditTime;
	private Button btnTrackDistanceDirection;
	private ComboViewer projectionViewer;
	
	private Font boldFont;
	
	/**
	 * @param parent
	 * @param title
	 */
	public ObservationOptionsPropertyPage() {
		super(Display.getCurrent().getActiveShell(), Messages.PatrolOptionsPropertyPage_DialogTitle);
		patrolOption = ObservationHibernateManager.getPatrolOptions(SmartDB.getCurrentConservationArea(), getSession());
	}
	

	@Override
	public boolean  close(){
		return super.close();
	}
	
	/* (non-Javadoc)
	 * @see org.wcs.smart.ui.ca.properties.AbstractPropertyJHeaderDialog#createContent(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected Composite createContent(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		container.setLayout(new GridLayout(1, false));
		container.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		Group g = new Group(container, SWT.NONE);
		g.setText(Messages.PatrolOptionsPropertyPage_DistanceDirection_OpLabel);
		g.setLayout(new GridLayout(2, false));
		g.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		FontData fd = g.getFont().getFontData()[0];
		fd.setStyle(SWT.BOLD);
		boldFont = new Font(getShell().getDisplay(), fd);
		parent.addDisposeListener(new DisposeListener() {
			@Override
			public void widgetDisposed(DisposeEvent e) {
				boldFont.dispose();	
			}
		});
		g.setFont(boldFont);
		
		Label lbl = new Label(g, SWT.WRAP);
		lbl.setText(Messages.PatrolOptionsPropertyPage_DistanceDirection_DescLabel);
		lbl.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		((GridData)lbl.getLayoutData()).widthHint = 350;
		
		
		
		btnTrackDistanceDirection = new Button(g, SWT.CHECK);
		btnTrackDistanceDirection.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1));
		btnTrackDistanceDirection.addSelectionListener(new SelectionAdapter(){
			public void widgetSelected(SelectionEvent e) {
				setChangesMade(true);
			}
		});
		btnTrackDistanceDirection.setText(Messages.PatrolOptionsPropertyPage_RecordDistanceDirectory_Op);
		
		lbl = new Label(container, SWT.NONE);  //spacer
		
		g = new Group(container, SWT.NONE);
		g.setText(Messages.PatrolOptionsPropertyPage_PatrolEditOptions_Label);
		g.setLayout(new GridLayout(2, false));
		g.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		g.setFont(boldFont);
		
		lbl = new Label(g, SWT.WRAP);
		lbl.setText(Messages.PatrolOptionsPropertyPage_PatrolEditOptions_DescLabel);
		lbl.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		((GridData)lbl.getLayoutData()).widthHint = 350;
		
		final ControlDecoration cdEditTime =createDecoration(lbl);
		cdEditTime.hide();
		
		
		txtEditTime = new Text(g, SWT.BORDER);
		txtEditTime.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
		((GridData)txtEditTime.getLayoutData()).widthHint = 30;
		txtEditTime.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				setChangesMade(true);
				try{
					int edittime = Integer.parseInt(txtEditTime.getText());
					if (edittime < -1){
						cdEditTime.show();
						cdEditTime.setDescriptionText(Messages.PatrolOptionsPropertyPage_Error_EditTime);
					}else{
						cdEditTime.hide();
					}
					
				}catch (Exception ex){
					cdEditTime.show();
					cdEditTime.setDescriptionText(Messages.PatrolOptionsPropertyPage_Error_InvalidInteger);
				}
			}
		});
		lbl = new Label(g, SWT.NONE);
		lbl.setText(Messages.PatrolOptionsPropertyPage_PatrolEditOptions_DaysLabel);
		lbl.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false));

		g = new Group(container, SWT.NONE);
		g.setText(Messages.ObservationOptionsPropertyPage_ViewProjectionGroupTitle);
		g.setLayout(new GridLayout(2, false));
		g.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		g.setFont(boldFont);

		lbl = new Label(g, SWT.WRAP);
		lbl.setText(Messages.ObservationOptionsPropertyPage_ViewProjectionDescr);
		lbl.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		((GridData)lbl.getLayoutData()).widthHint = 350;

		projectionViewer = new ComboViewer(g, SWT.READ_ONLY);
		projectionViewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		projectionViewer.setLabelProvider(new LabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof Projection) {
					return ((Projection)element).getName();
				}
				return super.getText(element);
			}
		});
		projectionViewer.setContentProvider(ArrayContentProvider.getInstance());
		Session s = HibernateManager.openSession();
		s.beginTransaction();
		try{
			final Object[] ps = HibernateManager.getCaProjectionList(s).toArray();
			projectionViewer.setInput(ps);
			Projection selection = null;
			for (Object x : ps) {
				if (x instanceof Projection && x.equals(patrolOption.getViewProjection())) {
					selection = (Projection) x;
					break;
				}
			}
			if (selection == null && ps.length > 0) {
				selection = (Projection) ps[0];
			}	
			if (selection != null){
				projectionViewer.setSelection(new StructuredSelection(selection));
			}
		}catch (final Exception ex){
			SmartPlugIn.displayLog(getShell(), Messages.ObservationOptionsPropertyPage_Projection_LoadError + ex.getLocalizedMessage(), ex);							
		}finally{
			s.getTransaction().rollback();
			s.close();
		}
		projectionViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				setChangesMade(true);
			}
		});
		
		
		//init values
		if (patrolOption != null){
			btnTrackDistanceDirection.setSelection(patrolOption.getTrackDistanceDirection());
			if (patrolOption.getEditTime() != null){
				txtEditTime.setText(patrolOption.getEditTime().toString());
			}else{
				txtEditTime.setText("-1"); //$NON-NLS-1$
			}
		}
		
		setTitle(Messages.PatrolOptionsPropertyPage_PageName);
		setMessage(Messages.PatrolOptionsPropertyPage_DialogMessage);
		setChangesMade(false);
		return container;
	}

	/*
	 * Creates a control decoration for a wizard page field.
	 */
	protected ControlDecoration createDecoration(Control control){
		ControlDecoration cd = new ControlDecoration(control, SWT.LEFT | SWT.TOP);
		cd.setImage(FieldDecorationRegistry.getDefault()
				.getFieldDecoration(FieldDecorationRegistry.DEC_ERROR).getImage());
		cd.setShowHover(true);
		return cd;
	}
	
	/* (non-Javadoc)
	 * @see org.wcs.smart.ui.ca.properties.AbstractPropertyJHeaderDialog#performSave()
	 */
	@Override
	protected boolean performSave() {
		if (patrolOption == null){
			return false;
		}
		patrolOption.setTrackDistanceDirection(btnTrackDistanceDirection.getSelection());
		int edittime = -1;
		try{
			edittime = Integer.parseInt(txtEditTime.getText());
		}catch (NumberFormatException ex){
			ObservationPlugIn.displayLog(Messages.PatrolOptionsPropertyPage_Error_EditTimeNotInteger, ex);
			return false;
		}
		if (edittime < -1){
			ObservationPlugIn.displayLog(Messages.PatrolOptionsPropertyPage_Error_EditTime, null);
			return false;
		}
		patrolOption.setEditTime(edittime);
		
		Object prjSelection = ((IStructuredSelection)projectionViewer.getSelection()).getFirstElement();
		if (prjSelection instanceof Projection) {
			patrolOption.setViewProjection((Projection)prjSelection);
		}
		
		Session s = getSession();
		s.beginTransaction();
		try{
			s.saveOrUpdate(patrolOption);
			s.getTransaction().commit();
			setChangesMade(false);
			return true;
		}catch (Exception ex){
			s.getTransaction().rollback();
			s.close();
			ObservationPlugIn.displayLog(Messages.PatrolOptionsPropertyPage_Error_CouldNotSave + ex.getLocalizedMessage(), ex);
		}
		return false;
	}
	
	
}
