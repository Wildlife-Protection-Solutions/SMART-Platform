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
package org.wcs.smart.i2.ui.views.query.dropitem;

import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.wcs.smart.i2.internal.Messages;
import org.wcs.smart.i2.query.ListItem;
import org.wcs.smart.ui.CheckboxSelectorKeyAdapter;
import org.wcs.smart.ui.SmartStyledTitleDialog;
import org.wcs.smart.ui.properties.DialogConstants;

/**
 * Dialog for filtering group by drop items
 * 
 * @author Emily
 *
 */
public class OptionDialog extends SmartStyledTitleDialog {
	
	private Composite main = null;
	private CheckboxTableViewer viewer;
	private IGroupByDropItem dropItem;
	
	private ListItem[] selectedItems;
	private List<ListItem> allItems;
	private Job getInputJob;
	
	/**
	 * Creates new dialog
	 * @param parent
	 */
	public OptionDialog(Shell parent) {
		super(parent);
	}

	
	/**
	 * @param dropItem group by drop item
	 * @param defaultSelection default selection; can be null if everything is to be selected
	 */
	public void setGroupByItem(IGroupByDropItem dropItem, List<ListItem> defaultSelection) {
		this.dropItem = dropItem;
		if (defaultSelection != null){
			selectedItems = defaultSelection.toArray(new ListItem[defaultSelection.size()]);
		}else{
			selectedItems = new ListItem[0];
		}
	}
	
	/**
	 * 
	 * @see org.eclipse.jface.dialogs.Dialog#buttonPressed(int)
	 */
	@Override
	protected void buttonPressed(int buttonId) {
		if(buttonId == IDialogConstants.OK_ID){
			if (viewer.getCheckedElements().length == 0){
				MessageDialog.openError(getShell(), Messages.OptionDialog_GroupByOptions, Messages.OptionDialog_ItemRequired);
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
	 * @return the list items selected by the user or null
	 * if all items selected
	 */
	public ListItem[] getSelectedItems(){
		return this.selectedItems;
	}
	
	/**
	 * @return a list of all the options displayed to the user
	 */
	public List<ListItem> getAllOptions(){
		return this.allItems;
	}
	
	@Override
	protected Control createDialogArea(Composite parent) {
		main = new Composite(parent, SWT.NONE);
		GridLayout gl = new GridLayout(1, false);
		main.setLayout(gl);
		main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		viewer = CheckboxTableViewer.newCheckList(main, SWT.BORDER | SWT.MULTI);
		viewer.setLabelProvider(new LabelProvider() {
			public String getText(Object element) {
				if (element instanceof ListItem) return ((ListItem)element).getName();
				return super.getText(element);
			}
		});
		viewer.setContentProvider(ArrayContentProvider.getInstance());
		viewer.setInput(new String[]{DialogConstants.LOADING_TEXT});
		viewer.getControl().setEnabled(false);
		viewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		((GridData)viewer.getControl().getLayoutData()).heightHint = 300;
		viewer.getControl().addKeyListener(new CheckboxSelectorKeyAdapter(viewer));
		
		Composite links = new Composite(main, SWT.NONE);
		links.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		gl = new GridLayout(3,false);
		gl.marginBottom=gl.marginTop=gl.marginHeight=gl.marginLeft=gl.marginRight=gl.marginWidth = 0;
		links.setLayout(gl);
		
		Link lnkSelectAll = new Link(links,SWT.NONE);
		lnkSelectAll.setText("<a>" + Messages.OptionDialog_AllItems + "</a>"); //$NON-NLS-1$ //$NON-NLS-2$ 
		lnkSelectAll.addListener (SWT.Selection, new Listener () {
			public void handleEvent(Event event) {
				viewer.setAllChecked(true);
			}
		});
		Label lbl = new Label(links, SWT.SEPARATOR | SWT.VERTICAL);
		GridData gd = new GridData(SWT.FILL, SWT.CENTER, false, false);
		gd.heightHint = lnkSelectAll.computeSize(SWT.DEFAULT, SWT.DEFAULT).y;
		lbl.setLayoutData(gd);
		Link lnkDeSelectAll = new Link(links,SWT.NONE);
		lnkDeSelectAll.setText("<a>" + Messages.OptionDialog_NoItems + "</a>"); //$NON-NLS-1$ //$NON-NLS-2$ 
		lnkDeSelectAll.addListener (SWT.Selection, new Listener () {
			public void handleEvent(Event event) {
				viewer.setAllChecked(false);
			}
		});
		
		getInputJob = new Job(Messages.OptionDialog_Loading) {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				allItems = dropItem.getListOptions();
				if (monitor.isCanceled()){
					return Status.OK_STATUS;
				}
				if (viewer != null){
					if (getShell() == null || getShell().isDisposed() ) return Status.OK_STATUS;
					getShell().getDisplay().syncExec(new Runnable() {
						@Override
						public void run() {
							viewer.setInput(allItems.toArray());
							viewer.refresh();	
							if (selectedItems.length == 0){
								viewer.setAllChecked(true);
							}else{
								viewer.setCheckedElements(selectedItems);
							}
							viewer.getControl().setEnabled(true);
						}
					});
				}
				getInputJob = null;
				return Status.OK_STATUS;
			}
		};
		getInputJob.schedule();
		this.getShell().addDisposeListener(new DisposeListener() {
			@Override
			public void widgetDisposed(DisposeEvent e) {
				if (getInputJob != null){
					getInputJob.cancel();
				}
				
			}
		});
		
		setMessage(Messages.OptionDialog_Message);
		String title = Messages.OptionDialog_Title;
		setTitle(title);
		getShell().setText(title);
		return main;
		
	}
	
	@Override
	public boolean isResizable(){
		return true;
	}
}