/*
 * Copyright (C) 2019 Wildlife Conservation Society
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
package org.wcs.smart.cybertracker.navigation.ui;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.e4.ui.css.swt.dom.WidgetElement;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.wcs.smart.cybertracker.CyberTrackerPlugIn;
import org.wcs.smart.cybertracker.internal.Messages;
import org.wcs.smart.cybertracker.navigation.INavigationExportAction;
import org.wcs.smart.ui.SmartStyledTitleDialog;
import org.wcs.smart.ui.properties.DialogConstants;

/**
 * Dialog to display navigation export options to user
 * 
 * @author Emily
 * @since 7.0.0
 *
 */
public class CtNavigationExportDialog extends SmartStyledTitleDialog {

	private static final String KEY_SEP = "|"; //$NON-NLS-1$
	private static final String ACTIONS_PREF_KEY = "org.wcs.smart.cybertracker.ctpackage.ui.CtNavigationExportDialog.action"; //$NON-NLS-1$
	
	private List<Button> actionButtons;
	private List<INavigationExportAction> allActions;
	private List<INavigationExportAction> selectedActions;
	
	public CtNavigationExportDialog(Shell parent, List<INavigationExportAction> actions) {
		super(parent);
		this.allActions = actions;
	}

	public List<INavigationExportAction> getSelectedActions() { return this.selectedActions;}
	
	
	@Override
	public void okPressed() {
		selectedActions = new ArrayList<>();
	
		for (Button b : actionButtons) {
			INavigationExportAction action = (INavigationExportAction)b.getData();
			if (b.getSelection()) {
				selectedActions.add(action);
				InstanceScope.INSTANCE.getNode(CyberTrackerPlugIn.PLUGIN_ID).putBoolean(ACTIONS_PREF_KEY + KEY_SEP + action.getClass().getCanonicalName(), true);
			}else {
				InstanceScope.INSTANCE.getNode(CyberTrackerPlugIn.PLUGIN_ID).putBoolean(ACTIONS_PREF_KEY + KEY_SEP + action.getClass().getCanonicalName(), false); 
			}
		}
		super.okPressed();
	}
	
	@Override
	public void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.OK_ID, DialogConstants.EXPORT_BUTTON_TEXT, true);
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
	}
	
	@Override
	public Composite createDialogArea(Composite parent) {
		parent = (Composite) super.createDialogArea(parent);
		parent.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_WHITE));
		
		Composite main = new Composite(parent, SWT.NONE);
		main.setLayout(new GridLayout());
		main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		main.setBackground(main.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));

		actionButtons = new ArrayList<>();
		
		Color selectionColor = new Color(main.getDisplay(), 226, 241, 255);
		main.addListener(SWT.Dispose, e-> selectionColor.dispose());

		Composite inner = new Composite(main, SWT.NONE);
		inner.setLayout(new GridLayout());
		inner.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		((GridLayout)inner.getLayout()).marginHeight = 0;
		
		for (INavigationExportAction a : allActions) {
			Composite small = new Composite(inner, SWT.NONE);
			small.setLayout(new GridLayout(3, false));
			((GridLayout)small.getLayout()).marginWidth = 0;
			((GridLayout)small.getLayout()).marginHeight = 0;
			small.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			
			Button btnAction = new Button(small, SWT.CHECK);
			btnAction.setData(a);
			btnAction.setBackground(main.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
			
			MouseListener click = new MouseAdapter() {
				@Override
				public void mouseUp(MouseEvent e) {
					btnAction.setSelection(!btnAction.getSelection());
					updateBackground(small, btnAction, selectionColor);
				}
			};
			
			Label l = new Label(small, SWT.NONE);
			l.setImage(a.getIcon());
			l.setBackground(main.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
			l.addMouseListener(click);
			
			l = new Label(small, SWT.NONE);
			l.setText(a.getName());
			l.setBackground(main.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
			l.addMouseListener(click);
			
			small.addMouseListener(click);
			
			actionButtons.add(btnAction);

			btnAction.addListener(SWT.Selection, e->updateBackground(small, btnAction, selectionColor));

			String key = ACTIONS_PREF_KEY + KEY_SEP + a.getClass().getCanonicalName();
			
			boolean dogenerate = InstanceScope.INSTANCE.getNode(CyberTrackerPlugIn.PLUGIN_ID).getBoolean(key, true);
			btnAction.setSelection(dogenerate);
			WidgetElement.setCSSClass(small, "donotstyle"); //$NON-NLS-1$
			updateBackground(small, btnAction, selectionColor);
			
		}
		
		setTitle(Messages.CtNavigationExportDialog_DialogTitle);
		setMessage(Messages.CtNavigationExportDialog_DialogMessage);
		getShell().setText(Messages.CtNavigationExportDialog_ShellTitle);
		return parent;
	}
	
	private void updateBackground(Composite small, Button btnItem, Color selectionColor) {
		if (btnItem.getSelection()) {
			small.setBackground(selectionColor);
		}else {
			small.setBackground(small.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
		}
	}
	@Override
	public boolean isResizable() {
		return true;
	}
}
