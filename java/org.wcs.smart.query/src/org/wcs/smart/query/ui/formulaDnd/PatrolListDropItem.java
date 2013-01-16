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
package org.wcs.smart.query.ui.formulaDnd;

import java.util.Collections;
import java.util.List;

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
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.query.QueryPlugIn;
import org.wcs.smart.query.internal.Messages;
import org.wcs.smart.query.model.ListItem;
import org.wcs.smart.query.parser.PatrolQueryOptions.PatrolQueryOption;
import org.wcs.smart.util.SmartUtils;

/**
 * Patrol drop item for a patrol option 
 * that contains a list of options (for example
 * station list, team list etc).
 * 
 * @author Emily
 * @since 1.0.0
 */
public class PatrolListDropItem extends DropItem{

	private String keyPart;
	private String text;
	private PatrolQueryOption option;
	
	private ComboViewer listViewer;
	private Font smallerFont = null;
	
	private ListItem currentSelection = null;
	
	/*
	 * job for loading options
	 */
	private Job loadItemsJobs = new Job(Messages.PatrolListDropItem_LoadingJobName){

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			
			Session s = HibernateManager.openSession();
			s.beginTransaction();
			try{
				final List<ListItem> items = option.getAllActiveValues(s);
				Collections.sort(items);
				Display.getDefault().asyncExec(new Runnable(){
					@Override
					public void run() {
						if (listViewer.getCombo().isDisposed()) return;
						listViewer.setInput(items.toArray(new ListItem[items.size()]));
						if (currentSelection != null){
							listViewer.setSelection(new StructuredSelection(currentSelection));
						}
						targetPanel.layout();
					}});
			}catch (Exception ex){
				QueryPlugIn.displayLog(Messages.PatrolListDropItem_ErrorLoadingItems, ex);
			}finally{
				s.getTransaction().rollback();
				s.close();
			}
			return Status.OK_STATUS;
		}};
	private Label lbl;
		
		
	/**
	 * Creates a new patrol list drop item
	 *  
	 * @param parent parent
	 * @param target target item
	 * @param option patrol filter option
	 */
	public PatrolListDropItem(PatrolQueryOption option) {
		this.keyPart = "patrol:" + option.getKey(); //$NON-NLS-1$
		this.text = option.getGuiName();
		this.option = option;
	}

	
	public void dispose(){
		super.dispose();
		loadItemsJobs.cancel();
		if (smallerFont != null){
			smallerFont.dispose();
		}
	}
	
	/**
	 * @see org.wcs.smart.query.ui.formulaDnd.DropItem#getText()
	 */
	@Override
	public String getText() {
		StringBuilder sb = new StringBuilder(this.text);
		sb.append(" = "); //$NON-NLS-1$
		if (listViewer.getSelection() != null){
			ListItem it = (ListItem)((IStructuredSelection)listViewer.getSelection()).getFirstElement();
			if (it != null){
				sb.append("\""); //$NON-NLS-1$
				sb.append(it.getName());
				sb.append("\""); //$NON-NLS-1$
			}
		}
		return sb.toString();
	}

	/**
	 * @see org.wcs.smart.query.ui.formulaDnd.DropItem#asQueryPart()
	 */
	@Override
	public String asQueryPart() {
		StringBuilder sb = new StringBuilder(this.keyPart);
		sb.append(" equals "); //$NON-NLS-1$
		ListItem it = null;
		
		
		if (currentSelection != null){
			it = currentSelection;
		}else if (listViewer.getSelection() != null){ 
			it = (ListItem)((IStructuredSelection)listViewer.getSelection()).getFirstElement();
		}
		if (it != null){
			sb.append("\""); //$NON-NLS-1$
			if (option == PatrolQueryOption.PATROL_TYPE){
				sb.append(it.getKey().toUpperCase());
			}else{
				if (it.getUuid() != null){
					sb.append( SmartUtils.encodeHex(it.getUuid()));
				}
			}
			sb.append("\""); //$NON-NLS-1$
		}
		return sb.toString();
	}

	/**
	 * @see org.wcs.smart.query.ui.formulaDnd.DropItem#createComposite(org.eclipse.swt.widgets.Composite)
	 */
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
		
		lbl = new Label(main, SWT.NONE);
		
		
		listViewer = new ComboViewer(main, SWT.DROP_DOWN | SWT.BORDER | SWT.READ_ONLY);

		FontData fd = (listViewer.getCombo().getFont().getFontData()[0]);
		fd.setHeight(fd.getHeight() - 1);
		smallerFont = new Font(Display.getCurrent(), fd);
		listViewer.getCombo().setFont(smallerFont);
		listViewer.setContentProvider(ArrayContentProvider.getInstance());
		
		listViewer.setLabelProvider(ListItem.createLabelProvider());
		listViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				ListItem newSelection =  (ListItem) ((IStructuredSelection)listViewer.getSelection()).getFirstElement();
				if (currentSelection != null && newSelection.equals(currentSelection)){
					//no change
				}else{
					currentSelection = newSelection;
					PatrolListDropItem.this.queryChanged();
					targetPanel.layout();
				}
			}
		});
		
		listViewer.setInput(new ListItem[]{new ListItem(null, Messages.PatrolListDropItem_LoadingLabel)});
		
		initDrag(main);
		initDrag(lbl);

		lbl.setText( formatStringForLabel(this.text + " = "));	 //$NON-NLS-1$
		loadItemsJobs.schedule();
	}


	
	/**
	 * @param data a list item
	 */
	@Override
	public void initializeData(Object data) {
		currentSelection = (ListItem)data;
	}
	
	/**
	 * @see org.wcs.smart.query.ui.formulaDnd.DropItem#isValueItem()
	 */
	@Override
	public boolean isValueItem(){
		return false;
	}
	
	/**
	 * @see org.wcs.smart.query.ui.formulaDnd.DropItem#isFilterItem()
	 */
	@Override
	public boolean isFilterItem(){
		return true;
	}

	/**
	 * @see org.wcs.smart.query.ui.formulaDnd.DropItem#isGroupByItem()
	 */
	@Override
	public boolean isGroupByItem(){
		return false;
	}
}
