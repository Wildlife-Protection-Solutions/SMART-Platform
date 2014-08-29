package org.wcs.smart.er.ui.surveydesign.editor;

import java.util.Arrays;
import java.util.Date;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IPersistableElement;
import org.wcs.smart.er.EcologicalRecordsPlugIn;

public class SurveyEditorInput implements IEditorInput {

	private byte[] uuid;
	private String id;
	private Date startDate;
	private String designName;
	/**
	 * Constructor
	 */
	public SurveyEditorInput(String id, byte[] uuid, Date startDate, String designName) {
		this.uuid = uuid;
		this.id = id;
		this.startDate = startDate;
		this.designName = designName;
	}
	
	public String getSurveyDesignName(){
		return this.designName;
	}
	
	public byte[] getUuid() {
		return uuid;
	}
	
	public Date getStartDate(){
		return this.startDate;
	}
	
	public String getSurveyId(){
		return this.id;
	}
	
	@SuppressWarnings("rawtypes")
	@Override
	public Object getAdapter(Class adapter) {
		return null;
	}

	@Override
	public boolean exists() {
		return false;
	}

	@Override
	public ImageDescriptor getImageDescriptor() {
		return EcologicalRecordsPlugIn.getDefault().getImageRegistry().getDescriptor(EcologicalRecordsPlugIn.SURVEY_ICON);
	}

	@Override
	public String getName() {
		return this.id + " [" + designName + "]"; 
	}

	@Override
	public IPersistableElement getPersistable() {
		return null;
	}

	@Override
	public String getToolTipText() {
		return ""; //$NON-NLS-1$
	}

	@Override
	public boolean equals(Object other){
		if (other instanceof SurveyDesignEditorInput){
			return Arrays.equals(uuid, ((SurveyDesignEditorInput) other).getUuid());
		}
		return false;
	}
	
	@Override
	public int hashCode(){
		return Arrays.hashCode(uuid);
	}
}
