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
package org.wcs.smart.er.query.report.table;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.hibernate.Session;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.datamodel.Attribute.AttributeType;
import org.wcs.smart.data.oda.smart.impl.table.SmartBirtTable;
import org.wcs.smart.er.hibernate.SurveyHibernateManager;
import org.wcs.smart.er.model.SamplingUnit;
import org.wcs.smart.er.model.SamplingUnit.SamplingUnitType;
import org.wcs.smart.er.model.MissionTrack;
import org.wcs.smart.er.model.SamplingUnitAttribute;
import org.wcs.smart.er.model.SamplingUnitAttributeValue;
import org.wcs.smart.er.model.SurveyDesign;
import org.wcs.smart.er.model.SurveyDesignSamplingUnitAttribute;
import org.wcs.smart.er.query.report.internal.Messages;

/**
 * Sampling unit table.
 * 
 * @author Emily
 *
 */
public class SurveySamplingUnitTable extends SmartBirtTable {

	public static final String SU_PREFIX = "SD_SU"; //$NON-NLS-1$
	private SurveyDesign sd;
	private SamplingUnit.SamplingUnitType type;
	
	public SurveySamplingUnitTable(SurveyDesign sd, SamplingUnit.SamplingUnitType type) {
		super(MessageFormat.format(Messages.SurveySamplingUnitTable_LongDisplayName, new Object[]{sd.getName(), type.getGuiName()}),
				sd.getName() + " [" + type.getGuiName() + "]", //$NON-NLS-1$ //$NON-NLS-2$
				SU_PREFIX + ":" + type.name() + ":" + sd.getKeyId()); //$NON-NLS-1$ //$NON-NLS-2$
		this.type = type;
		this.sd = sd;
		this.tableImage = type.getImage();
	}

	@Override
	public String[] getColumnNames() {
		String[] names = null;
		int i= 0;
		if (type == SamplingUnitType.PLOT || 
				type == SamplingUnitType.TRANSECT){ 
			names = new String[sd.getSamplingUnitAttributes().size() + 3];
			names[i++] = "sd:su:id"; //$NON-NLS-1$
			names[i++] = "sd:su:buffer"; //$NON-NLS-1$
			names[i++] = "sd:su:state"; //$NON-NLS-1$
			for (SurveyDesignSamplingUnitAttribute sua : sd.getSamplingUnitAttributes()){
				names[i++] = sua.getSamplingUnitAttribute().getKeyId();
			}
		}else if (type == SamplingUnitType.RECON){
			names = new String[4];
			names[i++] = "sd:su:id"; //$NON-NLS-1$
			names[i++] = "sd:su:missionid"; //$NON-NLS-1$
			names[i++] = "sd:su:surveyid"; //$NON-NLS-1$
			names[i++] = "sd:su:date"; //$NON-NLS-1$
		}
		return names;
	}

	@Override
	public String[] getColumnLabels() {
		String[] names = null;
		int i= 0;
		if (type == SamplingUnitType.PLOT || 
				type == SamplingUnitType.TRANSECT){ 
			names = new String[sd.getSamplingUnitAttributes().size() + 3];
		
			names[i++] = Messages.SurveySamplingUnitTable_IDColumnLabel;
			names[i++] = Messages.SurveySamplingUnitTable_BufferColumnLabel;
			names[i++] = Messages.SurveySamplingUnitTable_StateColumnLabel;
			for (SurveyDesignSamplingUnitAttribute sua : sd.getSamplingUnitAttributes()){
				names[i++] = sua.getSamplingUnitAttribute().getName();
			}
		}else if (type == SamplingUnitType.RECON){
			names = new String[4];
			names[i++] = "ID"; //$NON-NLS-1$
			names[i++] = "Mission ID"; //$NON-NLS-1$
			names[i++] = "Survey ID"; //$NON-NLS-1$
			names[i++] = "Date"; //$NON-NLS-1$
		}
		return names;
	}

	@Override
	public int[] getColumnTypes() {
		int[] names = null;
		int i= 0;
		if (type == SamplingUnitType.PLOT || 
				type == SamplingUnitType.TRANSECT){
			names = new int[sd.getSamplingUnitAttributes().size() + 3];
		
			names[i++] = java.sql.Types.VARCHAR;
			names[i++] = java.sql.Types.DOUBLE;
			names[i++] = java.sql.Types.VARCHAR;
			for (SurveyDesignSamplingUnitAttribute sua : sd.getSamplingUnitAttributes()){
				if (sua.getSamplingUnitAttribute().getType() == AttributeType.TEXT){
					names[i++] = java.sql.Types.VARCHAR;
				}else if (sua.getSamplingUnitAttribute().getType() == AttributeType.NUMERIC){
					names[i++] = java.sql.Types.DOUBLE;
				}
			}
		}else if (type == SamplingUnitType.RECON){
			names = new int[4];
			names[i++] = java.sql.Types.VARCHAR;
			names[i++] = java.sql.Types.VARCHAR;
			names[i++] = java.sql.Types.VARCHAR;
			names[i++] = java.sql.Types.DATE;
		}
		return names;
	}

	/**
	 * @see org.wcs.smart.data.oda.smart.impl.table.SmartBirtTable#getValues(org.wcs.smart.ca.ConservationArea)
	 */
	@Override
	public List<Object> getValues (Collection<ConservationArea> cas, Session session) {
		List<Object> sus = new ArrayList<Object>();
		for (Object su : SurveyHibernateManager.getInstance().getSamplingUnits(sd, session)){
			if (su instanceof SamplingUnit){
				SamplingUnit s = (SamplingUnit)su;
				if (s.getType() == type){
					sus.add(s);
				}
			}else if (su instanceof MissionTrack && 
					type == SamplingUnitType.RECON){
				sus.add(su);
			}
		}
		return sus;
		
	}

	/**
	 * @see org.wcs.smart.data.oda.smart.impl.table.SmartBirtTable#getValue(java.lang.Object, int)
	 */
	@Override
	public Object getValue(Object object, int index) {
		if (object instanceof SamplingUnit){
			SamplingUnit e = (SamplingUnit)object;
		
			if (index == 0){
				return e.getId();
			}else if (index == 1){
				return e.getBuffer();
			}else if (index == 2){
				return e.getState().getGuiName();
			}else{
				SamplingUnitAttribute sua = sd.getSamplingUnitAttributes().get(index - 3).getSamplingUnitAttribute();
				for (SamplingUnitAttributeValue suav : e.getAttributes()){
					if (suav.getSamplingUnitAttribute().equals(sua)){
						if (sua.getType() == AttributeType.TEXT){
							return suav.getStringValue();
						}else if (sua.getType() == AttributeType.NUMERIC){
							return suav.getNumberValue();
						}
					}
				}
			}
		}else if (object instanceof MissionTrack){
			MissionTrack mt = (MissionTrack)object;
			if (index == 0){
				return mt.getId();
			}else if (index == 1){
				return mt.getMission().getId();
			}else if (index == 2){
				return mt.getMission().getSurvey().getId();
			}else if (index == 3){
				return mt.getDate();
			}
		}
		return null;
	}

	/**
	 * @see org.wcs.smart.data.oda.smart.impl.table.SmartBirtTable#openQuery()
	 */
	@Override
	public void openQuery() {		
	}

	/**
	 * @see org.wcs.smart.data.oda.smart.impl.table.SmartBirtTable#closeQuery()
	 */
	@Override
	public void closeQuery() {
	}

}
