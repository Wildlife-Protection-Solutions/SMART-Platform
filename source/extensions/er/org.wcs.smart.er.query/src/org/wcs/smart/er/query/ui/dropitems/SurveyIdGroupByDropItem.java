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
import org.wcs.smart.er.model.Survey;
import org.wcs.smart.er.model.SurveyDesign;
import org.wcs.smart.er.query.ERQueryPlugIn;
import org.wcs.smart.er.query.internal.Messages;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.query.ui.model.IGroupByDropItem;
import org.wcs.smart.query.ui.model.impl.GroupByFilterDialog;
import org.wcs.smart.ui.ca.datamodel.dropitem.DropItem;
import org.wcs.smart.ui.ca.datamodel.dropitem.ListItem;
import org.wcs.smart.util.UuidUtils;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

/**
 * Survey id group by drop item.
 * 
 * @author Emily
 *
 */
public class SurveyIdGroupByDropItem extends DropItem implements IGroupByDropItem, ISurveyDesignDropItem {

	private Font smallerFont;
	private ToolTip toolTip;
	private List<ListItem> filteredValues = new ArrayList<ListItem>();
	
	private SurveyDesign currentDesign;
	private Label lblText;
	
	@Override
	public void dispose(){
		smallerFont.dispose();
		super.dispose();
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
			
		try(Session s = HibernateManager.openSession()){
			s.beginTransaction();
			try{
				CriteriaBuilder cb = s.getCriteriaBuilder();
				CriteriaQuery<Survey> c = cb.createQuery(Survey.class);
				Root<Survey> from = c.from(Survey.class);
				c.select(from);

				Predicate[] filters = new Predicate[currentDesign == null ? 1 : 2];
				filters[0] = cb.equal(from.get("surveyDesign").get("conservationArea"), SmartDB.getCurrentConservationArea()); //$NON-NLS-1$ //$NON-NLS-2$
				if (currentDesign != null){
					filters[1] = cb.equal(from.get("surveyDesign"), currentDesign); //$NON-NLS-1$
				}
				c.where(cb.and(filters));
				c.orderBy(cb.asc(from.get("surveyDesign").get("keyId"))); //$NON-NLS-1$ //$NON-NLS-2$
				
				List<Survey> ss = s.createQuery(c).getResultList();
				for (Survey survey : ss){
					items.add(new ListItem(survey.getUuid(), survey.getId() + " [" + survey.getSurveyDesign().getName() + "]")); //$NON-NLS-1$ //$NON-NLS-2$
				}
				
				
			}catch (Exception ex){
				ERQueryPlugIn.displayLog(Messages.SurveyIdGroupByDropItem_LoadError, ex);
			}finally {
				s.getTransaction().rollback();
			}
		}
		
		return items;
	}

	@Override
	public String getText() {
		StringBuilder sb = new StringBuilder();
		sb.append (Messages.SurveyIdGroupByDropItem_Text + "\n");  //$NON-NLS-1$ 
		int cnt = 0;
		if (filteredValues != null){
			for (ListItem it : filteredValues){
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
		StringBuilder sb = new StringBuilder();
		sb.append("sgb:survey:id:"); //$NON-NLS-1$
		if (filteredValues != null && filteredValues.size() > 0){
			for (ListItem id: filteredValues){
				sb.append(UuidUtils.uuidToString(id.getUuid()));
				sb.append(":"); //$NON-NLS-1$
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
		lblText.setText( formatStringForLabel(Messages.SurveyIdGroupByDropItem_Label));
		initDrag(lblText);
		
		final Link link = new Link(comp,  SWT.NONE);
		link.setForeground( parent.getShell().getDisplay().getSystemColor(SWT.COLOR_BLUE) );
		link.setText("<a>" + Messages.SurveyIdGroupByDropItem_FiltersLabel + "</a>"); //$NON-NLS-1$ //$NON-NLS-2$
		FontData fd = (link.getFont().getFontData()[0]);
		fd.setHeight(fd.getHeight() - 1);
		smallerFont = new Font(Display.getCurrent(), fd);
		link.setFont(smallerFont);
		link.setLayoutData(new GridData(SWT.BOTTOM, SWT.RIGHT, false, false));
		link.addSelectionListener(new SelectionAdapter() {
			
			@Override
			public void widgetSelected(SelectionEvent e) {
				GroupByFilterDialog dialog = new GroupByFilterDialog(Display.getDefault().getActiveShell());
				dialog.setGroupByItem(SurveyIdGroupByDropItem.this, filteredValues);
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
					SurveyIdGroupByDropItem.this.queryChanged();
					getTargetPanel().redraw();
				}
			}
			
		});
		
		toolTip = new ToolTip(parent.getShell(), SWT.BALLOON);
		toolTip.setText(Messages.SurveyIdGroupByDropItem_IncludedLabel);
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
			tipStr.append(Messages.SurveyIdGroupByDropItem_AllLabel);
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
