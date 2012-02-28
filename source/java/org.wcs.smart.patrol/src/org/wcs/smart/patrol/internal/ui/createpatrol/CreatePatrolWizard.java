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
package org.wcs.smart.patrol.internal.ui.createpatrol;

import org.eclipse.jface.dialogs.IPageChangingListener;
import org.eclipse.jface.dialogs.PageChangingEvent;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.hibernate.Session;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.patrol.PatrolEventManager;
import org.wcs.smart.patrol.PatrolHibernateManager;
import org.wcs.smart.patrol.internal.ui.editor.PatrolEditor;
import org.wcs.smart.patrol.internal.ui.editor.PatrolEditorInput;
import org.wcs.smart.patrol.model.Patrol;

/**
 * Wizard to create a new patrol.
 * 
 * @author Emily
 * @since 1.0.0
 */
public class CreatePatrolWizard extends Wizard implements IPageChangingListener{

	private boolean completedOK = false;

	private Patrol patrol = null;
	private Session session = null;
	
	private boolean canFinish = false;
	private IWizardPage lastPage = null;
	
	/**
	 * Creates a new wizard.
	 */
	public CreatePatrolWizard() {
		setWindowTitle("Create New Patrol");
		
		patrol = new Patrol();
		patrol.setConservationArea(SmartDB.getCurrentConservationArea());
	}
	
	/**
	 * Sets if the wizard can finish
	 * @param canFinish if the wizard can finish
	 */
	public void setCanFinish(boolean canFinish){
		this.canFinish = canFinish;
		getContainer().updateButtons();
	}
	
	@Override
	public boolean canFinish(){
		return super.canFinish() && this.canFinish;
	}
	/**
	 * 
	 * @return the current patrol being created
	 */
	public Patrol getPatrol(){
		return this.patrol;
	}

	/**
	 * Creates a new session and attaches
	 * the current conservation area.
	 * 
	 * @return
	 */
	public Session getSession(){
		if (session == null || !session.isOpen()){
			session = PatrolHibernateManager.openSession();
			session.refresh(patrol.getConservationArea());
		}
		return session;
	}
	
	@Override
	public void addPages() {
		((WizardDialog)getContainer()).addPageChangingListener(this);
		
		super.addPage(new PatrolTypeWizardPage());
		super.addPage(new TransportTypeWizardPage());
		super.addPage(new PatrolArmedWizardPage());
		super.addPage(new StationTeamWizardPage());
		super.addPage(new PatrolMandateWizardPage());
		super.addPage(new PatrolObjectiveWizardPage());
		super.addPage(new PatrolDateWizardPage());
		super.addPage(new PatrolMemberWizardPage());
		super.addPage(new PatrolLeaderWizardPage());
		super.addPage(new MultiLegWizardPage());
		super.addPage(new PatrolLegsWizardPage());
		
	}
	
	/**
	 * 
	 * @return true if the wizard completed okay with no errors; false if error occured
	 * while finishing wizard
	 */
	public boolean isCompletedOk(){
		return completedOK;
	}

	
	/**
	 * Creates the patrol leg days then saved the patrol to the database.
	 */
	@Override
	public boolean performFinish() {
		if (lastPage instanceof NewPatrolWizardPage){
			((NewPatrolWizardPage)lastPage).updateModel(this.patrol);
		}
		
		this.getPatrol().createLegDays();
		boolean ret = PatrolHibernateManager.savePatrol(getPatrol(), PatrolHibernateManager.openSession(), null);
		//fire events
		PatrolEventManager.getInstance().patrolAdded(getPatrol());
		//open in editor
		PatrolEditorInput input = new PatrolEditorInput(this.patrol.getUuid(), this.patrol.getId(), this.patrol.getPatrolType(), this.patrol.getStartDate());
		
		try {
			PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().openEditor(input, PatrolEditor.ID);
		} catch (PartInitException e) {
			//TODO:
			throw new RuntimeException(e);
		}
		
		return ret;
	}

	/**
	 * @see org.eclipse.jface.dialogs.IPageChangingListener#handlePageChanging(org.eclipse.jface.dialogs.PageChangingEvent)
	 */
	@Override
	public void handlePageChanging(PageChangingEvent event) {
		if (event.getCurrentPage() instanceof NewPatrolWizardPage){
			((NewPatrolWizardPage)event.getCurrentPage()).updateModel(patrol);
		}
		
		
		//last page of wizard
		if (event.getTargetPage().equals(getPages()[getPageCount()-1])){
			setCanFinish(true);
		}else{
			setCanFinish(false);
		}
		if (event.doit){
			lastPage = (IWizardPage) event.getTargetPage();
		}
	}

}
