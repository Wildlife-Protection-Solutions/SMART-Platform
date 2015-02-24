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
package org.wcs.smart.er.query.map.udig;

import org.locationtech.udig.catalog.IServiceInfo;
import org.wcs.smart.er.query.internal.Messages;
import org.wcs.smart.query.QueryPlugIn;

/**
 * Smart service information.
 * @author Emily
 * @since 1.0.0
 */
public class QueryServiceInfo extends IServiceInfo{

	public QueryServiceInfo(QueryService service){
		this.description = Messages.QueryServiceInfo_Description;
		this.icon = QueryPlugIn.getDefault().getImageRegistry().getDescriptor(QueryPlugIn.QUERY_ICON);
		this.keywords = new String[]{Messages.QueryServiceInfo_Keyword1, Messages.QueryServiceInfo_Keyword2, Messages.QueryServiceInfo_Keyword3, Messages.QueryServiceInfo_Keyword4};
		this.title = Messages.QueryServiceInfo_Title + service.getQuery().getName();
	}
	
}
