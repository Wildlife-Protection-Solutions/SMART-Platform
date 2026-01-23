/*
 * Copyright (C) 2026 Wildlife Conservation Society
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
package org.wcs.smart.er.ui;

import java.text.MessageFormat;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.nebula.jface.tablecomboviewer.TableComboViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.ca.IconManager;
import org.wcs.smart.ca.datamodel.Attribute.AttributeType;
import org.wcs.smart.common.control.SmartUiUtils;
import org.wcs.smart.er.internal.Messages;
import org.wcs.smart.er.internal.MissionMergeMetadata;
import org.wcs.smart.er.model.Mission;
import org.wcs.smart.er.model.MissionAttribute;
import org.wcs.smart.er.model.MissionAttributeListItem;
import org.wcs.smart.er.model.MissionPropertyValue;
import org.wcs.smart.er.model.Survey;
import org.wcs.smart.ui.NamedIconItemLabelProvider;
import org.wcs.smart.ui.SmartLabelProvider;
import org.wcs.smart.ui.SmartStyledTitleDialog;
import org.wcs.smart.util.SmartUtils;
import org.wcs.smart.util.SmartUtils.RegExLevel;

/**
 * Dialog for gathering metadata for merged missions
 */
public class MissionMergeDialog extends SmartStyledTitleDialog {
	
	private ComboViewer cmbId;
	private ComboViewer cmbSurvey;
	
	private ComboViewer cmbLeader;
	private Map<MissionAttribute, Object> attributeCtrls;
	private MissionMergeMetadata metadata;
	
