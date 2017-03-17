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
package org.wcs.smart.report.birt.map.properties;

import java.util.List;

import org.eclipse.birt.report.designer.internal.ui.views.attributes.provider.PropertyDescriptorProvider;
import org.eclipse.birt.report.model.api.DesignEngine;
import org.eclipse.birt.report.model.api.ExtendedItemHandle;
import org.eclipse.birt.report.model.api.activity.SemanticException;
import org.eclipse.birt.report.model.api.extension.ExtendedElementException;
import org.eclipse.birt.report.model.api.metadata.IElementPropertyDefn;
import org.wcs.smart.report.birt.map.execute.SmartMapPresentationImpl;
import org.wcs.smart.report.birt.map.item.SmartMapItem;

/**
 * Property provider for the map DPI property.
 * 
 */
public class DpiPropertyProvider extends PropertyDescriptorProvider {

	
	protected SmartMapItem mapItem;

	public DpiPropertyProvider(String property, String element) {
		super(property, element);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.birt.report.designer.internal.ui.views.attributes.provider
	 * .IDescriptorProvider#save(java.lang.Object)
	 */
	@Override
	public void save(Object value) throws SemanticException {

		Integer dpi = SmartMapPresentationImpl.DEFAULT_DPI;
		if (value instanceof Integer){
			dpi = (Integer) value;
		}else if (value instanceof String){
			try{
				dpi = Integer.parseInt((String)value);
			}catch (Exception ex){
				
			}
		}
		
		if (dpi < SmartMapPresentationImpl.MIN_DPI || dpi > SmartMapPresentationImpl.MAX_DPI){
			dpi = SmartMapPresentationImpl.DEFAULT_DPI;
		}
		
		mapItem.setDPI(dpi);
	}
	
	@Override
	public Object load( ){
		if (mapItem == null) initializeMap();
		return mapItem.getDPI();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.birt.report.designer.internal.ui.views.attributes.provider
	 * .IDescriptorProvider#setInput(java.lang.Object)
	 */
	@Override
	public void setInput(Object input) {
		super.setInput(input);
		initializeMap();
	}

	protected synchronized void initializeMap() {
		mapItem = null;
		if ((input == null)) {
			return;
		}

		if ((!(input instanceof List && ((List<?>) input).size() > 0 && ((List<?>) input)
				.get(0) instanceof ExtendedItemHandle))
				&& (!(input instanceof ExtendedItemHandle))) {
			return;
		}

		ExtendedItemHandle handle;
		if (((List<?>) input).size() > 0) {
			handle = (ExtendedItemHandle) (((List<?>) input).get(0));
		} else {
			handle = (ExtendedItemHandle) input;
		}

		try {
			mapItem = (SmartMapItem) handle.getReportItem();
			return;
		} catch (ExtendedElementException e) {
			e.printStackTrace();
			return;
		}
	}

	@Override
	public String getElement(){
		return super.getElement();
	}
	
	@Override
	public String getProperty(){
		return super.getProperty();
	}
	
	@Override
	public String getDisplayName( ) {		
		IElementPropertyDefn propertyDefn;
		propertyDefn = DesignEngine.getMetaDataDictionary( ) .getElement( element ).getProperty( property );
		if ( propertyDefn != null ) {
			return propertyDefn.getDisplayName() + ":"; //$NON-NLS-1$
		}
		return ""; //$NON-NLS-1$
	}

}
