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
package org.wcs.smart.connect.dataqueue.internal.ui.config;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.connect.ConnectPlugIn;
import org.wcs.smart.connect.dataqueue.ConnectDataQueuePlugin;
import org.wcs.smart.connect.dataqueue.internal.Messages;
import org.wcs.smart.connect.dataqueue.model.DataQueueProcessingOption;
import org.wcs.smart.connect.dataqueue.ui.IProcessingOptionPanel;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.ui.properties.DialogConstants;

/**
 * Dialog for configuring data queue processing options.
 * 
 * @author Emily
 *
 */
public class DataQueueProcessingOptionDialog extends TitleAreaDialog{

	public DataQueueProcessingOptionDialog(Shell parentShell) {
		super(parentShell);
	}

	private IProcessingOptionPanel[]  optionPanels = createOptionPanels();
	
	private IProcessingOptionPanel.ModifyListener listener = new IProcessingOptionPanel.ModifyListener() {
		@Override
		public void widgetChanged() {
			String error = null;
			for (IProcessingOptionPanel p : optionPanels){
				if (!p.isValid()){
					error = MessageFormat.format(Messages.DataQueueProcessingOptionDialog_PanelError, p.getName());
					break;
				}
			}
			getButton(IDialogConstants.OK_ID).setEnabled(error == null);
			setErrorMessage(error);
		}
	};
	
	@Override
	public void createButtonsForButtonBar(Composite parent){
		createButton(parent, IDialogConstants.OK_ID, DialogConstants.SAVE_TEXT, false);
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
		getButton(IDialogConstants.OK_ID).setEnabled(false);
	}
	
	@Override
	public void okPressed(){
		
		Session session = HibernateManager.openSession();
		try{
			session.beginTransaction();
			for (IProcessingOptionPanel pnl : optionPanels){
				pnl.update(session);
			}
			session.getTransaction().commit();
			getButton(IDialogConstants.OK_ID).setEnabled(false);
		}catch (Exception ex){
			ConnectDataQueuePlugin.displayLog(Messages.DataQueueProcessingOptionDialog_SaveError + ex.getMessage(), ex);
			return;
		}finally{
			session.close();
		}
	}
	
	
	@Override
	protected Control createDialogArea(Composite parent) {
		parent = (Composite) super.createDialogArea(parent);
		
		final Composite main = new Composite(parent, SWT.NONE);
		main.setLayout(new GridLayout());
		main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		if (optionPanels.length == 0){
			
			Label l = new Label(main, SWT.NONE);
			l.setText(Messages.DataQueueProcessingOptionDialog_NoOptions);
		}else{
			TabFolder tabConfig = new TabFolder(main, SWT.NONE);
			tabConfig.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true,3,1));
			for (IProcessingOptionPanel p : optionPanels){
				TabItem ti = new TabItem(tabConfig, SWT.DEFAULT);
				ti.setText(p.getName());
				ti.setControl(p.createComposite(tabConfig));
				p.addChangeListener(listener);
			}
			initControls();
		}
		setTitle(Messages.DataQueueProcessingOptionDialog_Title);
		getShell().setText(Messages.DataQueueProcessingOptionDialog_Title);
		setMessage(Messages.DataQueueProcessingOptionDialog_Message);
		
		return main;
	}
	
	private void initControls(){

		HashMap<String, DataQueueProcessingOption> optionMap = new HashMap<String, DataQueueProcessingOption>();
		Session session = HibernateManager.openSession();
		session.beginTransaction();
		try{
			List<DataQueueProcessingOption> options = session.createCriteria(DataQueueProcessingOption.class)
			.add(Restrictions.eq("id.conservationArea", SmartDB.getCurrentConservationArea().getUuid())) //$NON-NLS-1$
			.list();
			
			for (DataQueueProcessingOption op : options){
				optionMap.put(op.getOptionKey(), op);
			}
		}finally{
			session.getTransaction().rollback();
			session.close();
		}
		
		for (IProcessingOptionPanel pnl :optionPanels){
			pnl.initValues(optionMap);
		}
	}
	
	
	@Override
	public boolean isResizable(){
		return true;
	}
	
	
	private synchronized static IProcessingOptionPanel[] createOptionPanels(){
		if (Platform.getExtensionRegistry() == null) return new IProcessingOptionPanel[0];
		ArrayList<IProcessingOptionPanel> items = new ArrayList<IProcessingOptionPanel>();
		IConfigurationElement[] config = Platform.getExtensionRegistry().getConfigurationElementsFor(IProcessingOptionPanel.EXTENSION_ID);
		for (IConfigurationElement e : config) {
			try{
				items.add((IProcessingOptionPanel)e.createExecutableExtension("class")); //$NON-NLS-1$
			}catch (Exception ex){
				ConnectPlugIn.log(ex.getMessage(), ex);
			}
		}
		return items.toArray(new IProcessingOptionPanel[items.size()]);
	}
}
