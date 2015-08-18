/*
 * Copyright (C) 2015 Wildlife Conservation Society
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
package org.wcs.smart.connect.exceptions;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Exception mapper to map a SmartConnectException to an http response.
 * 
 * @author Emily
 *
 */
@Provider
public class SmartConnectExceptionMapper implements ExceptionMapper<SmartConnectException> {

	@Override
	public Response toResponse(SmartConnectException ex) {
		
		
		Response.ResponseBuilder builder = Response.status(ex.getResponseCode());
		builder.type(MediaType.APPLICATION_JSON);
		
		if (ex.getMessage() != null) {
			ObjectMapper mapper = new ObjectMapper();
			StringBuilder sb = new StringBuilder();
			sb.append("{ \"status\": " + ex.getResponseCode()); //$NON-NLS-1$

			sb.append(",\"error\": "); //$NON-NLS-1$
			try {
				sb.append(mapper.writeValueAsString(ex.getMessage()));
			} catch (JsonProcessingException e) {
				sb.append("unknown"); //$NON-NLS-1$
			}
			sb.append("}"); //$NON-NLS-1$

			builder.entity(sb.toString());
		}
		return builder.build();
	}

}
