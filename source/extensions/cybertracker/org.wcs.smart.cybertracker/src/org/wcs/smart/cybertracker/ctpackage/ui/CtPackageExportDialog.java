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
package org.wcs.smart.cybertracker.ctpackage.ui;

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
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.wcs.smart.cybertracker.CyberTrackerPlugIn;
import org.wcs.smart.cybertracker.internal.Messages;
import org.wcs.smart.ui.SmartStyledDialog;
import org.wcs.smart.ui.properties.DialogConstants;

/**
 * Dialog to display ct package export options to user
 * 
 * @author Emily
 *
 */
public class CtPackageExportDialog extends SmartStyledDialog {

	private static final String KEY_SEP = "|"; //$NON-NLS-1$
	private static final String ACTIONS_PREF_KEY = "org.wcs.smart.cybertracker.ctpackage.ui.CtPackageExportDialog.action"; //$NON-NLS-1$
	private static final String GENERATE_PREF_KEY = "org.wcs.smart.cybertracker.ctpackage.ui.CtPackageExportDialog.dogenerate"; //$NON-NLS-1$
	
	private boolean requireUpdateOp;
	private List<ICtExportAction> actions;
	private Button btnGenerate;
	private List<Button> actionButtons;
	
	private boolean doGenerate = false;
	private List<ICtExportAction> selectedActions;
	
	public CtPackageExportDialog(Shell parent, boolean requireUpdateOp,List<ICtExportAction> actions) {
		super(parent);
		this.requireUpdateOp = requireUpdateOp;
		this.actions = actions;
	}

	public boolean getDoGenerate() { return this.doGenerate; }
	public List<ICtExportAction> getSelectedActions() { return this.selectedActions;}
	
	public Point getInitialSize() {
		Point size = super.getInitialSize();
		return new Point(350, size.y);
	}
	
	@Override
	public void okPressed() {
		selectedActions = new ArrayList<>();
		
		doGenerate = btnGenerate.getSelection();
		for (Button b : actionButtons) {
			ICtExportAction action = (ICtExportAction)b.getData();
			if (b.getSelection()) {
				selectedActions.add(action);
				InstanceScope.INSTANCE.getNode(CyberTrackerPlugIn.PLUGIN_ID).putBoolean(ACTIONS_PREF_KEY + KEY_SEP + action.getClass().getCanonicalName(), true);
			}else {
				InstanceScope.INSTANCE.getNode(CyberTrackerPlugIn.PLUGIN_ID).putBoolean(ACTIONS_PREF_KEY + KEY_SEP + action.getClass().getCanonicalName(), false); 
			}
		}
		
		InstanceScope.INSTANCE.getNode(CyberTrackerPlugIn.PLUGIN_ID).putBoolean(GENERATE_PREF_KEY, doGenerate);
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
		((GridLayout)main.getLayout()).marginWidth = 0;
		((GridLayout)main.getLayout()).marginHeight = 0;
		main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		main.setBackground(main.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));

		
		actionButtons = new ArrayList<>();
		
		Color selectionColor = new Color(main.getDisplay(), 226, 241, 255);
		main.addListener(SWT.Dispose, e-> selectionColor.dispose());

		for (ICtExportAction a : actions) {
			Composite small = new Composite(main, SWT.NONE);
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
			WidgetElement.setCSSClass(small, "donotstyle");
			updateBackground(small, btnAction, selectionColor);
			
		}
		

		Label l = new Label(main, SWT.SEPARATOR | SWT.HORIZONTAL);
		l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		btnGenerate = new Button(main, SWT.CHECK);
		btnGenerate.setEnabled(requireUpdateOp);
		btnGenerate.setText(Messages.CtPackageExportDialog_RegenerateOp);
		btnGenerate.setToolTipText(Messages.CtPackageExportDialog_regenerateTooltip);
		btnGenerate.setBackground(main.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
		if (requireUpdateOp) {
			boolean dogenerate = InstanceScope.INSTANCE.getNode(CyberTrackerPlugIn.PLUGIN_ID).getBoolean(GENERATE_PREF_KEY, true);
			btnGenerate.setSelection(dogenerate);
		}else {
			btnGenerate.setSelection(true);
		}

		getShell().setText(Messages.CtPackageExportDialog_Title);
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
