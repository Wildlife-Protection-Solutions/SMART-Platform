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
package org.wcs.smart.intelligence.informant.editor;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.widgets.Form;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.part.EditorPart;
import org.hibernate.Session;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.intelligence.IntelligenceHibernateManager;
import org.wcs.smart.intelligence.IntelligencePlugIn;
import org.wcs.smart.intelligence.internal.Messages;
import org.wcs.smart.intelligence.model.Informant;

/**
 * Editor for viewing informant application data.
 * 
 * @author elitvin
 * @since 3.2.0
 */
public class InformantDataEditor extends EditorPart {

	public static final String ID = "org.wcs.smart.intelligence.informant.InformantDataEditor"; //$NON-NLS-1$

	private FormToolkit toolkit = new FormToolkit(Display.getCurrent());

	private TableViewer viewer;
    private List<Informant> informantList;
	
	public InformantDataEditor() {
		Session s = HibernateManager.openSession();
		try {
			informantList = IntelligenceHibernateManager.getInformants(SmartDB.getCurrentConservationArea(), s, false);
		} catch (Exception e) {
			IntelligencePlugIn.displayLog(Messages.IntelligenceSourceComposite_InformantLoad_Error, e);
			informantList = new ArrayList<Informant>();
		} finally {
			s.close();
		}
	}	
	
	@Override
	public void createPartControl(Composite parent) {
		Form form = toolkit.createForm(parent);
		form.setText("Informant Data");
		GridLayout layout = new GridLayout();
		form.getBody().setLayout(layout);
		
		Composite main = toolkit.createComposite(form.getBody());
		layout = new GridLayout();
		layout.marginHeight = layout.marginWidth = layout.horizontalSpacing = 0;
		main.setLayout(layout);
		
		viewer = new TableViewer(main, SWT.BORDER | SWT.VIRTUAL | SWT.FULL_SELECTION | SWT.MULTI);
		toolkit.paintBordersFor(viewer.getTable());
		viewer.getTable().setHeaderVisible(true);
		viewer.getTable().setLinesVisible(true);
		viewer.setContentProvider(ArrayContentProvider.getInstance());
		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		viewer.getTable().setLayoutData(gd);

		addColumns(viewer);
		viewer.setInput(informantList);
	}

	private void addColumns(TableViewer v) {
		//public data
		TableViewerColumn idColumn = new TableViewerColumn(v, SWT.NONE);
		idColumn.getColumn().setText("ID");
		idColumn.getColumn().setWidth(80);
		idColumn.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof Informant) {
					return ((Informant)element).getId();
				}
				return super.getText(element);
			}
		});

		TableViewerColumn activeColumn = new TableViewerColumn(v, SWT.NONE);
		activeColumn.getColumn().setText("Active");
		activeColumn.getColumn().setWidth(50);
		activeColumn.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof Informant) {
					return ((Informant)element).getIsActive() ? "Yes" : "No";
				}
				return super.getText(element);
			}
		});
		
		//secure data
//		for (CTPatrolTableColumn column : CTPatrolTableColumn.values()) {
//			TableViewerColumn viewerColumn = new TableViewerColumn(v, SWT.NONE);
//			viewerColumn.getColumn().setText(column.getGuiName());
//			viewerColumn.getColumn().setWidth(column.getWidth());
//			viewerColumn.setLabelProvider(new CTPatrolTableCellLabelProvider(column));
//		}
	}

	@Override
	public void setFocus() {
		viewer.getTable().setFocus();
	}
	
	@Override
	public void init(IEditorSite site, IEditorInput input) throws PartInitException {
		setSite(site);
		setInput(input);
	}

	@Override
	public void dispose(){
		super.dispose();
		if (toolkit != null){
			toolkit.dispose();
		}
	}
	
	@Override
	public boolean isDirty() {
		return false;
	}

	@Override
	public void doSave(IProgressMonitor monitor) {
		// nothing
	}

	@Override
	public void doSaveAs() {
		//not allowed
	}

	@Override
	public boolean isSaveAsAllowed() {
		return false;
	}
	
}
