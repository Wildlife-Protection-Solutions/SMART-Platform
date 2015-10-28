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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.dialogs.IPageChangingListener;
import org.eclipse.jface.dialogs.PageChangingEvent;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.widgets.Composite;
import org.hibernate.Session;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.patrol.PatrolEventManager;
import org.wcs.smart.patrol.PatrolHibernateManager;
import org.wcs.smart.patrol.SmartPatrolPlugIn;
import org.wcs.smart.patrol.internal.Messages;
import org.wcs.smart.patrol.model.Patrol;
import org.wcs.smart.patrol.ui.NewPatrolWizardPage;

/**
 * Wizard to create a new patrol.
 * 
 * @author Emily
 * @since 1.0.0
 */
public class CreatePatrolWizard extends Wizard implements IPageChangingListener {

	public static final String EXTENSION_ID = "org.wcs.smart.patrol.wizardpage"; //$NON-NLS-1$
	
	private boolean completedOK = false;

	private Patrol patrol = null;
	private Session session = null;

	private boolean canFinish = false;
	private IWizardPage lastPage = null;

	/**
	 * Creates a new wizard.
	 */
	public CreatePatrolWizard() {
		setWindowTitle(Messages.CreatePatrolWizard_Title);

		patrol = new Patrol();
		patrol.setConservationArea(SmartDB.getCurrentConservationArea());
		Session s = getSession();
		s.beginTransaction();
		try{
			patrol.setId(PatrolHibernateManager.generatePatrolId(patrol, s));
		}finally{
			s.getTransaction().rollback();
		}
	}

	/**
	 * Sets if the wizard can finish
	 * 
	 * @param canFinish
	 *            if the wizard can finish
	 */
	public void setCanFinish(boolean canFinish) {
		this.canFinish = canFinish;
		getContainer().updateButtons();
	}

	/**
	 * Closes the active session
	 */
	@Override
	public void dispose() {
		super.dispose();
		if (session != null && session.isOpen()) {
			session.close();
		}
	}

	@Override
	public boolean canFinish() {
		return super.canFinish() && this.canFinish;
	}

	/**
	 * 
	 * @return the current patrol being created
	 */
	public Patrol getPatrol() {
		return this.patrol;
	}

	/**
	 * Creates a new session and attaches the current conservation area.
	 * 
	 * @return
	 */
	public Session getSession() {
		if (session == null || !session.isOpen()) {
			session = PatrolHibernateManager.openSession();
		}
		return session;
	}

	@Override
	public void addPages() {
		((WizardDialog) getContainer()).addPageChangingListener(this);
		List<NewPatrolWizardPage> pages = findPages();
		if (pages == null){
			throw new IllegalStateException("Wizard pages cannot be null"); //$NON-NLS-1$
		}
		for (NewPatrolWizardPage p : pages){
			super.addPage(p);
		}
	}
	
	
	/**
	 * @return gets all hibernate mappings
	 */
	private List<NewPatrolWizardPage> findPages(){
		
		HashMap<String, NewPatrolWizardPage> idToPage = new HashMap<String, NewPatrolWizardPage>();
		List<NewPatrolWizardPage> thisitems = new ArrayList<NewPatrolWizardPage>();
		List<NewPatrolWizardPage> items = new ArrayList<NewPatrolWizardPage>();
		
		List<Object[]> sortRules = new ArrayList<Object[]>();
		
		if (Platform.getExtensionRegistry() == null) return Collections.emptyList();
		IConfigurationElement[] config = Platform.getExtensionRegistry().getConfigurationElementsFor(EXTENSION_ID);
		try {
			for (IConfigurationElement e : config) {
				NewPatrolWizardPage page = (NewPatrolWizardPage)e.createExecutableExtension("class"); //$NON-NLS-1$
				String i = e.getAttribute("id"); //$NON-NLS-1$
				idToPage.put(i, page);
				String sort = e.getAttribute("location"); //$NON-NLS-1$
				if (sort != null && sort.trim().length() > 0){
					sortRules.add(new Object[]{sort, page});
				}
				if (e.getContributor().getName().equals(SmartPatrolPlugIn.PLUGIN_ID)){
					thisitems.add(page);
				}else{
					items.add(page);
				}
			}
		}catch (Exception ex){
			SmartPatrolPlugIn.displayLog(Messages.CreatePatrolWizard_ErrorCreatingWizardPages, ex);
			return null;
		}
		
		//apply sort rules
		thisitems.addAll(items);
		for (Object[] sort : sortRules){
			try{
				String[] bits = ((String)sort[0]).split("="); //$NON-NLS-1$
				if (bits.length != 2){
					continue;
				}
				String targetId = bits[1];
				NewPatrolWizardPage sortTargetPage = idToPage.get(targetId);
				NewPatrolWizardPage sortSource = (NewPatrolWizardPage) sort[1];
				
				int targetIndex = thisitems.indexOf(sortTargetPage);
				int sourceIndex = thisitems.indexOf(sortSource);
				
				if (targetIndex < 0 || sourceIndex < 0){
					continue;
				}
				if (bits[0].toLowerCase().equals("before")){ //$NON-NLS-1$
					thisitems.remove(sourceIndex);
					thisitems.add(targetIndex, sortSource);
				}else if (bits[0].toLowerCase().endsWith("after")){ //$NON-NLS-1$
					thisitems.remove(sourceIndex);
					thisitems.add(targetIndex + 1, sortSource);
				}
			}catch (Exception ex){
				//eat this error
				//ex.printStackTrace();
			}
		}
		return thisitems;
	}
	
