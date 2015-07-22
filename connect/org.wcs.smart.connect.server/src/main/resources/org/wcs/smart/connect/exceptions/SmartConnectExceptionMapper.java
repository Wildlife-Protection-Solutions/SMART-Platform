package org.wcs.smart.connect.exceptions;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

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
