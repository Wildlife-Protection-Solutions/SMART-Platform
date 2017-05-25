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
package org.wcs.smart.patrol.query.ui.definition;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Listener;
import org.wcs.smart.patrol.query.internal.Messages;
import org.wcs.smart.patrol.query.model.PatrolGridQueryDefinition;
import org.wcs.smart.patrol.query.model.PatrolGriddedQuery;
import org.wcs.smart.query.common.model.GriddedQuery;
import org.wcs.smart.query.model.QueryProxy;
import org.wcs.smart.query.model.filter.IFilter.FilterType;
import org.wcs.smart.query.ui.definition.BasicFilterDefintionPanel;

/**
 * Filter panel for no data query filter
 * @author Emily
 *
 */
public class PatrolGridNoDataFilterPanel extends BasicFilterDefintionPanel {

	public static final String ID = "org.wcs.smart.patrol.query.PatrolGridNoDataFilterPanel"; //$NON-NLS-1$
	
	private Composite filterPanel;
	private Button btnOpDefault;
	private Button btnOpNone;
	private Button btnOpCustom;
	
	public PatrolGridNoDataFilterPanel() {
		super();
	}

	@Override
	public String getId() {
		return ID;
	}

	@Override
	public String getGuiName() {
		return Messages.PatrolGridNoDataFilterPanel_TabName;
	}


	@Override
	public String validate() {
		if (!btnOpCustom.getSelection() && !btnOpDefault.getSelection() && !btnOpNone.getSelection()){
			return Messages.PatrolGridNoDataFilterPanel_OptionRequired;
		}
		return super.validate();
	}

	@Override
	public Composite createComposite(Composite parent) {
		Composite core = new Composite(parent, SWT.NONE);
		core.setLayout(new GridLayout());
		
		Composite opComp = new Composite(core, SWT.NONE);
		opComp.setLayout(new GridLayout(1, false));
		opComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		((GridLayout)opComp.getLayout()).marginWidth = 0;
		((GridLayout)opComp.getLayout()).marginHeight = 0;
		
		btnOpDefault = new Button(opComp, SWT.RADIO);
		btnOpDefault.setText(Messages.PatrolGridNoDataFilterPanel_DefaultOp);
		btnOpDefault.setToolTipText(Messages.PatrolGridNoDataFilterPanel_DefaultTooltip);
		btnOpDefault.setSelection(true);
		
		btnOpNone = new Button(opComp, SWT.RADIO);
		btnOpNone.setText(Messages.PatrolGridNoDataFilterPanel_NoneOp);
		btnOpNone.setToolTipText(Messages.PatrolGridNoDataFilterPanel_NoneTooltip);
		
		btnOpCustom = new Button(opComp, SWT.RADIO);
		btnOpCustom.setText(Messages.PatrolGridNoDataFilterPanel_CustomOp);
		btnOpCustom.setToolTipText(Messages.PatrolGridNoDataFilterPanel_CustomTooltip);

		Listener l = (e) -> {
			updateEnabled();
			fireQueryChangedListeners();
		};
		
		btnOpDefault.addListener(SWT.Selection, l);
		btnOpNone.addListener(SWT.Selection, l);
		btnOpCustom.addListener(SWT.Selection, l);
		
		filterPanel = new Composite(core, SWT.BORDER);
		filterPanel.setLayout(new GridLayout());
		((GridLayout)filterPanel.getLayout()).marginWidth = 0;
		((GridLayout)filterPanel.getLayout()).marginHeight = 0;
		filterPanel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		super.createComposite(filterPanel);
		
		return core;
	}

	@Override
	public Composite getDropTargetComposite() {
		return super.getDropTargetComposite();
	}

	@Override
	public void dispose() {
		super.dispose();
	}

	@Override
	public String getQueryPart() {
		if (btnOpDefault.getSelection()) return PatrolGridQueryDefinition.ZeroFilterOption.DEFAULT.getKey() + "|"; //$NON-NLS-1$
		if (btnOpNone.getSelection()) return PatrolGridQueryDefinition.ZeroFilterOption.NONE.getKey() + "|"; //$NON-NLS-1$
		if (btnOpCustom.getSelection()) return PatrolGridQueryDefinition.ZeroFilterOption.CUSTOM.getKey() + "|" + super.getQueryPart(); //$NON-NLS-1$
		return null;
	}

	@Override
	public void saveItems(QueryProxy q) {
		super.saveItems(q);
	}

	@Override
	public void initItems(QueryProxy q) throws Exception {
		
		super.initItems(q);
		if (q.getQuery() instanceof GriddedQuery){
			if ( ((GriddedQuery)q.getQuery()).getQueryDefinition() != null){

				PatrolGridQueryDefinition def = (PatrolGridQueryDefinition) ((PatrolGriddedQuery)q.getQuery()).getQueryDefinition();
				
				PatrolGridQueryDefinition.ZeroFilterOption zop = def.getZeroDataFilterOption();
				btnOpCustom.setSelection(false);
				btnOpDefault.setSelection(false);
				btnOpNone.setSelection(false);
				if (zop != null){
					switch(zop){
					case CUSTOM:
						btnOpCustom.setSelection(true);
						break;
					case DEFAULT:
						btnOpDefault.setSelection(true);
						break;
					case NONE:
						btnOpNone.setSelection(true);
						break;
					default:
						break;
					}
				}else{
					btnOpDefault.setSelection(true);
				}
				if (def.getZeroDataFilter() != null){
					setFilterType( def.getZeroDataFilter().getFilterType() );
				}else{
					setFilterType(FilterType.WAYPOINT);
				}
				updateEnabled();
			}
		}
	}

	private void updateEnabled(){
		filterPanel.setEnabled(btnOpCustom.getSelection());
		
		boolean state = btnOpCustom.getSelection();
		List<Control> toProcess = new ArrayList<Control>();
		toProcess.addAll(Arrays.asList(filterPanel.getChildren()));
		while(toProcess.size() > 0){
			Control c = toProcess.remove(0);
			if (c instanceof Composite){
				toProcess.addAll( Arrays.asList(((Composite)c).getChildren()));
			}
			c.setEnabled(state);
		}
	}
}
