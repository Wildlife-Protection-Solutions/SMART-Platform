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
package org.wcs.smart.intelligence.ui.patrol;

import java.util.List;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.hibernate.Session;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.intelligence.IntelligenceHibernateManager;
import org.wcs.smart.intelligence.internal.Messages;
import org.wcs.smart.intelligence.model.Intelligence;
import org.wcs.smart.intelligence.ui.patrol.PatrolMotivationComposite.IPartolMotivationChangeListener;
import org.wcs.smart.patrol.SmartPatrolPlugIn;
import org.wcs.smart.patrol.model.Patrol;
import org.wcs.smart.ui.properties.AbstractPropertyJHeaderDialog;

/**
 * Dialog to edit intelligence list that motivated the patrol.
 * 
 * @author elitvin
 * @since 1.0.0
 */
public class EditPatrolMotivationDialog extends AbstractPropertyJHeaderDialog {
	
	private Patrol patrol;
	private List<Intelligence> selectedList;
	
	private PatrolMotivationComposite content;

	private boolean savePerformed = false;;
	
	public EditPatrolMotivationDialog(Shell parent, Patrol patrol, List<Intelligence> selectedList) {
		super(parent, Messages.EditPatrolMotivationDialog_Title);
		this.patrol = patrol;
		this.selectedList = selectedList;
	}

	@Override
	protected Composite createContent(Composite parent) {
		content = new PatrolMotivationComposite(parent, SWT.NONE);
		content.addInputChangeListener(new IPartolMotivationChangeListener() {
			@Override
			public void inputChanged() {
				setChangesMade(true);
				String errorMessage = content.getErrorMessage();
				EditPatrolMotivationDialog.this.setErrorMessage(errorMessage);
				if (getButton(IDialogConstants.OK_ID) != null){
					getButton(IDialogConstants.OK_ID).setEnabled(errorMessage == null);
				}
			}
		});
		Session s = HibernateManager.openSession();
		try{
			content.initFromModel(patrol, s, selectedList);
		}finally{
			s.close();
		}
		setChangesMade(false);
		
		setTitle(Messages.IntelligencePatrolWizardPage_PageTitle);
		setMessage(Messages.IntelligencePatrolWizardPage_Message);
		return content;
	}

	@Override
	protected boolean performSave() {
		content.updateModel(patrol);
		Session s = HibernateManager.openSession();
		s.beginTransaction();
		try {
			IntelligenceHibernateManager.savePatrolIntelligences(s, patrol, content.getCurrentIntelligences());
			s.getTransaction().commit();
		} catch (Exception ex) {
			s.getTransaction().rollback();
			SmartPatrolPlugIn.displayLog(Messages.EditPatrolMotivationDialog_Save_Error + ex.getLocalizedMessage(), ex);
			return false;
		} finally {
			s.close();
		}
		setChangesMade(false);
		savePerformed = true;
		return true;
	}

	public boolean isSavePerformed() {
		return savePerformed;
	}
	
}
