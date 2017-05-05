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
package org.wcs.smart.entity;

import org.wcs.smart.ISharedLabelProvider;

/**
 * Label provider for entity items.  Must provide
 * values for Status items and EntityType.Type items and
 * the various constants in this file.
 * 
 * @author Emily
 *
 */
public interface IEntityLabelProvider extends ISharedLabelProvider {

	public static final String ENTITY_TYPE_REPORT_KEY = "entitytypereporttable"; //$NON-NLS-1$
	
	public static final String ID_FIELD_KEY = "idfieldnamekey"; //$NON-NLS-1$
	public static final String STATUS_FIELD_KEY = "statusfieldnamekey"; //$NON-NLS-1$
	public static final String X_FIELD_KEY = "xfieldnamekey"; //$NON-NLS-1$
	public static final String Y_FIELD_KEY = "yfieldnamekey"; //$NON-NLS-1$
	public static final String CA_FIELD_KEY = "cafieldnamekey"; //$NON-NLS-1$
	
	public static final String MERGE_PROGRESS1_KEY = "mergeprgress1key"; //$NON-NLS-1$
	public static final String MERGE_PROGRESS2_KEY = "mergeprgress2key"; //$NON-NLS-1$
}
