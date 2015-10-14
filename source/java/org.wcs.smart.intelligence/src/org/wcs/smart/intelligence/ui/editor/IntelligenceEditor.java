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
package org.wcs.smart.intelligence.ui.editor;

import java.util.UUID;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.part.MultiPageEditorPart;
import org.hibernate.Session;
import org.locationtech.udig.project.internal.Map;
import org.locationtech.udig.project.ui.internal.MapPart;
import org.locationtech.udig.project.ui.tool.IMapEditorSelectionProvider;
import org.wcs.smart.common.attachment.ISmartAttachment;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.intelligence.IntelligenceEventManager;
import org.wcs.smart.intelligence.IntelligenceEventManager.EventType;
import org.wcs.smart.intelligence.IntelligenceEventManager.IIntelligenceEventListener;
import org.wcs.smart.intelligence.IntelligencePlugIn;
import org.wcs.smart.intelligence.internal.Messages;
import org.wcs.smart.intelligence.model.Intelligence;

/**
 * The Intelligence Editor
 * 
 * @author elitvin
 * @since 1.0.0
 */
public class IntelligenceEditor extends MultiPageEditorPart implements MapPart{

	public static final String ID = "org.wcs.smart.intelligence.IntelligenceEditor"; //$NON-NLS-1$

	private Intelligence intelligence;
	
	private IntelligenceEditorMapPage mapPage;
	private IntelligenceSummaryEditorPage summaryPage;

	
	/**
	 * listener for intelligence change events.
	 */
	private IIntelligenceEventListener intelligenceListener = new IIntelligenceEventListener(){
		@Override
		public void eventFired(int type, Intelligence source) {
			UUID uuid = ((IntelligenceEditorInput) getEditorInput()).getUuid();
			if (source.getUuid().equals(uuid)) {
				intelligence = null; //this will force the intelligence to be fully reloaded as it might be changed from outside
				setPartName(getIntelligence().getName());
				summaryPage.initValues();
				mapPage.refresh();
				
			}
		}
	};

	/**
	 * listener for intelligence delete events.
	 */
	private IIntelligenceEventListener deleteListener = new IIntelligenceEventListener(){
		@Override
		public void eventFired(int type, Intelligence source) {
			if (source.equals(IntelligenceEditor.this.intelligence)) {
				//close this editor
				IntelligenceEditor.this.getEditorSite().getWorkbenchWindow().getShell().getDisplay().asyncExec(new Runnable(){
					@Override
					public void run() {
						IntelligenceEditor.this.getEditorSite().getWorkbenchWindow().getActivePage().closeEditor(IntelligenceEditor.this, false);					
					}
				});
			}
		}
	};
	
	/**
	 * Default constructor
	 */
	public IntelligenceEditor() {
		super();
		IntelligenceEventManager.getInstance().addListener(EventType.INTELLIGENCE_MODIFIED, intelligenceListener);
		IntelligenceEventManager.getInstance().addListener(EventType.INTELLIGENCE_DELETED, deleteListener);
	}

	@Override
	public void dispose() {
		
		IntelligenceEventManager.getInstance().removeListener(EventType.INTELLIGENCE_MODIFIED, intelligenceListener);
		IntelligenceEventManager.getInstance().removeListener(EventType.INTELLIGENCE_DELETED, deleteListener);
		super.dispose();
	}
	

	@Override
	public boolean isDirty() {
		return false;
	}

	
	public Intelligence getIntelligence(){
		if (intelligence == null){
			UUID puuid = ((IntelligenceEditorInput) getEditorInput()).getUuid();
			Session session = HibernateManager.openSession();
			//load patrol items so don't have lazy loading issues later.
			session.beginTransaction();
			intelligence = (Intelligence) session.load(Intelligence.class, puuid);
			if (intelligence.getPatrol() != null) {
				intelligence.getPatrol().getId();
			}
			if (intelligence.getInformant() != null) {
				intelligence.getInformant().getId();
			}
			intelligence.getNames().size();
			if (intelligence.getSource() != null) {
				intelligence.getSource().getNames().size();
			}
			intelligence.getPoints().size();
			for (ISmartAttachment a : intelligence.getAttachments()){
				try {
					a.computeFileLocation(session);
				} catch (Exception e) {
					IntelligencePlugIn.log(e.getMessage(), e);
				}
			}
			session.getTransaction().commit();
			session.close();
		}
		return intelligence;
	}

	

	
	
	@Override
	public void doSave(IProgressMonitor monitor) {
		// nothing
	}

	@Override
	public void setFocus() {
		summaryPage.setFocus();
	}

	@Override
	public void doSaveAs() {
		//not allowed
	}

	@Override
	public boolean isSaveAsAllowed() {
		return false;
	}

	@Override
	protected void createPages() {
		try{
			summaryPage = new IntelligenceSummaryEditorPage(this);
			int i = addPage(summaryPage, getEditorInput());
			setPageText(i, Messages.IntelligenceEditor_SummaryPageName);
		
			mapPage = new IntelligenceEditorMapPage(this);
			i = addPage(mapPage, getEditorInput());
			setPageText(i, Messages.IntelligenceEditor_MapPageName);
			
			super.setPartName(getIntelligence().getName());
		}catch (Exception ex){
			IntelligencePlugIn.log(Messages.IntelligenceEditor_ErrorCreatingPages, ex);
			throw new RuntimeException(Messages.IntelligenceEditor_ErrorCreatingPages + ex.getMessage(), ex);
		}
	}

	@Override
	public Map getMap() {
		return mapPage.getMap();
	}

	@Override
	public void openContextMenu() {
		mapPage.openContextMenu();
	}

	@Override
	public void setFont(Control textArea) {
		mapPage.setFont(textArea);
		
	}

	@Override
	public void setSelectionProvider(
			IMapEditorSelectionProvider selectionProvider) {
		mapPage.setSelectionProvider(selectionProvider);
	}

	@Override
	public IStatusLineManager getStatusLineManager() {
		return mapPage.getStatusLineManager();
	}

}
