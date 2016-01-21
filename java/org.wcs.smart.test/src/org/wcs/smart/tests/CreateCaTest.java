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
package org.wcs.smart.tests;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.hibernate.Session;
import org.junit.Assert;
import org.junit.Test;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.ca.Employee.SmartUserLevel;
import org.wcs.smart.ca.Language;
import org.wcs.smart.ca.datamodel.DataModel;
import org.wcs.smart.internal.ca.datamodel.xml.DataModelSmartToXmlConverter;
import org.wcs.smart.internal.ca.datamodel.xml.DataModelXmlToSmartConverter;
import org.wcs.smart.internal.ca.datamodel.xml.XmlSmartDataModelManager;

/**
 * 
 * @author Emily
 * @since 1.0.0
 */
public class CreateCaTest {

	@Test
	public void testCreateCA(){
		
		ConservationArea ca = new ConservationArea();
		
		Language lang = new Language();
		lang.setCa(ca);
		lang.setCode("en_CA");
		lang.setDefault(true);
		
		HashSet<Language> langs = new HashSet<Language>();
		langs.add(lang);
		
		ca.setLanguages(langs);
		
		ca.setId("Test1");
		ca.setName("Auto Generate Test 1");
		ca.setDescription("This is a description");
		ca.setDesignation("This is a designation");
		
		Employee e1 = new Employee();
		e1.setGivenName("Smart");
		e1.setFamilyName("Tester");
		e1.setGender('F');
		e1.setSmartUserId("smart");
		e1.setSmartPassword("smart");
		e1.setSmartUserLevel(SmartUserLevel.ADMIN);
		e1.setBirthDate(new Date());
		e1.setStartEmploymentDate(new Date());
		e1.setConservationArea(ca);
		ca.setEmployees(new ArrayList<Employee>());
		ca.getEmployees().add(e1);
		
		Session session = Hibernate.openSession();
		session.beginTransaction();
		try{
			session.save(ca);
			Hibernate.generateEmployeeId(e1, session);
			session.save(e1);
			session.getTransaction().commit();
		}catch (Exception ex){
			session.getTransaction().rollback();
			ex.printStackTrace();
		}finally{
			session.close();
		}
		
		DataModel dm = null;
		try{
			dm = testLoadDataModel(ca);
		}catch (Exception ex){
			ex.printStackTrace();
			Assert.fail("Failed to Load Data Model." + ex.getMessage());
		}
		
		if (dm == null){
			Assert.fail("Data model cannot be null");
		}
		
		//try saving data model
		session = Hibernate.openSession();
		session.beginTransaction();
		dm.save(session, new NullProgressMonitor());
		session.getTransaction().commit();
		session.close();
		
		try{
			testExportDataModel(dm, ca);
		}catch (Exception ex){
			ex.printStackTrace();
			Assert.fail("Failed to export data mode." + ex.getMessage());
		}
	}
	

	public DataModel testLoadDataModel(ConservationArea ca) throws Exception{
		DataModelXmlToSmartConverter converter = new DataModelXmlToSmartConverter();
		InputStream is = this.getClass().getClassLoader().getResourceAsStream("IUCN_Threats.xml");
		Assert.assertNotNull(is);
		DataModel dm = converter.convert(is, ca, false);
		return dm;
	}
	
	public void testExportDataModel(DataModel dm, ConservationArea ca) throws Exception{
		DataModelSmartToXmlConverter converter = new DataModelSmartToXmlConverter();
		org.wcs.smart.internal.ca.datamodel.xml.generate.DataModel xmldm = converter.convert(dm, new NullProgressMonitor());
		
		
		File f = File.createTempFile("smartdm", "xml");
		FileOutputStream stream = new FileOutputStream(f);
		XmlSmartDataModelManager.writeDataModel(xmldm, stream);
		if (!f.exists()){
			Assert.fail("Failed to write data model to file.");
		}
		
		
		//can we read it back in?
		DataModelXmlToSmartConverter converter2 = new DataModelXmlToSmartConverter();
		DataModel dm2 = converter2.convert(f, ca, false);
		Assert.assertNotNull(dm2);
		Assert.assertNotNull(dm2.getCategories());
		Assert.assertNotNull(dm2.getAttributes());
		Assert.assertEquals(dm.getCategories().size(),dm2.getCategories().size());
		Assert.assertEquals(dm.getAttributes().size(),dm2.getAttributes().size());
		
	}
}