	public MissionMergeDialog(Shell parent, MissionMergeMetadata metadata) {
		super(parent);
		this.metadata = metadata;
	}
	
	
	@Override
	public Control createDialogArea(Composite parent) {
		parent = (Composite) super.createDialogArea(parent);
		parent = new Composite(parent, SWT.NONE);
		parent.setLayout(new GridLayout());
		parent.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		Composite cwarn = new Composite(parent, SWT.NONE);
		cwarn.setLayout(new GridLayout(2, false));
		cwarn.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		Label lblwarn = new Label(cwarn, SWT.NONE);
		lblwarn.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.WARN_ICON));
		
		lblwarn = new Label(cwarn, SWT.WRAP);
		lblwarn.setText(Messages.MissionMergeDialog_MergeMessage);
		lblwarn.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		SmartUiUtils.createHeaderLabel(parent, Messages.MissionMergeDialog_MergeMetadata);
		
		Composite cmetadata = new Composite(parent, SWT.NONE);
		cmetadata.setLayout(new GridLayout(2, false));
		cmetadata.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		Label l = new Label(cmetadata, SWT.NONE);
		l.setText(Messages.SurveyIdPage_IdLabel);

		cmbSurvey = new ComboViewer(cmetadata, SWT.DROP_DOWN | SWT.READ_ONLY);
		cmbSurvey.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		cmbSurvey.setContentProvider(ArrayContentProvider.getInstance());
		
		cmbSurvey.setInput(metadata.getSurveyOps());
		cmbSurvey.setLabelProvider(new LabelProvider() {
			@Override
			public String getText(Object element) {
				return ((Survey)element).getId();
			}
		});
		cmbSurvey.setSelection(new StructuredSelection(metadata.getSurveyOps().getFirst()));
		cmbSurvey.addSelectionChangedListener(e->validate());
		
		l = new Label(cmetadata, SWT.NONE);
		l.setText(Messages.ErLabelProvider_MissionMetadataId);

		cmbId = new ComboViewer(cmetadata, SWT.DROP_DOWN);
		cmbId.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		cmbId.setContentProvider(ArrayContentProvider.getInstance());
		cmbId.setInput(metadata.getMissionIdOps());
		cmbId.setLabelProvider(new LabelProvider() {
			@Override
			public String getText(Object element) {
				return (String)element;
			}
		});
		cmbId.setSelection(new StructuredSelection(metadata.getMissionIdOps().getFirst()));
		cmbId.getControl().addListener(SWT.FocusOut, e->validate());
		
		
		l = new Label(cmetadata, SWT.NONE);
		l.setText(Messages.DatesComponent_StartDateLabel);
		
		l = new Label(cmetadata, SWT.NONE);
		l.setText(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).format(metadata.getStartDate()));

		l = new Label(cmetadata, SWT.NONE);
		l.setText(Messages.DatesComponent_EndDateLabel);
		
		l = new Label(cmetadata, SWT.NONE);
		l.setText(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).format(metadata.getEndDate()));
		
		l = new Label(cmetadata, SWT.NONE);
		l.setText(Messages.ErLabelProvider_MissionMetadataLeader);

		cmbLeader = new ComboViewer(cmetadata, SWT.DROP_DOWN | SWT.READ_ONLY);
		cmbLeader.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		cmbLeader.setContentProvider(ArrayContentProvider.getInstance());
		cmbLeader.setInput(metadata.getEmployeeOps());
		cmbLeader.setLabelProvider(new LabelProvider() {
			@Override
			public String getText(Object element) {
				return SmartLabelProvider.getFullLabel((Employee)element);
			}
		});
		cmbLeader.setSelection(new StructuredSelection(metadata.getLeader()));
		cmbLeader.addSelectionChangedListener(e->validate());
		
		attributeCtrls = new HashMap<>();
		Map<MissionAttribute, List<MissionPropertyValue>> aops = metadata.getAttributesOps();
		for (MissionAttribute attribute : aops.keySet()) {
			l = new Label(cmetadata, SWT.NONE);
			l.setText(attribute.getName());
			
			if (attribute.getType() == AttributeType.TEXT){
				ComboViewer cmb = new ComboViewer(cmetadata, SWT.DROP_DOWN);
				cmb.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
				
				//cmb.getControl().setTextLimit(Attribute.STRING_ATTRIBUTE_MAX_LENGTH);
				cmb.setContentProvider(ArrayContentProvider.getInstance());
				cmb.setLabelProvider(new LabelProvider() {
					@Override
					public String getText(Object element) {
						return (String)element;
					}
				});
				List<String> ops = new ArrayList<>();
				aops.get(attribute).forEach(mv->ops.add(mv.getStringValue()));
				cmb.setInput(ops);
				if (!ops.isEmpty()) cmb.setSelection(new StructuredSelection(ops.get(0)));
				attributeCtrls.put(attribute, cmb);
				
				cmb.getControl().addListener(SWT.FocusOut, e->validate());
		} else if(attribute.getType() == AttributeType.NUMERIC){

				ComboViewer cmb = new ComboViewer(cmetadata, SWT.DROP_DOWN);
				cmb.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
				
				//cmb.getControl().setTextLimit(Attribute.STRING_ATTRIBUTE_MAX_LENGTH);
				cmb.setContentProvider(ArrayContentProvider.getInstance());
				cmb.setLabelProvider(new LabelProvider() {
					@Override
					public String getText(Object element) {
						return (String)element;
					}
				});
				List<String> ops = new ArrayList<>();
				aops.get(attribute).forEach(mv->ops.add(mv.getNumberValue().toString()));
				cmb.setInput(ops);
				if (!ops.isEmpty()) cmb.setSelection(new StructuredSelection(ops.get(0)));
				attributeCtrls.put(attribute, cmb);
				
				cmb.getControl().addListener(SWT.FocusOut, e->validate());
			} else if(attribute.getType() == AttributeType.LIST){
				TableComboViewer cmbViewer = new TableComboViewer(cmetadata, SWT.DROP_DOWN | SWT.READ_ONLY | SWT.BORDER);
				cmbViewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
				cmbViewer.setContentProvider(ArrayContentProvider.getInstance());
				cmbViewer.setLabelProvider(new NamedIconItemLabelProvider(IconManager.Size.ICON));

				List<Object> items = new ArrayList<Object>();
				items.add(""); //$NON-NLS-1$
				items.addAll(attribute.getAttributeList());
				cmbViewer.setInput(items);
				
				if (!aops.get(attribute).isEmpty()) cmbViewer.setSelection(new StructuredSelection(aops.get(attribute).getFirst().getAttributeListItem()));
				attributeCtrls.put(attribute, cmbViewer);
				cmbViewer.addSelectionChangedListener(e->validate());
			}
		}
		
		setMessage(Messages.MissionMergeDialog_DialogMessage);
		setTitle(Messages.MissionMergeDialog_DialogTitle);
		getShell().setText(Messages.MissionMergeDialog_DialogTitle);
		return parent;
	}
	
	private void validate() {
		boolean isvalid = this.isValidAndSet();
		
		getButton(IDialogConstants.OK_ID).setEnabled(isvalid);
	}
	
	private boolean isValidAndSet() {
		
		Survey survey = (Survey) cmbSurvey.getStructuredSelection().getFirstElement();
		if (survey == null) {
			setErrorMessage(Messages.MissionMergeDialog_SurveyRequired);
			return false;
		}
		
		String id = cmbId.getCombo().getText().trim();
		if (!SmartUtils.isSimpleString(id, RegExLevel.ALLOWED_CHARS_COMPLEX_REGEX, Mission.MAX_LENGTH_ID)){			
			setErrorMessage(MessageFormat.format(Messages.IdComposite_IdError, new Object[]{Mission.MAX_LENGTH_ID, RegExLevel.ALLOWED_CHARS_COMPLEX_REGEX.textDesc}));
			return false;
		}
		
		Employee leader = (Employee) cmbLeader.getStructuredSelection().getFirstElement();
		if (leader == null) {
			setErrorMessage(Messages.MissionMergeDialog_LeaderRequired);
			return false;
		}
		
		List<MissionPropertyValue> values = new ArrayList<>();
		for (MissionAttribute ma : attributeCtrls.keySet()) {
			if (ma.getType() == AttributeType.TEXT) {
				String txt = ((ComboViewer)attributeCtrls.get(ma)).getCombo().getText();
				if (txt.isBlank()) continue;
				MissionPropertyValue value = new MissionPropertyValue();
				value.setMissionAttribute(ma);
				value.setStringValue(txt);
				values.add(value);
			}else if (ma.getType() == AttributeType.NUMERIC) {
				String txt = ((ComboViewer)attributeCtrls.get(ma)).getCombo().getText();
				if (txt.isBlank()) continue;
				try {
					Double d = Double.parseDouble(txt);
					MissionPropertyValue value = new MissionPropertyValue();
					value.setMissionAttribute(ma);
					value.setNumberValue(d);
					values.add(value);
				}catch (Exception ex) {
					setErrorMessage(MessageFormat.format(Messages.MissionMergeDialog_InvalidNumberValue, ma.getName()));
					return false;

				}
			}else if (ma.getType() == AttributeType.LIST) {
				Object first = ((TableComboViewer)attributeCtrls.get(ma)).getStructuredSelection().getFirstElement();
				if (first == null) continue;
				if (first instanceof MissionAttributeListItem item) {
					MissionPropertyValue value = new MissionPropertyValue();
					value.setMissionAttribute(ma);
					value.setAttributeListItem(item);
					values.add(value);
				}
			}
			
		}
		
		metadata.setSelectedValues(survey, id, leader, values);
		setErrorMessage(null);
		return true;
	}
	
	@Override
	protected void okPressed() {
		if (!this.isValidAndSet()) return;
		super.okPressed();		
	}

}
