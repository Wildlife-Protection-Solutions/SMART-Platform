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
package org.wcs.smart.er.ui.samplingunit.wizard;

import java.util.HashMap;
import java.util.List;

import org.eclipse.jface.dialogs.IPageChangingListener;
import org.eclipse.jface.dialogs.PageChangingEvent;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.hibernate.Session;
import org.wcs.smart.er.SurveyEventHandler;
import org.wcs.smart.er.SurveyEventHandler.EventType;
import org.wcs.smart.er.model.SamplingUnit;
import org.wcs.smart.er.model.SamplingUnit.SamplingUnitType;
import org.wcs.smart.er.model.SurveyDesign;
import org.wcs.smart.er.ui.samplingunit.load.CsvSamplingUnitImporter;
import org.wcs.smart.er.ui.samplingunit.load.ISamplingUnitImporter;
import org.wcs.smart.hibernate.HibernateManager;

/**
 * Import sampling unit wizard.
 * 
 * @author Emily
 *
 */
public class ImportWizard extends Wizard implements IPageChangingListener{

	private Session session;
	
	private SurveyDesign surveyDesign;
	
	private boolean canFinish = false;
	
	private TypePage page1;
	private FileWizardPage page2;
	private AttributePage page3;
	private BufferPage page4;
	
	/**
	 * Creates a new wizard
	 */
	public ImportWizard(SurveyDesign surveyDesign){
		session = HibernateManager.openSession();
	
		setNeedsProgressMonitor(true);
		this.surveyDesign = (SurveyDesign) session.load(SurveyDesign.class, surveyDesign.getUuid());
	}
	
	@Override
	public void dispose(){
		if (session.isOpen()){
			session.close();
		}
		
		super.dispose();
		
	}
	
	@Override
	public boolean canFinish(){
		if (canFinish){
			return super.canFinish();
		}
		return false;
	}

	
	@Override
	public boolean performFinish() {
		HashMap<Object, Object> params = new HashMap<Object, Object>();
		
		ISamplingUnitImporter importer = page2.getImporter();
		
		params.put(CsvSamplingUnitImporter.DELIMETER_KEY, page2.getDelimiter());
		
		SamplingUnitType type = page1.getType();
		if (page1.getType() == SamplingUnitType.OPEN_TRANSECT){
			type = page4.getType();
		}
		params.put(ISamplingUnitImporter.TYPE_KEY, type);
		
		Double buffer = page4.getArea();
		params.put(ISamplingUnitImporter.BUFFER_KEY, buffer);
		
		params.put(ISamplingUnitImporter.PROJECTION_KEY, page3.getProjection());
		
		params.put(ISamplingUnitImporter.ID_FIELD_KEY, page3.getIdField());
		
		params.put(ISamplingUnitImporter.X1_FIELD_KEY, page3.getX1Field());
		params.put(ISamplingUnitImporter.Y1_FIELD_KEY, page3.getY1Field());
		params.put(ISamplingUnitImporter.X2_FIELD_KEY, page3.getX2Field());
		params.put(ISamplingUnitImporter.Y2_FIELD_KEY, page3.getY2Field());
		
		params.putAll(page3.getAttributeFields());
		
		try {
			List<SamplingUnit> units = importer.importFile(page2.getFile(), params);
			
			session.beginTransaction();
			try{
				for (SamplingUnit su : units){
					su.setSurveyDesign(surveyDesign);
					session.save(su);
				}
				session.getTransaction().commit();
				

			}catch (Exception ex){
				//TODO
				ex.printStackTrace();
				session.getTransaction().rollback();
				return false;
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
		
		session.close();
		
		SurveyEventHandler.getInstance().fireEvent(EventType.SURVEY_DESIGN_MODIFIED, surveyDesign);
		return true;
	}

	
	/**
     * The <code>Wizard</code> implementation of this <code>IWizard</code>
     * method does nothing. Subclasses should extend if extra pages need to be
     * added before the wizard opens. New pages should be added by calling
     * <code>addPage</code>.
     */
	public void addPages() {
    	setWindowTitle("Import Sampling Units");
    	
    	page1 = new TypePage();
    	page2 = new FileWizardPage();
    	page3 = new AttributePage(surveyDesign, HibernateManager.getCaProjectionList(session));
    	page4 = new BufferPage();
    	
    	super.addPage(page1);
    	super.addPage(page2);
    	super.addPage(page3);
    	super.addPage(page4);
    	
    	((WizardDialog) getContainer()).addPageChangingListener(this);
    }

    public SamplingUnitType getSamplingUnitType(){
    	return page1.getType();
    }

	@Override
	public void handlePageChanging(PageChangingEvent event) {
		if (event.getTargetPage() == page4){
			canFinish = true;
		}else{
			canFinish = false;
		}
		
		if (event.getTargetPage() == page4){
			page4.setType(getSamplingUnitType());
		}
		if (event.getCurrentPage() == page2){
			String[] items = page2.getFieldNames();
			if (items == null){
				event.doit = false;
				return;
			}
		}
		
		if (event.getTargetPage() == page3){
			page3.setFields(page2.getImporter(), page2.getFieldNames());
		}
	}

}
