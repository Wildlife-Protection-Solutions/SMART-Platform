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
package org.wcs.smart.er.ui.surveydesign.editor;

import java.util.Arrays;
import java.util.UUID;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.part.MultiPageEditorPart;
import org.hibernate.Session;
import org.locationtech.udig.project.internal.Map;
import org.locationtech.udig.project.ui.internal.MapPart;
import org.locationtech.udig.project.ui.tool.IMapEditorSelectionProvider;
import org.wcs.smart.ca.Label;
import org.wcs.smart.er.EcologicalRecordsPlugIn;
import org.wcs.smart.er.ISurveyEventListener;
import org.wcs.smart.er.SurveyEventHandler;
import org.wcs.smart.er.SurveyEventHandler.EventType;
import org.wcs.smart.er.SurveyPermissionManager;
import org.wcs.smart.er.internal.Messages;
import org.wcs.smart.er.model.SurveyDesign;
import org.wcs.smart.hibernate.HibernateManager;

/**
 * The Survey Design Editor
 * 
 * @author elitvin
 * @since 3.0.0
 */
public class SurveyDesignEditor extends MultiPageEditorPart implements MapPart{

	public static final String ID = "org.wcs.smart.er.SurveyDesignEditor"; //$NON-NLS-1$

	private SurveyDesign surveyDesign;
	private SurveyDesignSummaryEditorPage summaryPage;
	private SamplingUnitEditorPage suPage;
	private SurveyDesignDataPage dataPage;
	
	private ISurveyEventListener modifiedListener = new ISurveyEventListener() {
		@Override
		public void event(Object o) {
			if (o instanceof SurveyDesign) {
				SurveyDesign source = (SurveyDesign) o;
				UUID uuid = ((SurveyDesignEditorInput) getEditorInput()).getUuid();
				if (source.getUuid().equals(uuid)) {
					surveyDesign = null; //this will force the intelligence to be fully reloaded as it might be changed from outside
					getSurveyDesign();
					getSite().getShell().getDisplay().asyncExec(new Runnable(){
						@Override
						public void run() {
							summaryPage.initValues();
							suPage.initValues();	
							dataPage.initValues();
							setPartName(surveyDesign.getName());
						}});
					
				}
			}
		}
	};

	private ISurveyEventListener deleteListener = new ISurveyEventListener() {
		@Override
		public void event(Object o) {
			if (o instanceof SurveyDesign) {
				SurveyDesign source = (SurveyDesign) o;
				UUID uuid = ((SurveyDesignEditorInput) getEditorInput()).getUuid();
				if (source.getUuid().equals(uuid)) {
					//close this editor
					SurveyDesignEditor.this.getEditorSite().getWorkbenchWindow().getShell().getDisplay().asyncExec(new Runnable(){
						@Override
						public void run() {
							SurveyDesignEditor.this.getEditorSite().getWorkbenchWindow().getActivePage().closeEditor(SurveyDesignEditor.this, false);					
						}
					});
				}
			}
		}
	};
	
	/**
	 * Default constructor
	 */
	public SurveyDesignEditor() {
		super();
		SurveyEventHandler.getInstance().addListener(EventType.SURVEY_DESIGN_MODIFIED, modifiedListener);
		SurveyEventHandler.getInstance().addListener(EventType.SURVEY_DESIGN_DELETED, deleteListener);
	}

	@Override
	public void dispose() {
		SurveyEventHandler.getInstance().removeListener(EventType.SURVEY_DESIGN_MODIFIED, modifiedListener);
		SurveyEventHandler.getInstance().removeListener(EventType.SURVEY_DESIGN_DELETED, deleteListener);
		super.dispose();
	}
	
	public String getEditMessage(){
		return SurveyPermissionManager.INSTANCE.canEditSurveyDesign();
	}
	public boolean canEdit(){
		return SurveyPermissionManager.INSTANCE.canEditSurveyDesign() == null;
	}
	
	@Override
	protected void createPages() {
		try {
			summaryPage = new SurveyDesignSummaryEditorPage(this);
			int i = addPage(summaryPage, getEditorInput());
			setPageText(i, Messages.SurveyDesignEditor_Page_Summary);
		
			suPage = new SamplingUnitEditorPage(this);
			i = addPage(suPage, getEditorInput());
			setPageText(i, Messages.SurveyDesignEditor_SamplingUnitPageName);
			
			dataPage = new SurveyDesignDataPage(this);
			i = addPage(dataPage, getEditorInput());
			setPageText(i, Messages.SurveyDesignEditor_SurveyDataPageName);
			
			super.setPartName(getSurveyDesign().getName());
		}catch (Exception ex) {
			EcologicalRecordsPlugIn.log(Messages.SurveyDesignEditor_Error_Pages, ex);
		}
		
	}

	public SurveyDesign getSurveyDesign() {
		if (surveyDesign == null) {
			UUID puuid = ((SurveyDesignEditorInput) getEditorInput()).getUuid();
			Session session = HibernateManager.openSession();
			session.beginTransaction();
			try{
				surveyDesign = (SurveyDesign) session.load(SurveyDesign.class, puuid);
				surveyDesign.getName();
				if (surveyDesign.getConfigurableModel() != null) {
					surveyDesign.getConfigurableModel().getNames().size();
				}
				surveyDesign.getMissionProperties().size();
				surveyDesign.getProperties().size();
				
				//ensure all languages are loaded; this is to support the translate link
				for (Label l : surveyDesign.getNames()){
					l.getLanguage().equals(null);
				}
			}catch (Exception ex){
				EcologicalRecordsPlugIn.log(ex.getMessage(), ex);
			}finally{
				session.getTransaction().rollback();
				session.close();
			}
		}
		return surveyDesign;
	}
	
	@Override
	public void doSave(IProgressMonitor monitor) {
		// nothing
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
	public Map getMap() {
		return suPage.getMap();
	}

	@Override
	public void openContextMenu() {
		suPage.openContextMenu();
	}

	@Override
	public void setFont(Control textArea) {
		suPage.setFont(textArea);
	}

	@Override
	public void setSelectionProvider(
			IMapEditorSelectionProvider selectionProvider) {
		suPage.setSelectionProvider(selectionProvider);
	}

	@Override
	public IStatusLineManager getStatusLineManager() {
		return suPage.getStatusLineManager();
	}

}
