package org.wcs.smart.event.test;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;
import org.wcs.smart.event.model.EAction;
import org.wcs.smart.event.model.EActionParameterValue;
import org.wcs.smart.event.ui.model.IActionParameterCollector;

public class NotifyUserActionTypeParameterCollector implements IActionParameterCollector{

	private Text txtString;
	private Text txtNumber;
	
	private List<Listener> listeners;
	
	public NotifyUserActionTypeParameterCollector() {
		listeners = new ArrayList<>();
	}
	
	@Override
	public Composite createComposite(Composite parent) {
		Composite cc = new Composite(parent, SWT.NONE);
		cc.setLayout(new GridLayout(2, false));
		
		Label l = new Label(cc, SWT.NONE);
		l.setText(NotifyUserActionType.STRING_PARAMETER.getName(Locale.getDefault()));
		
		txtString = new Text(cc, SWT.BORDER);
		txtString.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false ));
		((GridData)txtString.getLayoutData()).widthHint = 100;
		
		l = new Label(cc, SWT.NONE);
		l.setText(NotifyUserActionType.NUMBER_PARAMETER.getName(Locale.getDefault()));
		
		txtNumber = new Text(cc, SWT.BORDER);
		txtNumber.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false ));
		((GridData)txtNumber.getLayoutData()).widthHint = 100;
		
		txtString.addModifyListener(e->fireListeners());
		txtNumber.addModifyListener(e->fireListeners());
		
		return cc;
	}

	private void fireListeners() {
		for (Listener l : listeners) l.handleEvent(new Event());
	}
	@Override
	public void initParameters(EAction action) {
		for (EActionParameterValue v : action.getParameters()) {
			if (v.getId().getParameterKey().equals(NotifyUserActionType.NUMBER_PARAMETER.getKey())) {
				txtNumber.setText(v.getParameterValue());
			}else if (v.getId().getParameterKey().equals(NotifyUserActionType.STRING_PARAMETER.getKey())) {
				txtString.setText(v.getParameterValue());
			}
		}
	}

	@Override
	public void updateParameters(EAction action) {
		EActionParameterValue value = null;
		for (EActionParameterValue v : action.getParameters()) {
			if (v.getId().getParameterKey().equals(NotifyUserActionType.NUMBER_PARAMETER.getKey())) {
				value = v;
				break;
			}
		}
		if (txtNumber.getText().trim().isEmpty()) {
			if (value != null) {
				action.getParameters().remove(value);
			}
		}else {
			Double d = Double.valueOf(txtNumber.getText());
			
			if (value == null) {
				value = new EActionParameterValue();
				value.getId().setAction(action);
				value.getId().setParameterKey(NotifyUserActionType.NUMBER_PARAMETER.getKey());
				action.getParameters().add(value);
			}
			value.setParameterValue(String.valueOf(d));
		}
		
		
		value = null;
		for (EActionParameterValue v : action.getParameters()) {
			if (v.getId().getParameterKey().equals(NotifyUserActionType.STRING_PARAMETER.getKey())) {
				value = v;
				break;
			}
		}
		if (txtString.getText().isEmpty()) {
			if (value != null) {
				action.getParameters().remove(value);
			}
		}else {
			if (value == null) {
				value = new EActionParameterValue();
				value.getId().setAction(action);
				value.getId().setParameterKey(NotifyUserActionType.STRING_PARAMETER.getKey());
				action.getParameters().add(value);
			}
			String text = txtString.getText();
			value.setParameterValue(text);
		}
	}

	@Override
	public String validate() {
		try {
			if (!txtNumber.getText().trim().isEmpty()) {
				Double d = Double.parseDouble(txtNumber.getText().trim());
			}
		}catch (Exception ex) {
			return "The number value parameter value must be a valid number";
		}
		return null;
	}

	@Override
	public void addModifyListener(Listener listener) {
		listeners.add(listener);
	}

}
