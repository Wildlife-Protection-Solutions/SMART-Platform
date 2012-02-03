package org.wcs.smart.tests.ca.datamodel;

import java.util.ArrayList;

import junit.framework.Assert;

import org.junit.Test;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.ca.datamodel.DataModel;
import org.wcs.smart.ca.datamodel.DmObject;

public class KeyGeneratorTest  {
	
	
	@Test
	public void testSimpleKey() {
		Assert.assertEquals("thisisasimpletest",DataModel.generateKey("This is a simple tesT.", null));
		Assert.assertEquals("really45_ok",DataModel.generateKey("really 45 -()*&^%%$#@!_ok.", null));
		Assert.assertEquals("object", DataModel.generateKey("<>.,/;'[]\\+.", null));
	}
	
	@Test
	public void testExistsKey(){
		
		ArrayList<DmObject> objects = generateDmObject();
		
		Assert.assertEquals("apple1",DataModel.generateKey("apple", objects));
		Assert.assertEquals("apple1",DataModel.generateKey("APPLE", objects));
		Assert.assertEquals("apple1",DataModel.generateKey("AppLE", objects));
		
		Assert.assertEquals("carrot2",DataModel.generateKey("carrot", objects));
		Assert.assertEquals("carrot2",DataModel.generateKey("CARROT", objects));
		Assert.assertEquals("carrot2",DataModel.generateKey("caRRot", objects));
		
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
		Assert.assertNull(DataModel.validateKey("abc123def", null));
		Assert.assertNotNull(DataModel.validateKey("Abcder324", null));
		Assert.assertNotNull(DataModel.validateKey("abc_Def", null));
		Assert.assertNotNull(DataModel.validateKey("$", null));
		Assert.assertNotNull(DataModel.validateKey("", null));
		Assert.assertNotNull(DataModel.validateKey(null, null));
		
		ArrayList<DmObject> objects = generateDmObject();
		Assert.assertNotNull(DataModel.validateKey("apple", objects));
		Assert.assertNotNull(DataModel.validateKey("carrot", objects));
		Assert.assertNotNull(DataModel.validateKey("carrot1", objects));
		Assert.assertNull(DataModel.validateKey("carrot2", objects));
		Assert.assertNotNull(DataModel.validateKey("carroT2", objects));
		
		
		
	}
}
