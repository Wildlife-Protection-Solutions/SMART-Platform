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
import java.util.Locale;

import org.hibernate.Session;
import org.wcs.smart.SmartContext;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.datamodel.Attribute.AttributeType;
import org.wcs.smart.data.oda.smart.impl.SmartConnection;
import org.wcs.smart.data.oda.smart.impl.table.SmartBirtTable;
import org.wcs.smart.er.hibernate.CaSurveyHibernateManager;
import org.wcs.smart.er.hibernate.CcaaSurveyHibernateManager;
import org.wcs.smart.er.hibernate.ISurveyHibernateManager;
import org.wcs.smart.er.model.IErLabelProvider;
import org.wcs.smart.er.model.MissionTrack;
import org.wcs.smart.er.model.SamplingUnit;
import org.wcs.smart.er.model.SamplingUnit.GeometryType;
import org.wcs.smart.er.model.SamplingUnitAttribute;
import org.wcs.smart.er.model.SamplingUnitAttributeValue;
import org.wcs.smart.er.model.SurveyDesign;
import org.wcs.smart.er.model.SurveyDesignSamplingUnitAttribute;


/**
 * Sampling unit table.
 * 
 * @author Emily
 *
 */
public class SurveySamplingUnitTable extends SmartBirtTable {

	public static final String SU_PREFIX = "SD_SU"; //$NON-NLS-1$
	private SurveyDesign sd;
	private SamplingUnit.GeometryType type;
	
	
	public SurveySamplingUnitTable(SurveyDesign sd, SamplingUnit.GeometryType type) {
		super(SU_PREFIX + ":" + type.name() + ":" + sd.getKeyId()); //$NON-NLS-1$ //$NON-NLS-2$
		this.type = type;
		this.sd = sd;	
	}

	public SamplingUnit.GeometryType getType(){
		return this.type;
	}
	
	
	@Override
	public String[] getColumnNames(SmartConnection connection) {
		String[] names = null;
		int i= 0;
		if (type == GeometryType.PLOT || 
				type == GeometryType.TRANSECT){ 
			int length = sd.getSamplingUnitAttributes().size() + 2;
			if (type == GeometryType.TRANSECT){
				length ++;
			}
			names = new String[length];
			names[i++] = "sd:su:id"; //$NON-NLS-1$
			names[i++] = "sd:su:state"; //$NON-NLS-1$
			if (type == GeometryType.TRANSECT){
				names[i++] = "sd:su:length"; //$NON-NLS-1$
			}
			for (SurveyDesignSamplingUnitAttribute sua : sd.getSamplingUnitAttributes()){
				names[i++] = sua.getSamplingUnitAttribute().getKeyId();
			}
		}
		return names;
	}

	@Override
	public String[] getColumnLabels(SmartConnection connection) {
		String[] names = null;
		int i= 0;
		if (type == GeometryType.PLOT || 
				type == GeometryType.TRANSECT){ 
			int length = sd.getSamplingUnitAttributes().size() + 2;
			if (type == GeometryType.TRANSECT){
				length ++;
			}
			names = new String[length];
			
			IErLabelProvider lblprovider = SmartContext.INSTANCE.getClass(IErLabelProvider.class);
			
			names[i++] = lblprovider.getLabel(IErLabelProvider.ID_COLUMN_KEY, connection.getCurrentLocale());
			names[i++] = lblprovider.getLabel(IErLabelProvider.STATE_COLUMN_KEY, connection.getCurrentLocale());
			if (type == GeometryType.TRANSECT){
				names[i++] = lblprovider.getLabel(IErLabelProvider.LENGTH_COLUMN_KEY, connection.getCurrentLocale());	
			}
			for (SurveyDesignSamplingUnitAttribute sua : sd.getSamplingUnitAttributes()){
				names[i++] = sua.getSamplingUnitAttribute().getName();
			}
		}
		return names;
	}

	@Override
	public int[] getColumnTypes(SmartConnection connection) {
		int[] names = null;
		int i= 0;
		if (type == GeometryType.PLOT || 
				type == GeometryType.TRANSECT){
			int length = sd.getSamplingUnitAttributes().size() + 2;
			if (type == GeometryType.TRANSECT){
				length ++;
			}
			names = new int[length];
		
			names[i++] = java.sql.Types.VARCHAR;
			names[i++] = java.sql.Types.VARCHAR;
			if (type == GeometryType.TRANSECT){
				names[i++] = java.sql.Types.DOUBLE;
			}
			for (SurveyDesignSamplingUnitAttribute sua : sd.getSamplingUnitAttributes()){
				if (sua.getSamplingUnitAttribute().getType() == AttributeType.TEXT){
					names[i++] = java.sql.Types.VARCHAR;
				}else if (sua.getSamplingUnitAttribute().getType() == AttributeType.NUMERIC){
					names[i++] = java.sql.Types.DOUBLE;
				}
			}
		}
		return names;
	}

	/**
	 * @see org.wcs.smart.data.oda.smart.impl.table.SmartBirtTable#getValues(org.wcs.smart.ca.ConservationArea)
	 */
	@Override
	public List<Object> getValues (SmartConnection connection) {
		List<Object> sus = new ArrayList<Object>();
		
		ISurveyHibernateManager mgr = null;
		if (connection.getConservationAreas().size() == 1){
			mgr = new CaSurveyHibernateManager(connection.getConservationAreas().iterator().next());
		}else{
			mgr = new CcaaSurveyHibernateManager();
		}
		for (Object su : mgr.getSamplingUnits(sd, connection.getSession(), null)){
			if (su instanceof SamplingUnit){
				SamplingUnit s = (SamplingUnit)su;
				if (s.getType() == type){
					sus.add(s);
				}
			}
		}
		return sus;
		
	}

	/**
	 * @see org.wcs.smart.data.oda.smart.impl.table.SmartBirtTable#getValue(java.lang.Object, int)
	 */
	@Override
	public Object getValue(Object object, int index, SmartConnection connection) {
		if (object instanceof SamplingUnit){
			SamplingUnit e = (SamplingUnit)object;
		
			if (index == 0){
				return e.getId();
			}else if (index == 1){
				return e.getState().getGuiName(connection.getCurrentLocale());
			}else{
				if (type == GeometryType.TRANSECT){
					if (index == 2){
						try{
							return e.getGeometryLengthKm();
						}catch (Exception ex){
							throw new RuntimeException(ex.getMessage(),ex);
						}
					}
					index--;
				}
				SamplingUnitAttribute sua = sd.getSamplingUnitAttributes().get(index - 2).getSamplingUnitAttribute();
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
				return mt.getMissionDay().getMission().getId();
			}else if (index == 2){
				return mt.getMissionDay().getMission().getSurvey().getId();
			}else if (index == 3){
				return mt.getMissionDay().getDate();
			}else if (index == 4){
				try{
					return mt.getGeometryLengthKm();
				}catch (Exception ex){
					throw new RuntimeException(ex.getMessage(),ex);
				}
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

	@Override
	public String getTableFullName(Locale l) {
		return MessageFormat.format(SmartContext.INSTANCE.getClass(IErLabelProvider.class).getLabel(IErLabelProvider.SU_TABLE_LONGNAME_KEY, l),
				new Object[]{sd.getName(), type.getGuiName(l)});
	}

	@Override
	public String getTableShortName(Locale l) {
		return sd.getName() + " [" + type.getGuiName(l) + "]"; //$NON-NLS-1$ //$NON-NLS-2$
	}

}
