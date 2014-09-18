package org.wcs.smart.tests.ca.datamodel;

import java.util.ArrayList;

import junit.framework.Assert;

import org.junit.Test;
import org.wcs.smart.ca.NamedKeyItem;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.ca.datamodel.DmObject;

public class KeyGeneratorTest  {
	
	
	@Test
	public void testSimpleKey() {
		Assert.assertEquals("thisisasimpletest",NamedKeyItem.generateKey("This is a simple tesT.", null));
		Assert.assertEquals("really45_ok",NamedKeyItem.generateKey("really 45 -()*&^%%$#@!_ok.", null));
		Assert.assertEquals("object", NamedKeyItem.generateKey("<>.,/;'[]\\+.", null));
	}
	
	@Test
	public void testExistsKey(){
		
		ArrayList<DmObject> objects = generateDmObject();
		
		Assert.assertEquals("apple1",NamedKeyItem.generateKey("apple", objects));
		Assert.assertEquals("apple1",NamedKeyItem.generateKey("APPLE", objects));
		Assert.assertEquals("apple1",NamedKeyItem.generateKey("AppLE", objects));
		
		Assert.assertEquals("carrot2",NamedKeyItem.generateKey("carrot", objects));
		Assert.assertEquals("carrot2",NamedKeyItem.generateKey("CARROT", objects));
		Assert.assertEquals("carrot2",NamedKeyItem.generateKey("caRRot", objects));
		
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
		Assert.assertNull(NamedKeyItem.validateKey("abc123def", null));
		Assert.assertNotNull(NamedKeyItem.validateKey("Abcder324", null));
		Assert.assertNotNull(NamedKeyItem.validateKey("abc_Def", null));
		Assert.assertNotNull(NamedKeyItem.validateKey("$", null));
		Assert.assertNotNull(NamedKeyItem.validateKey("", null));
		Assert.assertNotNull(NamedKeyItem.validateKey(null, null));
		
		ArrayList<DmObject> objects = generateDmObject();
		Assert.assertNotNull(NamedKeyItem.validateKey("apple", objects));
		Assert.assertNotNull(NamedKeyItem.validateKey("carrot", objects));
		Assert.assertNotNull(NamedKeyItem.validateKey("carrot1", objects));
		Assert.assertNull(NamedKeyItem.validateKey("carrot2", objects));
		Assert.assertNotNull(NamedKeyItem.validateKey("carroT2", objects));
		
		Assert.assertNotNull(NamedKeyItem.validateKey("1carroT2", objects));
		Assert.assertNotNull(NamedKeyItem.validateKey(".", objects));
		Assert.assertNotNull(NamedKeyItem.validateKey("this.that", objects));
		Assert.assertNotNull(NamedKeyItem.validateKey("123", objects));
		Assert.assertNotNull(NamedKeyItem.validateKey("i am smart.", objects));
		Assert.assertNotNull(NamedKeyItem.validateKey("1carroT2.", objects));
		
		
		
	}
}
