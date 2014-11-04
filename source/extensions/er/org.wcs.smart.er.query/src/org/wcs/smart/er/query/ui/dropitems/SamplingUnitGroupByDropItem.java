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
package org.wcs.smart.er.query.ui.dropitems;

import java.util.ArrayList;
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
import org.wcs.smart.er.hibernate.SurveyHibernateManager;
import org.wcs.smart.er.model.SamplingUnit;
import org.wcs.smart.er.model.SurveyDesign;
import org.wcs.smart.er.query.ERQueryPlugIn;
import org.wcs.smart.er.query.filter.SamplingUnitFilter;
import org.wcs.smart.er.query.internal.Messages;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.query.ui.model.DropItem;
import org.wcs.smart.query.ui.model.IGroupByDropItem;
import org.wcs.smart.query.ui.model.ListItem;
import org.wcs.smart.query.ui.model.impl.GroupByFilterDialog;
import org.wcs.smart.util.SmartUtils;

/**
 * Sampling unit group by drop item.
 * 
 * @author Emily
 *
 */
public class SamplingUnitGroupByDropItem extends DropItem 
	implements IGroupByDropItem, ISurveyDesignDropItem {

	private Font smallerFont;
	private ToolTip toolTip;
	private Label lblText;
	private List<ListItem> filteredValues = new ArrayList<ListItem>();
	
	private SurveyDesign currentDesign;
	
	@Override
	public void dispose(){
		super.dispose();
		if (smallerFont != null){
			smallerFont.dispose();
			smallerFont = null;
		}
	}
	
	@Override
	public void setSurveyDesign(SurveyDesign design) {
		this.currentDesign = design;
	}

	/**
	 * Opens and closes a hibernate session so this should
	 * be executed in a separate thread.
	 * 
	 * @see org.wcs.smart.query.ui.formulaDnd.IGroupByDropItem#getListItem()
	 */
	@Override
	public List<ListItem> getListItem() {
		List<ListItem> items = new ArrayList<ListItem>();
			
		Session s = HibernateManager.openSession();
		s.beginTransaction();
		try{
			List<SamplingUnit> objects = SurveyHibernateManager.getInstance().getSamplingUnits(currentDesign, s, null);
			for (SamplingUnit su : objects){
				items.add(new ListItem(su.getUuid(), su.getId(), null));
			}
			items.add(new ListItem(null, SamplingUnitFilter.NONE.getId(), SamplingUnitFilter.NONE_KEY));
			
			s.getTransaction().rollback();
			s.close();
		}catch (Exception ex){
			ERQueryPlugIn.displayLog(Messages.SamplingUnitGroupByDropItem_LoadingError, ex);
			s.close();
		}
		
		return items;
	}

	@Override
	public String getText() {
		StringBuilder sb = new StringBuilder();
		sb.append (Messages.SamplingUnitGroupByDropItem_Text + "\n");  //$NON-NLS-1$ 
		int cnt = 0;
		for (ListItem it : filteredValues){
			if (cnt >= 3){
				sb.append("..."); //$NON-NLS-1$
				break;
			}
			sb.append("   " + it.getName()); //$NON-NLS-1$
			sb.append("\n"); //$NON-NLS-1$
			cnt ++;
		}
		return sb.toString();
	}
	
	@Override
	public String asQueryPart() {
		StringBuilder sb = new StringBuilder();
		sb.append("sgb:samplingunit:"); //$NON-NLS-1$
		if (filteredValues != null && filteredValues.size() > 0){
			for (ListItem id: filteredValues){
				if (id.getUuid() == null){
					sb.append(SamplingUnitFilter.NONE_KEY);
					sb.append(":"); //$NON-NLS-1$
				}else{
					sb.append(SmartUtils.encodeHex(id.getUuid()));
					sb.append(":"); //$NON-NLS-1$
				}
			}
			sb.deleteCharAt(sb.length() - 1);
		}
		return sb.toString();
	}

	/**
	 * @param data - must be a (ListItem[]) that represents
	 * the selected group by options or null if all selected
	 * @see org.wcs.smart.query.ui.formulaDnd.DropItem#initializeData(java.lang.Object)
	 */
	@Override
	public void initializeData(Object data) {
		if (data == null){
			this.filteredValues.clear();
		}else{
			ListItem[] d = (ListItem[])data;
			this.filteredValues = new ArrayList<ListItem>();
			for (int i = 0; i < d.length; i ++){
				filteredValues.add(d[i]);
			}
		}		
	}

	@Override
	protected void createComposite(Composite parent) {
		Composite comp = new Composite(parent, SWT.NONE);
		comp.setLayout(new GridLayout(2, false));
		
		lblText = new Label(comp, SWT.NONE);
		lblText.setText( formatStringForLabel(Messages.SamplingUnitGroupByDropItem_Label));
		initDrag(lblText);
		
		final Link link = new Link(comp,  SWT.NONE);
		link.setForeground( parent.getShell().getDisplay().getSystemColor(SWT.COLOR_BLUE) );
		link.setText("<a>" + Messages.SamplingUnitGroupByDropItem_FiltersLabel + "</a>"); //$NON-NLS-1$ //$NON-NLS-2$
		FontData fd = (link.getFont().getFontData()[0]);
		fd.setHeight(fd.getHeight() - 1);
		smallerFont = new Font(Display.getCurrent(), fd);
		link.setFont(smallerFont);
		link.setLayoutData(new GridData(SWT.BOTTOM, SWT.RIGHT, false, false));
		link.addSelectionListener(new SelectionAdapter() {
			
			@Override
			public void widgetSelected(SelectionEvent e) {
				GroupByFilterDialog dialog = new GroupByFilterDialog(Display.getDefault().getActiveShell());
				dialog.setGroupByItem(SamplingUnitGroupByDropItem.this, filteredValues);
				int ret = dialog.open();
				if (ret == IDialogConstants.OK_ID){
					//save results here
					filteredValues.clear();					
					if (dialog.getSelectedItems() != null){
						for (int i= 0; i < dialog.getSelectedItems().length; i ++){
							filteredValues.add(dialog.getSelectedItems()[i]);
						}
					}
					updateLabel();
					queryChanged();
					getTargetPanel().redraw();
				}
			}
			
		});
		
		toolTip = new ToolTip(parent.getShell(), SWT.BALLOON);
		toolTip.setText(Messages.SamplingUnitGroupByDropItem_IncludedLabel);
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
		if (filteredValues == null || filteredValues.size() == 0){
			tipStr.append(Messages.SamplingUnitGroupByDropItem_AllLabel);
		}else{
			for (ListItem item: filteredValues){
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

}
