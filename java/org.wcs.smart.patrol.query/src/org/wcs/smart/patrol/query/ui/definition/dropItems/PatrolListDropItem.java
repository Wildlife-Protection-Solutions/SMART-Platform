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
package org.wcs.smart.patrol.query.ui.definition.dropItems;

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
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.patrol.query.internal.Messages;
import org.wcs.smart.patrol.query.model.IPatrolQueryOption;
import org.wcs.smart.patrol.query.model.PatrolQueryOption;
import org.wcs.smart.patrol.query.model.PatrolQueryOptionType;
import org.wcs.smart.patrol.query.ui.IPatrolOptionData;
import org.wcs.smart.query.QueryPlugIn;
import org.wcs.smart.query.ui.model.DropItem;
import org.wcs.smart.query.ui.model.IFilterDropItem;
import org.wcs.smart.query.ui.model.ListItem;
import org.wcs.smart.util.UuidUtils;

/**
 * Patrol drop item for a patrol option 
 * that contains a list of options (for example
 * station list, team list etc).
 * 
 * @author Emily
 * @since 1.0.0
 */
public class PatrolListDropItem extends DropItem implements IFilterDropItem{

	private String keyPart;
	private String text;
	private IPatrolQueryOption option;
	private IPatrolOptionData data;
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
				final List<ListItem> items = data.getAllActiveValues(s);
				Display.getDefault().asyncExec(new Runnable(){
					@Override
					public void run() {
						if (listViewer.getCombo().isDisposed()) return;
						if (currentSelection != null && !items.contains(currentSelection)){
							//item is not longer active; but still in query
							items.add(currentSelection);
						}
						listViewer.setInput(items.toArray(new ListItem[items.size()]));
						if (currentSelection != null){
							
							listViewer.setSelection(new StructuredSelection(currentSelection));
						}
						getTargetPanel().redraw();
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
	public PatrolListDropItem(IPatrolQueryOption option) {
		this.keyPart = "patrol:" + option.getKey(); //$NON-NLS-1$
		this.text = option.getGuiName(Locale.getDefault());
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
			}else if (option.getType() == PatrolQueryOptionType.KEY){
				sb.append(it.getKey());
			}else{
				if (it.getUuid() != null){
					sb.append( UuidUtils.uuidToString(it.getUuid()));
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
					getTargetPanel().redraw();
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
	 * This expects an array with two elements.  The first is
	 * the patrol option data providing the additional details, the
	 * second is optional and can be null or the current selection.  
	 * @param data a array {PatrolOptionData, ListItem}
	 */
	@Override
	public void initializeData(Object data) {
		Object[] values = (Object[]) data;
		this.data = (IPatrolOptionData) values[0];
		if (values.length < 2 || values[1] == null){
			this.currentSelection = this.data.getDefaultListItem();	
		}else{
			currentSelection = (ListItem)values[1];
		}
	}
	
}
