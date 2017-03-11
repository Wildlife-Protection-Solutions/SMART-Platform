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
package org.wcs.smart.i2.udig.query;

import org.locationtech.udig.catalog.IServiceInfo;
import org.wcs.smart.i2.internal.Messages;

/**
 * Smart service information.
 * @author Emily
 * @since 1.0.0
 */
public class QueryServiceInfo extends IServiceInfo{

	public QueryServiceInfo(QueryService service){
		this.description = Messages.QueryServiceInfo_Description;
		this.keywords = new String[]{Messages.QueryServiceInfo_keyword1, Messages.QueryServiceInfo_keyword2, Messages.QueryServiceInfo_keyword3, Messages.QueryServiceInfo_keyword4};
		this.title = Messages.QueryServiceInfo_Title;
	}
	
}
