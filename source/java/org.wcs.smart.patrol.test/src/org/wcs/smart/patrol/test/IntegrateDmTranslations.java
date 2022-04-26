package org.wcs.smart.patrol.test;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Locale;

import org.wcs.smart.internal.ca.datamodel.xml.XmlSmartDataModelManager;
import org.wcs.smart.internal.ca.datamodel.xml.generate.v10.AttributeType;
import org.wcs.smart.internal.ca.datamodel.xml.generate.v10.CategoryType;
import org.wcs.smart.internal.ca.datamodel.xml.generate.v10.ListNode;
import org.wcs.smart.internal.ca.datamodel.xml.generate.v10.NameType;
import org.wcs.smart.internal.ca.datamodel.xml.generate.v10.TreeNodeType;

public class IntegrateDmTranslations {
	private static void doSomething() throws Exception{
		org.wcs.smart.internal.ca.datamodel.xml.generate.v10.DataModel defaultDm = XmlSmartDataModelManager.readDataModel(Files.newInputStream(Paths.get("C:\\data\\SMART\\Source\\trunk\\source\\java\\org.wcs.smart\\properties\\datamodel.xml")), Locale.getDefault());
		org.wcs.smart.internal.ca.datamodel.xml.generate.v10.DataModel mergeDm = XmlSmartDataModelManager.readDataModel(Files.newInputStream(Paths.get("C:\\temp\\ka\\smart.ka.xml")), Locale.getDefault());
		
		
		for (AttributeType da : defaultDm.getAttributes().getAttributes()){
			for (AttributeType ma : mergeDm.getAttributes().getAttributes()){
				if (da.getKey().equals(ma.getKey())){
					for (NameType nt : ma.getNames()){
						if (nt.getLanguageCode().equals("ka")){
							da.getNames().add(nt);
							break;
						}
					}
				
					if (da.getType().equals("LIST") && ma.getType().equals("LIST")){
						for (ListNode node : da.getValues()){
							for (ListNode mnode : ma.getValues()){
								if (node.getKey().equals(mnode.getKey())){
									for (NameType nt : mnode.getNames()){
										if (nt.getLanguageCode().equals("ka")){
											node.getNames().add(nt);
										}
									}
									break;
								}
							}
						}
					}
					if (da.getType().equals("TREE") && ma.getType().equals("TREE")){
						for (TreeNodeType node : da.getTrees()){
							for (TreeNodeType mnode : ma.getTrees()){
								if (node.getKey().equals(mnode.getKey())){
									processTreeNode(node, mnode);
									break;
								}
							}
						}
					}
					break;
				}
			}
			
			
		}
		
		for (CategoryType defaultKid : defaultDm.getCategories().getCategories()){
			for (CategoryType kaKid : mergeDm.getCategories().getCategories()){
				if (defaultKid.getKey().equals(kaKid.getKey())){
					processCategory(defaultKid, kaKid);
					break;
				}
			}
		}
		 XmlSmartDataModelManager.writeDataModel(defaultDm, Files.newOutputStream(Paths.get("C:\\temp\\ka\\datamodel.2.xml")));

		System.out.println("done");
		
	}
	
	private static void processCategory(CategoryType defaultCa, CategoryType kaCa){
		if (!defaultCa.getKey().equals(kaCa.getKey())) return;
		
		for (NameType nt : kaCa.getNames()){
			if (nt.getLanguageCode().equals("ka")){
				defaultCa.getNames().add(nt);
				break;
			}
		}
		
		if (defaultCa.getCategories() != null && !defaultCa.getCategories().isEmpty() && kaCa.getCategories() != null){
			for (CategoryType defaultKid : defaultCa.getCategories()){
				for (CategoryType kaKid : kaCa.getCategories()){
					if (defaultKid.getKey().equals(kaKid.getKey())){
						processCategory(defaultKid, kaKid);
						break;
					}
				}
			}
		}
	}
	
	private static void processTreeNode(TreeNodeType defaultNode, TreeNodeType kaNode){
		if (!defaultNode.getKey().equals(kaNode.getKey())) return;
		
		for (NameType nt : kaNode.getNames()){
			if (nt.getLanguageCode().equals("ka")){
				defaultNode.getNames().add(nt);
			}
		}
		
		if (defaultNode.getChildrens() != null && !defaultNode.getChildrens().isEmpty() && kaNode.getChildrens() != null){
			for (TreeNodeType defaultKid : defaultNode.getChildrens()){
				for (TreeNodeType kaKid : kaNode.getChildrens()){
					if (defaultKid.getKey().equals(kaKid.getKey())){
						processTreeNode(defaultKid, kaKid);
						break;
					}
				}
			}
		}
	}

}
