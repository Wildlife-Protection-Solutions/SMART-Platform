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
import org.hibernate.type.BinaryType;
import org.hibernate.type.UUIDBinaryType;

public class SmartUtils {

	public static Locale getRequestLocale(HttpHeaders headers){
		if (headers.getAcceptableLanguages().size() != 0){
			return headers.getAcceptableLanguages().get(0);
		}else{
			//return default en locale
			return Locale.ENGLISH;
		}
		
	}
	
	public static Locale getRequestLocale(ServletRequest request){
		return request.getLocale();
	}
	
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
	
	private static final SimpleDateFormat DT_FORMAT = new SimpleDateFormat("yyyy-MM-dd H:m:s");
	private static final SimpleDateFormat D_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
	
	public static Date parseDate(String dateString) throws Exception{
		try{
			return DT_FORMAT.parse(dateString);
		}catch (Exception ex){
			
		}return D_FORMAT.parse(dateString);
	}
}
