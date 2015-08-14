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
package org.wcs.smart.connect;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Properties;
import java.util.UUID;

import javax.servlet.ServletRequest;
import javax.ws.rs.core.HttpHeaders;

import org.hibernate.Session;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.id.UUIDGenerationStrategy;
import org.hibernate.id.UUIDGenerator;
import org.hibernate.id.uuid.StandardRandomStrategy;
import org.hibernate.type.UUIDBinaryType;

/**
 * A collection of utility functions
 * 
 * @author Emily
 *
 */
public class SmartUtils {

	/**
	 * Finds the locale provided in the given headers.  Returns
	 * english locale if no locale found in headers.
	 * 
	 * @param headers
	 * @return
	 */
	public static Locale getRequestLocale(HttpHeaders headers){
		if (headers.getAcceptableLanguages().size() != 0){
			return headers.getAcceptableLanguages().get(0);
		}else{
			//return default en locale
			return Locale.ENGLISH;
		}
		
	}
	/**
	 * Finds the request locale.
	 * 
	 * @param request
	 * @return
	 */
	public static Locale getRequestLocale(ServletRequest request){
		return request.getLocale();
	}
	
	/**
	 * Generates a new uuid for the given object.
	 * 
	 * @param session
	 * @param obj
	 * @return
	 */
	public static UUID generateUUID(Session session, Object obj){
		UUIDGenerator uuidGenerator = UUIDGenerator
				.buildSessionFactoryUniqueIdentifierGenerator();
		Properties prop = new Properties();
		prop.put(UUIDGenerator.UUID_GEN_STRATEGY,
				StandardRandomStrategy.INSTANCE);
		prop.put(UUIDGenerator.UUID_GEN_STRATEGY_CLASS,
				UUIDGenerationStrategy.class.getName());
		uuidGenerator.configure(new UUIDBinaryType(), prop, null);
		return (UUID)uuidGenerator.generate((SessionImplementor) session, obj);
	}
	
	public static final SimpleDateFormat DT_FORMAT = new SimpleDateFormat("yyyy-MM-dd H:m:s");
	public static final SimpleDateFormat D_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
	/**
	 * Parses a date string.  Assumes the string is provided as either
	 * DT_FORMAT or D_FORMAT.
	 *  
	 * @param dateString
	 * @return
	 * @throws Exception
	 */
	public static Date parseDate(String dateString) throws Exception{
		try{
			return DT_FORMAT.parse(dateString);
		}catch (Exception ex){
		}
		return D_FORMAT.parse(dateString);
	}
}
