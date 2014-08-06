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

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ui.part.MultiPageEditorPart;
import org.hibernate.Session;
import org.wcs.smart.er.EcologicalRecordsPlugIn;
import org.wcs.smart.er.ISurveyEventListener;
import org.wcs.smart.er.SurveyEventHandler;
import org.wcs.smart.er.SurveyEventHandler.EventType;
import org.wcs.smart.er.model.SurveyDesign;
import org.wcs.smart.hibernate.HibernateManager;

/**
 * The Survey Design Editor
 * 
 * @author elitvin
 * @since 3.0.0
 */
public class SurveyDesignEditor extends MultiPageEditorPart {

	public static final String ID = "org.wcs.smart.er.SurveyDesignEditor"; //$NON-NLS-1$

	private SurveyDesign surveyDesign;
	private SurveyDesignSummaryEditorPage summaryPage;
	
	private ISurveyEventListener modifiedListener = new ISurveyEventListener() {
		@Override
		public void event(Object o) {
			if (o instanceof SurveyDesign) {
				SurveyDesign source = (SurveyDesign) o;
				byte[] uuid = ((SurveyDesignEditorInput) getEditorInput()).getUuid();
				if (Arrays.equals(source.getUuid(), uuid)) {
					surveyDesign = null; //this will force the intelligence to be fully reloaded as it might be changed from outside
					summaryPage.initValues();
				}
			}
		}
	};

	private ISurveyEventListener deleteListener = new ISurveyEventListener() {
		@Override
		public void event(Object o) {
			if (o instanceof SurveyDesign) {
				SurveyDesign source = (SurveyDesign) o;
				byte[] uuid = ((SurveyDesignEditorInput) getEditorInput()).getUuid();
				if (Arrays.equals(source.getUuid(), uuid)) {
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
	
	@Override
	protected void createPages() {
		try {
			summaryPage = new SurveyDesignSummaryEditorPage(this);
			int i = addPage(summaryPage, getEditorInput());
			setPageText(i, "Summary");
		
			super.setPartName(getSurveyDesign().getName());
		}catch (Exception ex) {
			EcologicalRecordsPlugIn.log("Error creating pages.", ex);
		}
		
	}

	public SurveyDesign getSurveyDesign() {
		if (surveyDesign == null) {
			byte[] puuid = ((SurveyDesignEditorInput) getEditorInput()).getUuid();
			Session session = HibernateManager.openSession();
			session.clear(); //for some reason this may be active session with attached old survey design
			//load patrol items so don't have lazy loading issues later.
			session.beginTransaction();
			surveyDesign = (SurveyDesign) session.load(SurveyDesign.class, puuid);
			surveyDesign.getNames().size();
			if (surveyDesign.getConfigurableModel() != null) {
				surveyDesign.getConfigurableModel().getNames().size();
			}
			session.getTransaction().commit();
			session.close();
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

}
