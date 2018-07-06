package GenerateDm;

import java.io.File;
import java.io.PrintStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Pattern;

public class GenerateDm {

	public static void main(String[] args ) throws Exception{
		(new GenerateDm()).doIt();
	}
	
	public void doIt() throws Exception{
		PrintStream out = new PrintStream(new File("C:\\temp\\fish.xml"), "UTF-8");	
		
		Connection c = DriverManager.getConnection("jdbc:postgresql://192.168.50.131/connectemily", "smart", "smart");
	
		
		String sql = "SELECT distinct class from fish.fish";
		
		List<String> classes = new ArrayList<String>();
		
		try(ResultSet rs = c.createStatement().executeQuery(sql)){
			while(rs.next()){
				String cz = rs.getString(1);
				classes.add(cz);
			}
		}
		
		HashSet<String> classKeys = new HashSet<String>();
		for (String cz : classes){
			String classKey = generateKey(cz, classKeys);
			classKeys.add(classKey);
		
			
			out.println("<children key=\"" + classKey + "\" isactive=\"true\">");
			out.println("\t<names value=\"" + cz + "\" language_code=\"en\" />");
			
			
			//WRITE CLASS START
			sql = "SELECT distinct forder from fish.fish where class = '" + cz + "'";
			
			try(ResultSet rorder = c.createStatement().executeQuery(sql)){
				HashSet<String> orderKeys = new HashSet<String>();
				while(rorder.next()){
					//WRITE ORDER START
					String order = rorder.getString(1);
					String orderkey = generateKey(rorder.getString(1), orderKeys);
					orderKeys.add(orderkey);
					
					out.println("\t<children key=\"" + orderkey + "\" isactive=\"true\">");
					out.println("\t\t<names value=\"" + rorder.getString(1) + "\" language_code=\"en\" />");
					
					sql = "SELECT distinct famcode, famname, famnamecom FROM fish.fish where class = '" + cz + "' and forder = '" + order + "'";
					try(ResultSet rfamily = c.createStatement().executeQuery(sql)){
						//WRITE FAMILY START
						HashSet<String> familyKeys = new HashSet<String>();
						
						while(rfamily.next()){
							//WRITE GENUS START
							String fkey = generateKey(rfamily.getString(2), familyKeys);
							familyKeys.add(fkey);
							
							out.println("\t\t<children key=\"" + fkey + "\" isactive=\"true\">");
							String name = rfamily.getString(2);
							String com = rfamily.getString(3);
							if (com != null && !com.trim().isEmpty()) name = name + " (" + com + ")";
							out.println("\t\t\t<names value=\"" + name + "\" language_code=\"en\" />");
							
							int familycode = rfamily.getInt(1);
							sql = "SELECT distinct subfamily FROM fish.fish where famcode = " + familycode + "";
							
							try(ResultSet rsubfamily = c.createStatement().executeQuery(sql)){
								
								HashSet<String> subfamilyKeys = new HashSet<String>();
								
								while(rsubfamily.next()){
									//WRITE GENUS START
									String sub = rsubfamily.getString(1);
									if (sub == null) sub = "All " + name;
									String sfkey = generateKey(sub, subfamilyKeys);
									subfamilyKeys.add(sfkey);
									
									out.println("\t\t\t<children key=\"" + sfkey + "\" isactive=\"true\">");
									out.println("\t\t\t\t<names value=\"" + sub + "\" language_code=\"en\" />");
									
									//WRITE SUBFAMILY START
									String subfamily = rsubfamily.getString(1);
									if (subfamily == null){
										sql = "SELECT distinct genus FROM fish.fish where famcode = " + familycode + " and subfamily is null";
									}else{
										sql = "SELECT distinct genus FROM fish.fish where famcode = " + familycode + " and subfamily = '" + subfamily + "'";
									}
									try(ResultSet rgenus= c.createStatement().executeQuery(sql)){
		
										HashSet<String> genusKeys = new HashSet<String>();
										
										while(rgenus.next()){
											//WRITE GENUS START
											String gkey = generateKey(rgenus.getString(1), genusKeys);
											genusKeys.add(gkey);
											
											out.println("\t\t\t\t<children key=\"" + gkey + "\" isactive=\"true\">");
											out.println("\t\t\t\t\t<names value=\"" + rgenus.getString(1) + "\" language_code=\"en\" />");
											
											if (subfamily == null){
												sql = "SELECT distinct species, en_name, fr_name,es_name FROM fish.fish where famcode = " + familycode + " and subfamily is null and genus = '" + rgenus.getString(1) + "'";
											}else{
												sql = "SELECT distinct species, en_name, fr_name,es_name FROM fish.fish where famcode = " + familycode + " and subfamily = '" + subfamily + "'  and genus = '" + rgenus.getString(1) + "'";
											}
											try(ResultSet rspecies = c.createStatement().executeQuery(sql)){
												HashSet<String> speciesKey = new HashSet<String>();
												while(rspecies.next()){
													//WRITE SPECIES START
													String skey = generateKey(rspecies.getString(1), speciesKey);
													speciesKey.add(skey);
													String sname = rspecies.getString(1);
				
													out.println("\t\t\t\t\t<children key=\"" + skey + "\" isactive=\"true\">");
				
													String en = rspecies.getString(2);
													String ename = sname;
													if (en != null && en.trim().length() > 0){
														ename = sname + " (" + en + ")";	
													}
													out.println("\t\t\t\t\t\t<names value=\"" + ename + "\" language_code=\"en\" />");
													
													String es = rspecies.getString(3);
													if (es != null && es.trim().length() > 0){
														es = sname + " (" + es + ")";
														out.println("\t\t\t\t\t\t<names value=\"" + es + "\" language_code=\"es\" />");
													}
													
													String fr = rspecies.getString(4);
													if (fr != null && fr.trim().length() > 0){
														fr = sname + " (" + fr + ")";
														out.println("\t\t\t\t\t\t<names value=\"" + fr + "\" language_code=\"fr\" />");
													}
													out.println("\t\t\t\t\t</children>");
												}
												//WRITE SPECIES END
											}
											out.println("\t\t\t\t</children>");
											//WRITE  GENUS END		
										}
									}
									out.println("\t\t\t</children>");
									//WRITE SUBFAMILY END
								}
							}
							out.println("\t\t</children>");
							//WRITE FAMILY END
						}
					}
					out.println("\t</children>");
					//WRITE ORDER END
				}
				
			}
			out.println("</children>");
			//WRITE CLASS END
			
		}
		out.close();
	}
	
	
	public static final String INVALID_START_CHARS_KEY_PATTERN = "[^a-z]+"; //$NON-NLS-1$
	public static final String VALID_DM_KEY_PATTERN = "[a-z]{1}[a-z0-9_]*"; //$NON-NLS-1$
	/*
	 * These are keywords that cannot be used as keys; for querying purposes.
	 */
	public static final String[] KEYWORDS = new String[]{"and", "or", "not", "contains", "notcontains", "equals"}; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
	
	
	private String generateKey(String value, HashSet<String> usedKeys){
		String raw = value.toLowerCase().replaceAll("[^a-z0-9_]", ""); //$NON-NLS-1$ //$NON-NLS-2$
		//DM keys should not start with number or '_' character or queries will be invalid see ticket #354
		if (!raw.isEmpty() && Pattern.matches(INVALID_START_CHARS_KEY_PATTERN, raw.subSequence(0, 1))) {
			raw = raw.replaceFirst(INVALID_START_CHARS_KEY_PATTERN, ""); //$NON-NLS-1$
		}
		if (raw.isEmpty()){
			raw = "object"; //$NON-NLS-1$
		}
	
		int count = 0;
		String key = raw;
		if (raw.length() > 128){
			key = raw.substring(0, 128);
		}

		for (String keyword: KEYWORDS){
			if (keyword.equals(key)){
				key = key + "_"; //$NON-NLS-1$
				break;
			}
		}
		while(usedKeys.contains(key)){
			count ++;
			String cnt = String.valueOf(count);
			if (raw.length() + cnt.length() > 128){
				key = raw.substring(0, 128 - cnt.length() ) + cnt;
			}else{
				key = raw + String.valueOf(count);
			}
			
		}
		
		return key;
	}
}
