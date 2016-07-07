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
package org.wcs.smart.report.birt.map;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.birt.report.model.api.DataSetHandle;
import org.eclipse.birt.report.model.api.ExtendedItemHandle;
import org.eclipse.birt.report.model.api.OdaDataSetHandle;
import org.eclipse.birt.report.model.api.ReportDesignHandle;
import org.eclipse.birt.report.model.api.elements.DesignChoiceConstants;
import org.eclipse.birt.report.model.api.metadata.DimensionValue;
import org.eclipse.birt.report.model.api.util.DimensionUtil;
import org.eclipse.ui.XMLMemento;
import org.locationtech.udig.project.internal.ProjectFactory;
import org.locationtech.udig.project.internal.StyleBlackboard;
import org.locationtech.udig.style.sld.SLDContent;
import org.wcs.smart.udig.style.StyleManager;

/**
 * Utilities to support the BIRT Smart map item generation. 
 * 
 * @author Emily
 *
 */
public class BirtMapUtils {
	
	
	public static StyleBlackboard parseStyleString(String styleString){
		//for backwards compatibility this can either be a blackboard
		//json string or a single SLD xmlMemento
		if (styleString.startsWith("[")){ //$NON-NLS-1$
			try{
				StyleBlackboard sb = StyleManager.INSTANCE.fromString(styleString);
				return sb;
			}catch (Exception ex){
				Logger.getLogger(BirtMapUtils.class.getName()).log(Level.WARNING, ex.getMessage(), ex);
				return null;
			}
		}else{
			//xml sld content string
			StyleBlackboard sb = ProjectFactory.eINSTANCE.createStyleBlackboard();
			sb.put(SLDContent.ID, mementoToStyle(styleString));
			return sb;
		}
	}
	
	/**
	 * Converts an xml style momento to a Style object. Assumes
	 * SLD style momento.
	 * 
	 * @param xmlMemento
	 * @return
	 */
	private static Object mementoToStyle(String xmlMemento) {
		try {
			XMLMemento memento = XMLMemento.createReadRoot(new StringReader(
					xmlMemento));
			SLDContent cnt = new SLDContent();
			return cnt.load(memento);
		} catch (Exception ex) {
			Logger.getLogger(BirtMapUtils.class.getName()).log(Level.WARNING, ex.getMessage(), ex);
			return null;
		}
	}
	
	/**
	 * @param element
	 * @return array of smart query dataset handles added to the report
	 */
	public static OdaDataSetHandle[] getDataSets(ExtendedItemHandle element) {
		ReportDesignHandle handle = (ReportDesignHandle) ((ExtendedItemHandle) element)
				.getRoot();
		List<?> datasets = handle.getDataSets().getContents();
		List<OdaDataSetHandle> sets = new ArrayList<OdaDataSetHandle>();
		for (Iterator<?> iterator = datasets.iterator(); iterator.hasNext();) {
			DataSetHandle dataset = (DataSetHandle) iterator.next();
			if (dataset instanceof OdaDataSetHandle) {
				OdaDataSetHandle h = (OdaDataSetHandle) dataset;
				sets.add(h);
			}
		}
		return sets.toArray(new OdaDataSetHandle[sets.size()]);
	}
	
	/**
	 * Finds a smart query with the given query text in the 
	 * provided report handle
	 * 
	 * @param handle report handle
	 * @param queryText query text (Smart query hex encoded uuid)
	 * @return
	 */
	public static OdaDataSetHandle findHandle(ReportDesignHandle handle,
			String dataSetName, String queryText) {
		List<?> datasets = handle.getDataSets().getContents();
		for (Iterator<?> iterator = datasets.iterator(); iterator.hasNext();) {
			DataSetHandle dataset = (DataSetHandle) iterator.next();
			if (dataset instanceof OdaDataSetHandle) {
				OdaDataSetHandle h = (OdaDataSetHandle) dataset;
				if (h.getName().equals(dataSetName)){
					return h;
				}
				
			}
		}
		for (Iterator<?> iterator = datasets.iterator(); iterator.hasNext();) {
			DataSetHandle dataset = (DataSetHandle) iterator.next();
			if (dataset instanceof OdaDataSetHandle) {
				OdaDataSetHandle h = (OdaDataSetHandle) dataset;
 				if (h.getQueryText().equals(queryText)){
					return h;
				}
				
			}
		}
		
		return null;

	}
	
	/**
	 * The width of the extended item handle in pixels
	 * @param handle
	 * @param dpi
	 * @return
	 */
	public static int getWidthInPx(ExtendedItemHandle handle, int dpi){
		return getDimensionInPx((DimensionValue) handle.getWidth().getValue(), dpi);
	}
	/**
	 * The height of the extendeditemhandle in pixels
	 * @param handle
	 * @param dpi
	 * @return
	 */
	public static int getHeightInPx(ExtendedItemHandle handle, int dpi){
		return getDimensionInPx((DimensionValue) handle.getHeight().getValue(), dpi);
	}
	/**
	 * Converts the dimensionvalue to pixels measure
	 * @param dv
	 * @param dpi
	 * @return
	 */
	private static int getDimensionInPx(DimensionValue dv, int dpi){
		if(dv == null) return 50;	//deafult map size to 50 px
		int value = 0;
		if (dv.getUnits().equals(DesignChoiceConstants.UNITS_PX)){
			value = (int)dv.getMeasure();
		}else{
			Double w1 = DimensionUtil.convertTo(dv.getMeasure(),
				dv.getUnits(), DesignChoiceConstants.UNITS_IN)
				.getMeasure();
			value = (int) (w1 * dpi);
		}
		return value;	
	}
	
}
