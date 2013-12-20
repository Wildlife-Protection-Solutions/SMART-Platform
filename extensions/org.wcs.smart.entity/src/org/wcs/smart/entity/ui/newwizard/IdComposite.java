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
package org.wcs.smart.entity.ui.newwizard;

import java.text.MessageFormat;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.hibernate.Session;
import org.wcs.smart.entity.internal.Messages;
import org.wcs.smart.entity.model.Entity;
import org.wcs.smart.entity.model.EntityType;
import org.wcs.smart.util.SmartUtils;
import org.wcs.smart.util.SmartUtils.RegExLevel;
/**
 * Composite for modifying the entity type id property.
 *  
 * @author Emily
 *
 */
public class IdComposite extends AbstractEntityComposite{

	private Text txtId;
	
	@Override
	public String getName() {
		return Messages.IdComposite_TypeIdName;
	}

	@Override
	public String getDescription() {
		return Messages.IdComposite_TypeIdDescription;
	}

	@Override
	public String validate() {
		if (txtId.getText().length() == 0){
			return Messages.IdComposite_IdRequired;
		}
		if (!SmartUtils.isSimpleString(txtId.getText().trim(), RegExLevel.ALLOWED_CHARS_SIMPLE_REGEX, Entity.KEY_MAX_LENGTH, 1)){
			return MessageFormat.format(Messages.IdComposite_InvalidId,
					new Object[]{1, Entity.KEY_MAX_LENGTH, RegExLevel.ALLOWED_CHARS_SIMPLE_REGEX.textDesc});
		}
		return null;
	}

	@Override
	public Composite createComposite(Composite parent) {
		Composite main = new Composite(parent, SWT.NONE);
		main.setLayout(new GridLayout(3, false));
		main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		((GridLayout)main.getLayout()).marginWidth = 0;
		//ID
		Label l = new Label(main, SWT.NONE);
		l.setText(Messages.IdComposite_IdLabel);
		l.setToolTipText(Messages.IdComposite_IdTooltip);
		
		txtId = new Text(main, SWT.BORDER);
		txtId.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				fireChange(new Event());
			}
		});
		txtId.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		
		return main;
	}
	
	@Override
	public void updateEntityType(EntityType entityType) {
		entityType.setId(txtId.getText().trim());
	}

	@Override
	public void initFields(EntityType entityType, Session session) {
		if (entityType.getId() != null){
			txtId.setText(entityType.getId());
		}
	}

}