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
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.hibernate.Session;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.intelligence.query.filter.IntelligenceFilterOption;
import org.wcs.smart.intelligence.query.internal.Messages;
import org.wcs.smart.patrol.PatrolHibernateManager;
import org.wcs.smart.query.model.filter.Operator;
import org.wcs.smart.query.ui.model.DropItem;
import org.wcs.smart.query.ui.model.ListItem;

/**
 * Combination List/Text filter option used for 
 * Patrol IDs so users can type or select
 * from a list.
 * 
 * @author Emily
 *
 */
public class TextListFilterDropItem  extends DropItem{

	public IntelligenceFilterOption filter;

	private String currentSelection;
	private String currentOp = null;	
	private Label lblAttribute;
	protected ComboViewer listViewer;
	private Combo operators;

	private Font smallerFont;
	private boolean isInit = false;
	
	private Job loadItemsJob = new Job(Messages.TextListFilterDropItem_JobName){

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			final List<ListItem> items = new ArrayList<ListItem>();
			Session s = HibernateManager.openSession();
			try{
				 if (filter == IntelligenceFilterOption.PATROLID){
					List<String> pids =  PatrolHibernateManager.getPatrolIds(s);
					for (String i : pids){
						items.add(new ListItem(null, i, i));
					}
				}
			}finally{
				s.close();
			}
			
			Display.getDefault().syncExec(new Runnable(){
				@Override
				public void run() {
					if (listViewer.getControl().isDisposed()) return;
					
					isInit = true;
					try{
						listViewer.setInput(items);
						if (currentSelection != null){
							listViewer.getCombo().setText(currentSelection);
						}
					}finally{
						isInit = false;
					}
				}});
			return Status.OK_STATUS;
		}
		
	};
	
	public TextListFilterDropItem(IntelligenceFilterOption filter){
		if (filter != IntelligenceFilterOption.PATROLID) {
			throw new IllegalStateException("Intelligence filter " + filter.getKey() + " not listtext type."); //$NON-NLS-1$ //$NON-NLS-2$
		}
		this.filter = filter;
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
		for (Operator op : Operator.STRING_OPS){
			if (op.getGuiValue().equals(currentOp)){
				sb.append(op.asSmartValue());
				break;
			}
		}
		sb.append(" "); //$NON-NLS-1$
		sb.append("\""); //$NON-NLS-1$
		sb.append(listViewer.getCombo().getText());
		sb.append("\""); //$NON-NLS-1$
		
		return sb.toString();
	}
	@Override
	public void initializeData(Object data) {
		if (data == null || !(data instanceof Object[])) return;
		
		if (data != null && data instanceof Object[]){
			Object[] initd = (Object[])data;
			this.currentOp = ((Operator)initd[0]).asSmartValue();
			try {
				this.currentSelection = (String) initd[1]; 
			} catch (Exception e) {
				throw new RuntimeException("Invalid uuid '" + initd[1] + "'", e); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}
	}
	
	@Override
	public void dispose(){
		smallerFont.dispose();
		super.dispose();
	}
	
	
	@Override
	protected void createComposite(Composite parent) {
		Composite main = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout(4, false);
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		main.setLayout(layout);
		main.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, false, true));
		
		lblAttribute = new Label(main, SWT.NONE);
	
		operators = new Combo(main, SWT.DROP_DOWN | SWT.READ_ONLY);
		operators.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				if (currentOp != null
						&& currentOp.equals(operators.getText())) {
					// no change
				} else {
					currentOp = operators.getText();
					queryChanged();
				}
			}
		});
		
		FontData fd = (operators.getFont().getFontData()[0]);
		fd.setHeight(fd.getHeight() - 1);
		smallerFont = new Font(Display.getCurrent(), fd);
		operators.setFont(smallerFont);

		listViewer = new ComboViewer(main, SWT.DROP_DOWN | SWT.BORDER );
		listViewer.getCombo().setFont(smallerFont);
		listViewer.setContentProvider(ArrayContentProvider.getInstance());
		listViewer.setLabelProvider(ListItem.createLabelProvider());
		
		listViewer.setInput(new ListItem[]{new ListItem(Messages.TextListFilterDropItem_Loading)});
		if (currentSelection != null){
			listViewer.getCombo().setText(currentSelection);
		}
		
		listViewer.getCombo().addListener(SWT.Modify, new Listener(){
			@Override
			public void handleEvent(Event event) {
				if (isInit) return;
				queryChanged();
			}});
		listViewer.addPostSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				ListItem newSelection = (ListItem) ((IStructuredSelection)listViewer.getSelection()).getFirstElement();
				String lastSelection = currentSelection;
				
				currentSelection = newSelection.getKey();
				if (! (lastSelection != null && lastSelection.equals(newSelection))){
					queryChanged();	
				}
			}
		});
		
		GridData gd = new GridData();
		gd.minimumWidth = 50;
		gd.widthHint = 100;
		listViewer.getControl().setLayoutData(gd);

		Operator[] options = Operator.STRING_OPS;
		int index = 01;
		for (int i = 0; i < options.length; i++) {
			operators.add(options[i].getGuiValue());
			if (currentOp != null
					&& currentOp.equals(options[i].getGuiValue())) {
				index = i;
			}
		}
		operators.select(index);
		
		initDrag(main);
		initDrag(lblAttribute);
		
		lblAttribute.setText(formatStringForLabel(getText()));
		
		loadItemsJob.schedule();
	}
	
}