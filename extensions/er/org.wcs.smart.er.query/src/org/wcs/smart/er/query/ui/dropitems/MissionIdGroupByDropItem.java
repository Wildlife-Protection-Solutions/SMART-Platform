package org.wcs.smart.er.query.ui.dropitems;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.ToolTip;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.er.model.Mission;
import org.wcs.smart.er.model.Survey;
import org.wcs.smart.er.model.SurveyDesign;
import org.wcs.smart.er.query.ERQueryPlugIn;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.query.ui.model.DropItem;
import org.wcs.smart.query.ui.model.IGroupByDropItem;
import org.wcs.smart.query.ui.model.ListItem;
import org.wcs.smart.query.ui.model.impl.GroupByFilterDialog;
import org.wcs.smart.util.SmartUtils;

public class MissionIdGroupByDropItem extends DropItem implements IGroupByDropItem, ISurveyDesignDropItem {

	private Font smallerFont;
	private ToolTip toolTip;
	private List<ListItem> filteredValues = new ArrayList<ListItem>();
	
	private SurveyDesign currentDesign;
	
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
			
		Session s = HibernateManager.openSession();
		s.beginTransaction();
		try{
			Criteria q = s.createCriteria(Mission.class)
					.createAlias("survey", "s")
					.createAlias("s.surveyDesign", "sd")
					.add(Restrictions.eq("sd.conservationArea", SmartDB.getCurrentConservationArea()));
			if (currentDesign != null){
				q.add(Restrictions.eq("s.surveyDesign", currentDesign));
			}
			q.addOrder(Order.desc("sd.startDate"));
			q.addOrder(Order.desc("startDate"));
			
			List<Mission> ss = q.list();
			for (Mission mission : ss){
				items.add(new ListItem(mission.getUuid(), mission.getId() + " [" + mission.getSurvey().getId() + " - " + mission.getSurvey().getSurveyDesign().getName() + "]"));
			}
			s.getTransaction().rollback();
			s.close();
		}catch (Exception ex){
			ERQueryPlugIn.displayLog("Error loading mission id items.", ex);
			s.close();
		}
		
		return items;
	}

	@Override
	public String getText() {
		return "Mission Id";
	}

	@Override
	public String asQueryPart() {
		StringBuilder sb = new StringBuilder();
		sb.append("s:mission:");
		if (filteredValues != null && filteredValues.size() > 0){
			for (ListItem id: filteredValues){
				sb.append(SmartUtils.encodeHex(id.getUuid()));
				sb.append(":");
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
		
		Label lbl = new Label(comp, SWT.NONE);
		lbl.setText( formatStringForLabel("Mission ID"));
		initDrag(lbl);
		
		final Link link = new Link(comp,  SWT.NONE);
		link.setForeground( parent.getShell().getDisplay().getSystemColor(SWT.COLOR_BLUE) );
		link.setText("<a>" + "Filters..." + "</a>");
		FontData fd = (link.getFont().getFontData()[0]);
		fd.setHeight(fd.getHeight() - 1);
		smallerFont = new Font(Display.getCurrent(), fd);
		link.setFont(smallerFont);
		link.setLayoutData(new GridData(SWT.BOTTOM, SWT.RIGHT, false, false));
		link.addSelectionListener(new SelectionAdapter() {
			
			@Override
			public void widgetSelected(SelectionEvent e) {
				GroupByFilterDialog dialog = new GroupByFilterDialog(Display.getDefault().getActiveShell());
				dialog.setGroupByItem(MissionIdGroupByDropItem.this, filteredValues);
				int ret = dialog.open();
				if (ret == IDialogConstants.OK_ID){
					//save results here
					filteredValues.clear();					
					if (dialog.getSelectedItems() != null){
						for (int i= 0; i < dialog.getSelectedItems().length; i ++){
							filteredValues.add(dialog.getSelectedItems()[i]);
						}
					}
					updateToolTipMessage();
					MissionIdGroupByDropItem.this.queryChanged();
				}
			}
			
		});
		
		toolTip = new ToolTip(parent.getShell(), SWT.BALLOON);
		toolTip.setText("Included:");
		toolTip.setAutoHide(false);
		updateToolTipMessage();
		link.addListener(SWT.MouseHover, new Listener(){
			@Override
			public void handleEvent(Event event) {
				toolTip.setVisible(true);
			}
		});
		
		link.addListener(SWT.MouseExit, new Listener(){
			@Override
			public void handleEvent(Event event) {
				toolTip.setVisible(false);
			}});
	}
	
	private void updateToolTipMessage(){
		StringBuilder tipStr = new StringBuilder();
		if (filteredValues == null){
			tipStr.append("All");
		}else{
			for (ListItem item: filteredValues){
				tipStr.append("'" + item.getName() + "'" + "\n"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			}
		}
		toolTip.setMessage(tipStr.toString());
	}

}
