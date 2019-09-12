package org.wcs.smart.tests.ca.datamodel;

import java.util.ArrayList;
import java.util.Locale;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.wcs.smart.ICoreLabelProvider;
import org.wcs.smart.SmartContext;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.ca.datamodel.DataModelManager;
import org.wcs.smart.ca.datamodel.DmObject;

public class KeyGeneratorTest  {
	
	@BeforeClass
	public static void startup() throws Exception{
		SmartContext.INSTANCE.setClass(ICoreLabelProvider.class, new ICoreLabelProvider() {
			@Override
			public String getLabel(Object item, Locale l) {
				return "";
			}
			
			@Override
			public String getEmployeeShortLabel(Employee e, Locale l) {
				return "";
			}
		});
	}
	
	@Test
	public void testSimpleKey() {
		Assert.assertEquals("thisisasimpletest",DataModelManager.INSTANCE.generateKey("This is a simple tesT.", null));
		Assert.assertEquals("really45_ok",DataModelManager.INSTANCE.generateKey("really 45 -()*&^%%$#@!_ok.", null));
		Assert.assertEquals("object", DataModelManager.INSTANCE.generateKey("<>.,/;'[]\\+.", null));
	}
	
	@Test
	public void testExistsKey(){
		
		ArrayList<DmObject> objects = generateDmObject();
		
		Assert.assertEquals("apple1",DataModelManager.INSTANCE.generateKey("apple", objects));
		Assert.assertEquals("apple1",DataModelManager.INSTANCE.generateKey("APPLE", objects));
		Assert.assertEquals("apple1",DataModelManager.INSTANCE.generateKey("AppLE", objects));
		
		Assert.assertEquals("carrot2",DataModelManager.INSTANCE.generateKey("carrot", objects));
		Assert.assertEquals("carrot2",DataModelManager.INSTANCE.generateKey("CARROT", objects));
		Assert.assertEquals("carrot2",DataModelManager.INSTANCE.generateKey("caRRot", objects));
		
	}

	private ArrayList<DmObject> generateDmObject() {
		ArrayList<DmObject> objects = new ArrayList<DmObject>();
		
		DmObject dm = new Category();
		dm.setKeyId("apple");
		objects.add(dm);
		
		dm = new Category();
		dm.setKeyId("carrot");
		objects.add(dm);
		
		dm = new Category();
		dm.setKeyId("carrot1");
		objects.add(dm);
		return objects;
	}
	
	@Test
	public void testKeyValidator(){
		Assert.assertNull(DataModelManager.INSTANCE.validateKey("abc123def", null));
		Assert.assertNotNull(DataModelManager.INSTANCE.validateKey("Abcder324", null));
		Assert.assertNotNull(DataModelManager.INSTANCE.validateKey("abc_Def", null));
		Assert.assertNotNull(DataModelManager.INSTANCE.validateKey("$", null));
		Assert.assertNotNull(DataModelManager.INSTANCE.validateKey("", null));
		Assert.assertNotNull(DataModelManager.INSTANCE.validateKey(null, null));
		
		ArrayList<DmObject> objects = generateDmObject();
		Assert.assertNotNull(DataModelManager.INSTANCE.validateKey("apple", objects));
		Assert.assertNotNull(DataModelManager.INSTANCE.validateKey("carrot", objects));
		Assert.assertNotNull(DataModelManager.INSTANCE.validateKey("carrot1", objects));
		Assert.assertNull(DataModelManager.INSTANCE.validateKey("carrot2", objects));
		Assert.assertNotNull(DataModelManager.INSTANCE.validateKey("carroT2", objects));
		
		Assert.assertNotNull(DataModelManager.INSTANCE.validateKey("1carroT2", objects));
		Assert.assertNotNull(DataModelManager.INSTANCE.validateKey(".", objects));
		Assert.assertNotNull(DataModelManager.INSTANCE.validateKey("this.that", objects));
		Assert.assertNotNull(DataModelManager.INSTANCE.validateKey("123", objects));
		Assert.assertNotNull(DataModelManager.INSTANCE.validateKey("i am smart.", objects));
		Assert.assertNotNull(DataModelManager.INSTANCE.validateKey("1carroT2.", objects));
		
		
		
	}
}
