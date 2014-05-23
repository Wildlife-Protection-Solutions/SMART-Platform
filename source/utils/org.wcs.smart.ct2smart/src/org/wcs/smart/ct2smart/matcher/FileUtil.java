package org.wcs.smart.ct2smart.matcher;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import org.wcs.smart.ct2smart.matcher.model.Ct2Smart;

public class FileUtil {

	public static void write(File file, Ct2Smart ct2Smart) throws JAXBException, IOException {
		BufferedOutputStream outXml = new BufferedOutputStream(new FileOutputStream(file));
		try {
			write(ct2Smart, outXml, Ct2Smart.class);
		} finally {
			outXml.close();
		}
	}
	
	public static void write(Object obj, OutputStream file, Class<?> clazz) throws JAXBException, IOException {
		JAXBContext context = JAXBContext.newInstance(obj.getClass());
		Marshaller marshaller = context.createMarshaller();
		marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
		marshaller.marshal(obj, file);
	}
	
}
