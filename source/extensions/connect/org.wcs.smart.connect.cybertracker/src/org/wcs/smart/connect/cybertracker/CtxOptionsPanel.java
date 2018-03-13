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
package org.wcs.smart.connect.cybertracker;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.hibernate.Session;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.connect.cybertracker.internal.Messages;
import org.wcs.smart.connect.model.ConnectServer;
import org.wcs.smart.connect.ui.server.configure.IServerOptionsPanel;
import org.wcs.smart.cybertracker.model.CyberTrackerPropertiesOption;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.hibernate.SmartDB;

/**
 * Connect option panel for replicating archived ctx files to connect 
 * 
 * @author Emily
 *
 */
public class CtxOptionsPanel implements IServerOptionsPanel {

	private CyberTrackerPropertiesOption option;
	private Button btnExcludeCtxFiles; 
	private List<ModifyListener> listeners = new ArrayList<>();
	
	private boolean requiresRestart = false;
	private ConnectServer toUpdate;
	@Override
	public boolean isSupported(ConservationArea ca) {
		return !ca.getIsCcaa();
	}

	@Override
	public String getName() {
		return Messages.CtxOptionsPanel_Name;
	}

	@Override
	public String getDescription() {
		return getName();
	}

	@Override
	public void initValues(ConnectServer server, Session session) {
		this.toUpdate = server;
		option = QueryFactory.buildQuery(session, CyberTrackerPropertiesOption.class, 
				new Object[] {"conservationArea", SmartDB.getCurrentConservationArea()}, //$NON-NLS-1$
				new Object[] {"optionId", FilestoreWatcherConfigurator.EXCLUDE_CTX_DIR_OPTION}).uniqueResult(); //$NON-NLS-1$
		btnExcludeCtxFiles.setSelection(option == null || option.getBooleanValue());
	}

	@Override
	public void updateServer(ConnectServer server, Session session) {
		CyberTrackerPropertiesOption currentOption = QueryFactory.buildQuery(session, CyberTrackerPropertiesOption.class, 
				new Object[] {"conservationArea", SmartDB.getCurrentConservationArea()}, //$NON-NLS-1$
				new Object[] {"optionId", FilestoreWatcherConfigurator.EXCLUDE_CTX_DIR_OPTION}).uniqueResult(); //$NON-NLS-1$
		if (currentOption == null) {
			if (!btnExcludeCtxFiles.getSelection()) {
				//make a new option with false value
				CyberTrackerPropertiesOption op = new CyberTrackerPropertiesOption();
				op.setConservationArea(SmartDB.getCurrentConservationArea());
				op.setBooleanValue(Boolean.FALSE);
				op.setOptionId(FilestoreWatcherConfigurator.EXCLUDE_CTX_DIR_OPTION);
				session.save(op);
				requiresRestart = true;
			}
		}else {
			boolean newValue = btnExcludeCtxFiles.getSelection();
			//only make change if value has changed
			if (newValue != option.getBooleanValue()) {
				currentOption.setBooleanValue(newValue);
				option = currentOption;
				requiresRestart = true;
			}
		}
		session.flush();
	}

	private Shell getShell() {
		return btnExcludeCtxFiles.getShell();
	}
	
	@Override
	public void afterSave(ConnectServer server) {
		if (requiresRestart) {
			boolean restart = MessageDialog.openQuestion(getShell(), Messages.CtxOptionsPanel_RestartOpTitle, 
					Messages.CtxOptionsPanel_RestartOpMsg);
			if (restart) {
				PlatformUI.getWorkbench().restart();
			}
		}
	}

	@Override
	public boolean isValid() {
		return true;
	}

	@Override
	public Composite createComposite(Composite parent, boolean isEditable) {
		Composite part = new Composite(parent, SWT.NONE);
		part.setLayout(new GridLayout());
		part.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		Label l = new Label(part, SWT.WRAP);
		l.setText(Messages.CtxOptionsPanel_ReplicationInfo);
		l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		((GridData)l.getLayoutData()).widthHint = 120;
		
		if (toUpdate != null && toUpdate.getUuid() != null) {
			l = new Label(part, SWT.WRAP);
			l.setText(Messages.CtxOptionsPanel_RestartInfo);
			l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			((GridData)l.getLayoutData()).widthHint = 120;
		}
		
		btnExcludeCtxFiles = new Button(part, SWT.CHECK);
		btnExcludeCtxFiles.setText(Messages.CtxOptionsPanel_Option);
		btnExcludeCtxFiles.setToolTipText(Messages.CtxOptionsPanel_OptionTooltip);
		btnExcludeCtxFiles.addListener(SWT.Selection, e->{
			listeners.forEach(listener->listener.modifyText(new ModifyEvent(e)));
		});
		btnExcludeCtxFiles.setEnabled(isEditable);
		
		requiresRestart = false;
		return part;
	}

	@Override
	public void addChangeListener(ModifyListener listener) {
		listeners.add(listener);
	}

}
