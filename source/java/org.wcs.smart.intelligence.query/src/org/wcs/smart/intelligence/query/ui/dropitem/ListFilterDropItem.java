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
package org.wcs.smart.intelligence.query.ui.dropitem;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.intelligence.model.Informant;
import org.wcs.smart.intelligence.model.IntelligenceSource;
import org.wcs.smart.intelligence.query.filter.IntelligenceFilterOption;
import org.wcs.smart.intelligence.query.internal.Messages;
import org.wcs.smart.query.model.filter.Operator;
import org.wcs.smart.query.ui.model.DropItem;
import org.wcs.smart.query.ui.model.ListItem;

/**
 * Drop item for list filters.  This is used for Informant ID
 * and Source options.
 * 
 * @author Emily
 *
 */
public class ListFilterDropItem extends DropItem{

	public IntelligenceFilterOption filter;
	
	protected Label lblAttribute;
	protected ComboViewer listViewer;
	protected Font smallerFont;
	
	private ListItem currentSelection;
	
	private Job loadItemsJob = new Job(Messages.ListFilterDropItem_loadingListJobName){

		@SuppressWarnings("unchecked")
		@Override
		protected IStatus run(IProgressMonitor monitor) {
			final List<ListItem> items = new ArrayList<ListItem>();
			Session s = HibernateManager.openSession();
			try{
				if (filter == IntelligenceFilterOption.INFORMANTID){ 
					List<Informant> temp = null;
					if (SmartDB.isMultipleAnalysis()){
						temp = s.createCriteria(Informant.class)
								.add(Restrictions.in("conservationArea", SmartDB.getConservationAreaConfiguration().getConservationAreas())) //$NON-NLS-1$
								.addOrder(Order.desc("isActive")) //$NON-NLS-1$
								.addOrder(Order.asc("id")).list(); //$NON-NLS-1$
					}else{
						temp = s.createCriteria(Informant.class)
							.add(Restrictions.eq("conservationArea", SmartDB.getCurrentConservationArea())) //$NON-NLS-1$
							.addOrder(Order.desc("isActive")) //$NON-NLS-1$
							.addOrder(Order.asc("id")).list(); //$NON-NLS-1$
					}
					for (Informant i : temp){
						items.add(new ListItem(null, i.getId(), i.getId()));
					}
				}else if (filter == IntelligenceFilterOption.SOURCE){
					if (SmartDB.isMultipleAnalysis()){
						List<IntelligenceSource> temp = s.createCriteria(IntelligenceSource.class)
								.add(Restrictions.in("conservationArea", SmartDB.getConservationAreaConfiguration().getConservationAreas())) //$NON-NLS-1$
								.addOrder(Order.asc("name")).list(); //$NON-NLS-1$
						HashSet<String> keys = new HashSet<String>();
						for (IntelligenceSource i : temp){
							if (!keys.contains(i.getKeyId())){
								items.add(new ListItem(null, i.getName(), i.getKeyId()));
								keys.add(i.getKeyId());
							}
						}
					}else{
						List<IntelligenceSource> temp = s.createCriteria(IntelligenceSource.class)
							.add(Restrictions.eq("conservationArea", SmartDB.getCurrentConservationArea())) //$NON-NLS-1$
							.addOrder(Order.asc("name")).list(); //$NON-NLS-1$
						for (IntelligenceSource i : temp){
							items.add(new ListItem(null, i.getName(), i.getKeyId()));
						}
					}
				}
			}finally{
				s.close();
			}
			
			Display.getDefault().syncExec(new Runnable(){
				@Override
				public void run() {
					if (listViewer.getControl().isDisposed()) return;
					listViewer.setInput(items);
					if (currentSelection != null){
						listViewer.setSelection(new StructuredSelection(currentSelection));
					}
					listViewer.refresh();
				}});
			return Status.OK_STATUS;
		}
		
	};
	public ListFilterDropItem(IntelligenceFilterOption filter){
		if (filter != IntelligenceFilterOption.INFORMANTID &&
			filter != IntelligenceFilterOption.SOURCE){
			throw new IllegalStateException("Intelligence filter " + filter.getKey() + " not list type."); //$NON-NLS-1$ //$NON-NLS-2$
		}
		this.filter = filter;
	}
	
	@Override
	public void dispose(){
		smallerFont.dispose();
		super.dispose();
	}
	
	@Override
	public String getText() {
		return filter.getGuiName(Locale.getDefault());
	}

	@Override
	public String asQueryPart() {
		StringBuilder sb = new StringBuilder();
		sb.append(filter.getKey());
		sb.append(" "); //$NON-NLS-1$
		sb.append(Operator.STR_EQUALS.asSmartValue());
		sb.append(" \""); //$NON-NLS-1$
		
		ListItem it = null;
		if (currentSelection != null){
			it = currentSelection;
		}else{
			IStructuredSelection sel = (IStructuredSelection) listViewer.getSelection();
			if (sel != null && !sel.isEmpty()){
				it = (ListItem) sel.getFirstElement();
			}
		}
		if (it != null){
			if (it.getUuid() != null){			
				sb.append(it.getUuid());
			}else if (it.getKey() != null){ 
				sb.append(it.getKey());
			}
		}
		sb.append("\""); //$NON-NLS-1$
		return sb.toString();
	}

	@Override
	public void initializeData(Object data) {
		if (data == null || !(data instanceof Object[])) return;
		Object[] opString = (Object[])data;
		if (opString[1] instanceof String){
			currentSelection = new ListItem(null, null, (String)opString[1]);
		}
	}

	@Override
	protected void createComposite(Composite parent) {
		Composite main = new Composite(parent, SWT.NONE);
		GridLayout gl = new GridLayout(2, false);
		gl.marginTop = 0;
		gl.marginBottom = 0;
		gl.marginWidth = 0;
		gl.marginHeight = 0;
		
		main.setLayout(gl);
		main.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, false, true));
		
		lblAttribute = new Label(main, SWT.NONE);

		listViewer = new ComboViewer(main, SWT.DROP_DOWN | SWT.BORDER | SWT.READ_ONLY);
		
		FontData fd = (listViewer.getCombo().getFont().getFontData()[0]);
		fd.setHeight(fd.getHeight() - 1);
		smallerFont = new Font(Display.getCurrent(), fd);
		listViewer.getCombo().setFont(smallerFont);
		listViewer.setContentProvider(ArrayContentProvider.getInstance());
		listViewer.setLabelProvider(ListItem.createLabelProvider());
		
		listViewer.addPostSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				ListItem newSelection = (ListItem) ((IStructuredSelection)listViewer.getSelection()).getFirstElement();
				ListItem lastSelection = currentSelection;
				
				currentSelection = newSelection;
				if (! (lastSelection != null && lastSelection.equals(newSelection))){
					queryChanged();	
				}
			}
		});
		listViewer.setInput(new ListItem[]{new ListItem(Messages.ListFilterDropItem_Loading)});
		
		initDrag(main);
		initDrag(lblAttribute);
		
		lblAttribute.setText(formatStringForLabel(getText() + " = ")); //$NON-NLS-1$
		loadItemsJob.schedule();
	}

}
