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

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
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
import org.eclipse.swt.widgets.Label;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.er.model.Mission;
import org.wcs.smart.er.model.Survey;
import org.wcs.smart.er.model.SurveyDesign;
import org.wcs.smart.er.query.filter.MissionFilter;
import org.wcs.smart.er.query.filter.SurveyFilter;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.query.model.filter.Operator;
import org.wcs.smart.query.ui.model.DropItem;
import org.wcs.smart.query.ui.model.IFilterDropItem;

/**
 * Patrol id drop item. This consists of a list of 
 * all the patrol ids along that can also be typed into.
 *  
 * @author Emily
 * @since 1.0.0
 */
public class MissionIdDropItem  extends DropItem implements IFilterDropItem, ISurveyDesignDropItem{

	
	private Label lblAttribute;
	private Combo value;
	private Combo operators;
	
	private Font smallerFont;
	private Font smallerFont2;
	
	private String currentValue = null;
	private String currentOp = null;
	
	private SurveyDesign design = null;
	
	/*
	 * job to load all patrol ids
	 */
	private Job loadIdsJob = new Job("Load Mission Ids"){
		@Override
		protected IStatus run(IProgressMonitor monitor) {
			if (value.isDisposed() || design == null){
				return Status.OK_STATUS;
			}
			
			final List<String> data = new ArrayList<String>();
			Session s = HibernateManager.openSession();
			try{
				List<Mission> ss = s.createCriteria(Mission.class)
						.createAlias("survey", "s")
						.add(Restrictions.eq("s.surveyDesign", design)).list();
				for (Mission sy : ss){
					data.add(sy.getId());
				}
			}finally{
				s.close();
			}
			Display.getDefault().asyncExec(new Runnable(){
				@Override
				public void run() {
					if (value.isDisposed()){
						return ;
					}
					value.removeAll();
					for (String id : data){
						value.add(id);
					}		
				}});
			return Status.OK_STATUS;
		}};
		
	/**
	 * Creates a new survey id drop item
	 */
	public MissionIdDropItem() {
		
	}

	/**
	 * @see org.wcs.smart.query.ui.formulaDnd.DropItem#getText()
	 */
	@Override
	public String getText() {
		return MissionFilter.ID_QUERY_KEY + " " + Operator.STRING_OPS[operators.getSelectionIndex()].getGuiValue() + " " ;//+ value.getText() ; //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * @see org.wcs.smart.query.ui.formulaDnd.DropItem#asQueryPart()
	 */
	@Override
	public String asQueryPart() {
		return MissionFilter.ID_QUERY_KEY + " " +  Operator.STRING_OPS[operators.getSelectionIndex()].asSmartValue() + " \"" + value.getText() + "\""; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

	/**
	 * @see org.eclipse.swt.widgets.Widget#dispose()
	 */
	@Override
	public void dispose(){
		super.dispose();
		if (smallerFont != null){
			smallerFont.dispose();
		}
		if (smallerFont2 != null){
			smallerFont2.dispose();
		}
		this.design = null;
	}

	/**
	 * @see org.wcs.smart.query.ui.formulaDnd.DropItem#createComposite(org.eclipse.swt.widgets.Composite)
	 */
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
		
		FontData fd = (operators.getFont().getFontData()[0]);
		fd.setHeight(fd.getHeight() - 1);
		smallerFont = new Font(Display.getCurrent(), fd);
		operators.setFont(smallerFont);
		operators.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				if (currentOp != null && currentOp.equals(operators.getText())){
					//do nothing as has not changed
				}else{
					queryChanged();
					currentOp = operators.getText();
				}
			}
		});
		
		value = new Combo(main, SWT.BORDER | SWT.DROP_DOWN);
		value.addModifyListener(new ModifyListener() {			
			@Override
			public void modifyText(ModifyEvent e) {
				if (currentValue != null && currentValue.equals(value.getText())){
					//ignore; not changed
				}else{
					queryChanged();
					value.setToolTipText(value.getText());
					currentValue = value.getText();
				}
			}
		});
		
		fd = (value.getFont().getFontData()[0]);
		fd.setHeight(fd.getHeight() - 1);
		smallerFont2 = new Font(Display.getCurrent(), fd);
		value.setFont(smallerFont2);
		
		GridData gd = new GridData();
		gd.minimumWidth = 50;
		gd.widthHint = 100;
		value.setLayoutData(gd);
		
		initDrag(main);
		initDrag(lblAttribute);
		
		
		lblAttribute.setText(formatStringForLabel("Mission ID"));
		
		int index = 0;
		for (int i = 0; i < Operator.STRING_OPS.length; i ++){
			operators.add(Operator.STRING_OPS[i].getGuiValue());
			if (currentOp != null && Operator.STRING_OPS[i].getGuiValue().equals(currentOp)){
				index =i;
			}
		}
		operators.select(index);
		if (currentValue != null){
			value.setText(currentValue);
		}
		
	}

	/**
	 * @param data array containing the following values {currentOperator, currentValue}
	 */
	@Override
	public void initializeData(Object data) {
		this.currentOp = ((String[])data)[0];
		this.currentValue = ((String[])data)[1];
		
	}

	@Override
	public void setSurveyDesign(SurveyDesign design) {
		this.design = design;
		loadIdsJob.schedule();
	}
	
}