	@Override
	 public void createPageControls(Composite pageContainer) {
		 super.createPageControls(pageContainer);
		 ((NewPatrolWizardPage)getPages()[0]).initModel(patrol, getSession());
	 }

	/**
	 * 
	 * @return true if the wizard completed okay with no errors; false if error
	 *         occured while finishing wizard
	 */
	public boolean isCompletedOk() {
		return completedOK;
	}

	/**
	 * Creates the patrol leg days then saved the patrol to the database.
	 */
	@Override
	public boolean performFinish() {
		if (lastPage instanceof NewPatrolWizardPage) {
			((NewPatrolWizardPage) lastPage).updateModel(this.patrol, getSession());
		}

		Session session = PatrolHibernateManager.openSession();
		session.beginTransaction();
		boolean ret = true;
		try{
			patrol.createLegDays(session);
			PatrolHibernateManager.savePatrol(patrol, session, false);
			for (IWizardPage p : getPages()){
				if (p instanceof NewPatrolWizardPage){
					((NewPatrolWizardPage) p).save(patrol, session);
				}
			}
			session.getTransaction().commit();
		}catch (Exception ex){
			ret = false;
			SmartPatrolPlugIn.displayLog(Messages.PatrolHibernateManager_Error_CouldNoSavePatrol + ex.getLocalizedMessage(), ex);
			session.getTransaction().rollback();			
		}finally{
			session.close();
		}

		if (!ret)
			return false;

		// fire events
		PatrolEventManager.getInstance().patrolAdded(getPatrol());
		
		return ret;
	}

	/**
	 * @see org.eclipse.jface.dialogs.IPageChangingListener#handlePageChanging(org.eclipse.jface.dialogs.PageChangingEvent)
	 */
	@Override
	public void handlePageChanging(PageChangingEvent event) {
		
		if (event.getCurrentPage() instanceof NewPatrolWizardPage) {
			boolean movingNext = event.getTargetPage().equals(((WizardPage)event.getCurrentPage()).getNextPage());
			boolean update = ((NewPatrolWizardPage) event.getCurrentPage()).updateModel(patrol, getSession()); 
			if (movingNext && !update) {
				//only if moving to the next page to we disallow this event; if moving backwards we are ok
				event.doit = false;
				return;
			}
		}
		
		if (event.getTargetPage() instanceof NewPatrolWizardPage) {
			((NewPatrolWizardPage) event.getTargetPage()).initModel(patrol,
					getSession());
		}

		if (((IWizardPage) event.getTargetPage()).getNextPage() == null){
			setCanFinish(true);
		}else{
			setCanFinish(false);
		}
		

		if (event.doit) {
			lastPage = (IWizardPage) event.getTargetPage();
		}
	}

}
