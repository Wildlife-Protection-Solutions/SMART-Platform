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
package org.wcs.smart.query.ui.model.impl;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseTrackAdapter;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.ToolTip;
import org.hibernate.Session;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.query.QueryPlugIn;
import org.wcs.smart.query.internal.Messages;
import org.wcs.smart.query.ui.model.DropItem;
import org.wcs.smart.query.ui.model.IGroupByDropItem;
import org.wcs.smart.query.ui.model.ListItem;
import org.wcs.smart.ui.SmartLabelProvider;
import org.wcs.smart.util.UuidUtils;

/**
 * Group by drop item for observer options.
 * 
 * @author Emily
 *
 */
public class ObserverGroupByDropItem extends DropItem implements IGroupByDropItem{

	private List<ListItem> selectedItems = new ArrayList<ListItem>();;
	private Font smallerFont = null;
	private ToolTip toolTip;
	private Label lblText;
	
	@Override
	public List<ListItem> getListItem() {
		List<ListItem> items = new ArrayList<ListItem>();
		
		Session s = HibernateManager.openSession();
		s.beginTransaction();
		try{
			List<Employee> e = HibernateManager.getActiveEmployees(SmartDB.getCurrentConservationArea(), s);
			Collections.sort(e, new Comparator<Employee>() {
				@Override
				public int compare(Employee arg0, Employee arg1) {
					return Collator.getInstance().compare(getLabel(arg0).toUpperCase(), getLabel(arg1).toUpperCase());
				}
			});
			for (Employee emp : e){
				items.add(new ListItem(emp.getUuid(), getLabel(emp)));
			}
			s.getTransaction().rollback();
			s.close();
		}catch (Exception ex){
			QueryPlugIn.displayLog(Messages.ObserverGroupByDropItem_ErrorLoadingEmployees, ex);
			s.close();
		}
		
		return items;
	}
	
	@Override
	public void dispose(){
		if (smallerFont != null){
			smallerFont.dispose();
		}
		super.dispose();
	}

	@Override
	public String getText() {
		StringBuilder sb = new StringBuilder();
		sb.append (Messages.ObserverGroupByDropItem_ObserverLabel + "\n");  //$NON-NLS-1$ 
		int cnt = 0;
		if (selectedItems != null){
			for (ListItem it : selectedItems){
				if (cnt >= 3){
					sb.append("..."); //$NON-NLS-1$
					break;
				}
				sb.append("   " + it.getName()); //$NON-NLS-1$
				sb.append("\n"); //$NON-NLS-1$
				cnt ++;
			}
		}
		return sb.toString();
	}

	@Override
	public String asQueryPart() {
		StringBuilder sql = new StringBuilder();
		sql.append(ObserverDropItem.KEY_PART); 
		sql.append(":"); //$NON-NLS-1$
		if (selectedItems != null && selectedItems.size() > 0){
			for (ListItem li : selectedItems){
				sql.append(UuidUtils.uuidToString(li.getUuid()) + ":"); //$NON-NLS-1$
			}
			sql.deleteCharAt(sql.length() - 1);
		}
		return sql.toString();
	}

	@Override
	public void initializeData(Object data) {
		if (data == null){
			this.selectedItems.clear();
		}else{
			ListItem[] d = (ListItem[])data;
			this.selectedItems = new ArrayList<ListItem>();
			for (int i = 0; i < d.length; i ++){
				selectedItems.add(d[i]);
			}
		}		
	}

	@Override
	protected void createComposite(Composite parent) {
		Composite comp = new Composite(parent, SWT.NONE);
		comp.setLayout(new GridLayout(2, false));
		
		lblText = new Label(comp, SWT.NONE);
		lblText.setText( formatStringForLabel(getText()));
		initDrag(lblText);
		
		final Link link = new Link(comp,  SWT.NONE);
		link.setForeground( parent.getShell().getDisplay().getSystemColor(SWT.COLOR_BLUE) );
		link.setText("<a>" + Messages.ObserverGroupByDropItem_FiltersLabel + "</a>"); //$NON-NLS-1$ //$NON-NLS-2$
		FontData fd = (link.getFont().getFontData()[0]);
		fd.setHeight(fd.getHeight() - 1);
		smallerFont = new Font(Display.getCurrent(), fd);
		link.setFont(smallerFont);
		link.setLayoutData(new GridData(SWT.BOTTOM, SWT.RIGHT, false, false));
		link.addSelectionListener(new SelectionAdapter() {
			
			@Override
			public void widgetSelected(SelectionEvent e) {
				GroupByFilterDialog dialog = new GroupByFilterDialog(Display.getDefault().getActiveShell());
				dialog.setGroupByItem(ObserverGroupByDropItem.this, selectedItems);
				int ret = dialog.open();
				if (ret == IDialogConstants.OK_ID){
					//save results here
					selectedItems.clear();					
					if (dialog.getSelectedItems() != null){
						for (int i= 0; i < dialog.getSelectedItems().length; i ++){
							selectedItems.add(dialog.getSelectedItems()[i]);
						}
					}
					updateLabel();
					ObserverGroupByDropItem.this.queryChanged();
					getTargetPanel().redraw();
				}
			}
			
		});
		
		toolTip = new ToolTip(parent.getShell(), SWT.BALLOON);
		toolTip.setText(Messages.ObserverGroupByDropItem_IncludedLabel);
		toolTip.setAutoHide(false);
		updateLabel();
		link.addMouseTrackListener(new MouseTrackAdapter() {			
			@Override
			public void mouseExit(MouseEvent e) {
				toolTip.setVisible(false);
			}
			
			@Override
			public void mouseEnter(MouseEvent e) {
				toolTip.setVisible(true);
			}
		});
	}
	
	private void updateToolTipMessage(){
		StringBuilder tipStr = new StringBuilder();
		if (selectedItems == null || selectedItems.size() == 0){
			tipStr.append(Messages.ObserverGroupByDropItem_AllLabel);
		}else{
			for (ListItem item: selectedItems){
				tipStr.append("'" + item.getName() + "'" + "\n"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			}
		}
		toolTip.setMessage(tipStr.toString());
	}


	/**
	 * Updates the text label
	 */
	private void updateLabel(){
		lblText.setText( formatStringForLabel(getText()));
		updateToolTipMessage();
	}
	
	protected String getLabel(Employee e){
		return SmartLabelProvider.getShortLabel(e);
	}
}
