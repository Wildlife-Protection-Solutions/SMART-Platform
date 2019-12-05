/*
 * Copyright (C) 2019 Wildlife Conservation Society
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
package org.wcs.smart.paws.udig;

import java.io.Serializable;
import java.net.URL;
import java.util.Map;

import org.locationtech.udig.catalog.ui.UDIGConnectionFactory;

public class PawsConnectionFactory extends UDIGConnectionFactory {

	public PawsConnectionFactory() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public Map<String, Serializable> createConnectionParameters(Object context) {
		if (context instanceof PawsService){
			return ((PawsService)context).getConnectionParams();
		}
		
		if (context instanceof URL ){
			return PawsServiceExtension.createParamsFromUrl((URL)context);
		}
		
		return null;

	}

	@Override
	public URL createConnectionURL(Object context) {
		if (context instanceof URL){
			if (PawsServiceExtension.isValid((URL)context)){
				return (URL)context;
			}
			return null;
		}
		if (context instanceof Map){
			@SuppressWarnings("unchecked")
			Map<String, Serializable> params = (Map<String,Serializable>)context;
			return PawsServiceExtension.createURL(params);
		}
		return null;
	}

}
