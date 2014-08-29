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
package org.wcs.smart.er.ui.samplingunit.load;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.widgets.Display;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.common.control.WarningDialog;
import org.wcs.smart.er.EcologicalRecordsPlugIn;
import org.wcs.smart.er.SurveyEventHandler;
import org.wcs.smart.er.SurveyEventHandler.EventType;
import org.wcs.smart.er.internal.Messages;
import org.wcs.smart.er.model.SamplingUnit;
import org.wcs.smart.er.model.SamplingUnitAttribute;
import org.wcs.smart.er.model.SamplingUnitAttributeValue;
import org.wcs.smart.er.model.SurveyDesign;
import org.wcs.smart.hibernate.HibernateManager;

import au.com.bytecode.opencsv.CSVReader;

/**
 * Updates sampling unit attributes from a text delimited file.
 * 
 * @author Emily
 *
 */
public class ImportAttributes implements IRunnableWithProgress {

	private File file;
	private Character delimiter;
	private String idField;
	private HashMap<SamplingUnitAttribute, String> attributeFields;
	private SurveyDesign survey;

	private boolean error;
	
	/**
	 * 
	 * @param file text file
	 * @param delimiter delimiter 
	 * @param idField id field
	 * @param attributeFields mapping of attribute fields
	 * @param survey survey design
	 */
	public ImportAttributes(File file, Character delimiter, String idField,
			HashMap<SamplingUnitAttribute, String> attributeFields, SurveyDesign survey){
		this.file = file;
		this.delimiter = delimiter;
		this.idField = idField;
		this.attributeFields = attributeFields;
		this.survey = survey;
	}

	/**
	 * If error occurred
	 * @return
	 */
	public boolean hasError(){
		return error;
	}
	
	@Override
	public void run(IProgressMonitor monitor) throws InvocationTargetException,
			InterruptedException {
		
		
		error = false;
		CSVReader reader = null;
		Session session = HibernateManager.openSession();
		try {
			session.beginTransaction();
			reader = new CSVReader(new FileReader(file), delimiter);
			
			int fileCnt = 0;
			while(reader.readNext() != null){
				fileCnt++;
			}
			reader.close();
			
			monitor.beginTask(Messages.ImportAttributes_Progress1, fileCnt);
			
			reader = new CSVReader(new FileReader(file), delimiter);
			String[] headers = reader.readNext();
			monitor.worked(1);
			int idIndex = -1;
			HashMap<SamplingUnitAttribute, Integer> attributeIndex = new HashMap<SamplingUnitAttribute, Integer>();
			
			for (int i = 0; i < headers.length; i ++){
				if (headers[i].equals(idField)){
					idIndex = i;
				}
				for(Entry<SamplingUnitAttribute, String> v : attributeFields.entrySet()){
					if (v.getValue() != null){
						if (v.getValue().equals(headers[i])){
							attributeIndex.put(v.getKey(), i);
						}
					}
				}
			}
			if (idIndex < 0){
				throw new Exception(Messages.ImportAttributes_IdFieldNotFound);
			}
			final List<String> warnings = new ArrayList<String>();
			
			headers = reader.readNext();
			monitor.worked(1);
			int lineCnt=2;
			boolean changes = false;
			while(headers != null){
				monitor.subTask(MessageFormat.format(Messages.ImportAttributes_Progress2, new Object[]{lineCnt}));
				String suId = headers[idIndex];
				
				//find units to update
				List<SamplingUnit> units = findSamplingUnit(suId, session);
				if (units.size() == 0){
					warnings.add(MessageFormat.format(Messages.ImportAttributes_SuNotFound, new Object[]{suId, lineCnt})); 
					//perhaps warn users
				}else{
					//update attributes
					for(Entry<SamplingUnitAttribute, Integer> index : attributeIndex.entrySet()){
					
						String newValue = headers[index.getValue()];
						Double dValue = null;
						if (index.getKey().getType() == Attribute.AttributeType.NUMERIC){
							//parse value from string
							try{
								dValue = Double.parseDouble(newValue);
							}catch (Exception ex){
								//cannot compute value so skip
								warnings.add(MessageFormat.format(Messages.ImportAttributes_ConversionError, new Object[]{newValue, index.getKey().getName(), lineCnt})); 
								break;
							}
						}
					
						//find sua
						for (SamplingUnit unit : units){
							SamplingUnitAttributeValue toUpdate = null;
							for (SamplingUnitAttributeValue v : unit.getAttributes()){
								if (v.getSamplingUnitAttribute().equals(index.getKey())){
									toUpdate = v;
								}
							}
							//if not found create a new one
							if (toUpdate == null){
								toUpdate = new SamplingUnitAttributeValue();
								toUpdate.setSamplingUnit(unit);
								toUpdate.setSamplingUnitAttribute(index.getKey());
								unit.getAttributes().add(toUpdate);
							}
						
							if (toUpdate.getSamplingUnitAttribute().getType() == Attribute.AttributeType.NUMERIC){
								toUpdate.setDoubleValue(dValue);
							}else{
								toUpdate.setStringValue(newValue);
							}
							changes = true;
						}
					}
					
				}
			
				headers = reader.readNext();
				lineCnt++;
				monitor.worked(1);
			}
			
			if (warnings.size() > 0){
				final boolean[] canContinue = {false};
				
				Display.getDefault().syncExec(new Runnable(){

					@Override
					public void run() {
						WarningDialog wd = new WarningDialog(Display.getDefault().getActiveShell(),
								Messages.ImportAttributes_WarningsLabel, Messages.ImportAttributes_ErrorList, 
								warnings,
								new String[]{IDialogConstants.YES_LABEL, IDialogConstants.NO_LABEL},
								0);	
						if (wd.open() == 0){
							canContinue[0] = true;
						}
					}});
				if (!canContinue[0]){
					session.getTransaction().rollback();
					error = true;
					return;
				}
				
			}
			session.getTransaction().commit();
			
			if (changes){
				SurveyEventHandler.getInstance().fireEvent(EventType.SURVEY_DESIGN_MODIFIED, survey);
			}
			
			
		} catch (Exception e) {
			session.getTransaction().rollback();
			EcologicalRecordsPlugIn.displayLog(Messages.ImportAttributes_ErrorUpdating + "\n\n" + e.getMessage(), e); //$NON-NLS-1$ 
			error = true;
		}finally{
			if (reader != null){
				try {
					reader.close();
				} catch (IOException e) {
					EcologicalRecordsPlugIn.log(e.getMessage(), e);
				}
					
			}
			session.close();
			monitor.done();
		}
	}

	@SuppressWarnings("unchecked")
	private List<SamplingUnit> findSamplingUnit(String id, Session session){
		return session.createCriteria(SamplingUnit.class)
		.add(Restrictions.eq("surveyDesign", survey)) //$NON-NLS-1$
		.add(Restrictions.eq("id", id)).list(); //$NON-NLS-1$
	}
}
