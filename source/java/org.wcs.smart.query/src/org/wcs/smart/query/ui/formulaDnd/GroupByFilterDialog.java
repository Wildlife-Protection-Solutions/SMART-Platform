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

import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.wcs.smart.query.model.ListItem;

/**
 * Dialog for selecting items group by items
 * to include the query.
 * 
 * 
 * @author egouge
 * @since 1.0.0
 */
public class GroupByFilterDialog extends TitleAreaDialog{

	private Composite main = null;
	private CheckboxTableViewer viewer;
	private IGroupByDropItem dropItem;
	
	private ListItem[] selectedItems;
	
	/**
	 * Creates new dialog
	 * @param parent
	 */
	public GroupByFilterDialog(Shell parent) {
		super(parent);
	}

	
	/**
	 * @param dropItem group by drop item
	 * @param defaultSelection default selection
	 */
	public void setGroupByItem(final IGroupByDropItem dropItem, List<ListItem> defaultSelection) {
		this.dropItem = dropItem;
		selectedItems = defaultSelection.toArray(new ListItem[defaultSelection.size()]);
	}
	
	/**
	 * 
	 * @see org.eclipse.jface.dialogs.Dialog#buttonPressed(int)
	 */
	@Override
	protected void buttonPressed(int buttonId) {
		if(buttonId == IDialogConstants.OK_ID){
			if (viewer.getCheckedElements().length == 0){
				MessageDialog.openError(getShell(), "Error", "At least one item must be selected.");
				return;
			}
		}
		if (viewer.getCheckedElements().length == viewer.getTable().getItemCount()){
			selectedItems = null;
		}else{
			Object[] data = viewer.getCheckedElements();
			selectedItems = new ListItem[data.length];
			for (int i = 0; i < data.length; i++){
				selectedItems[i] = (ListItem) data[i];
			}
		}
		super.buttonPressed(buttonId);
	}
	
	/**
	 * @return the list items selected by the user
	 */
	public ListItem[] getSelectedItems(){
		return this.selectedItems;
	}
	
	@Override
	protected Control createDialogArea(Composite parent) {
		main = new Composite(parent, SWT.NONE);
		GridLayout gl = new GridLayout(1, false);
		main.setLayout(gl);
		
		viewer = CheckboxTableViewer.newCheckList(parent, SWT.BORDER);
		viewer.setLabelProvider(new LabelProvider(){
			/**
			 * The <code>LabelProvider</code> implementation of this
			 * <code>ILabelProvider</code> method returns the element's
			 * <code>toString</code> string. Subclasses may override.
			 */
			public String getText(Object element) {
				if (element instanceof ListItem){
					return ((ListItem) element).getName();
				}
				return super.getText(element);
			}
		});
		viewer.setContentProvider(ArrayContentProvider.getInstance());
		viewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		
		
		Job getInput = new Job("Loading list items.") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				// TODO Auto-generated method stub
				final List<ListItem> items = dropItem.getListItem();
				if (viewer != null){
					GroupByFilterDialog.this.getShell().getDisplay().asyncExec(new Runnable() {
						@Override
						public void run() {
							viewer.setInput(items.toArray());
							viewer.refresh();	
							if (selectedItems.length == 0){
								viewer.setAllChecked(true);
							}else{
								viewer.setCheckedElements(selectedItems);
							}
						}
					});
				}
				return Status.OK_STATUS;
			}
		};
		getInput.schedule();
		
		setMessage("Select the items to include");
		setTitle("Group By Filters");
		getShell().setText("Group By Filter");
		return main;
		
	}
	
	@Override
	public boolean isResizable(){
		return true;
	}

}