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

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ui.part.MultiPageEditorPart;
import org.hibernate.Session;
import org.wcs.smart.er.EcologicalRecordsPlugIn;
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

	@Override
	protected void createPages() {
		try {
			summaryPage = new SurveyDesignSummaryEditorPage(this);
			int i = addPage(summaryPage, getEditorInput());
			setPageText(i, "Summary");
		
//			super.setPartName(getSurveyDesign().getName());
		}catch (Exception ex) {
			EcologicalRecordsPlugIn.log("Error creating pages.", ex);
		}
		
	}

	public SurveyDesign getSurveyDesign() {
		if (surveyDesign == null){
			byte[] puuid = ((SurveyDesignEditorInput) getEditorInput()).getUuid();
			Session session = HibernateManager.openSession();
			//load patrol items so don't have lazy loading issues later.
			session.beginTransaction();
			surveyDesign = (SurveyDesign) session.load(SurveyDesign.class, puuid);
			surveyDesign.getNames().size();
			surveyDesign.getConfigurableModel().getNames().size();
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
