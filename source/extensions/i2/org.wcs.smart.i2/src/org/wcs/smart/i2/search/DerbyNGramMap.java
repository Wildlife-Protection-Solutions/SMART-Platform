package org.wcs.smart.i2.search;

import info.debatty.java.stringsimilarity.Levenshtein;
import info.debatty.java.stringsimilarity.NGram;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.codec.language.DoubleMetaphone;
import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.wcs.smart.util.UuidUtils;

public class DerbyNGramMap {

	// public HashMap<String, List<Entry>> map;

	public static final String MapTable = "smart.i_search_map";

	int gramSize = 3;
	private NGram ngram;// = new NGram(gramSize);

	public DerbyNGramMap(Session session) {
		ngram = new NGram(gramSize);
		createDbMap(session);
	}

	public void createDbMap(Session session) {
		try {
			session.createSQLQuery("DROP TABLE " + MapTable).executeUpdate();
		} catch (Exception ex) {

		}
		session.createSQLQuery(
				"CREATE TABLE " + MapTable + " (attribute_uuid char(16) for bit data, entity_uuid char(16) for bit data, word varchar(32000), meta1 varchar(32000), meta2 varchar(32000))")
				.executeUpdate();
	}

	private Collection<String> createGrams(String string) {
		string = string.toLowerCase();
		Set<String> grams = new HashSet<>();
		for (int i = 0; i <= string.length() - gramSize; i++) {
			String gram = string.substring(i, i + gramSize);
			grams.add(gram);
		}

		grams.add("  " + string.charAt(0));
		grams.add(" " + string.substring(0, 1));
		grams.add(string.substring(string.length() - 2) + " ");

		return grams;
	}

	private DoubleMetaphone tester = new DoubleMetaphone();
	public List<IntelSearchResultItem> search(String string, Session session) {
//		Collection<String> grams = createGrams(string);

		Map<UUID, IntelSearchResultItem> results = new HashMap<>();
		SQLQuery q = session
				.createSQLQuery("SELECT distinct entity_uuid, word from "
						+ MapTable + " WHERE meta1 = :meta1 or meta1 = :meta2 or meta2 =:meta3 or meta2 = :meta4");
		q.setParameter("meta1", tester.doubleMetaphone(string, false));
		q.setParameter("meta2", tester.doubleMetaphone(string, true));
		q.setParameter("meta3", tester.doubleMetaphone(string, false));
		q.setParameter("meta4", tester.doubleMetaphone(string, true));
		
		Levenshtein distance = new Levenshtein();
		
		List<Object[]> qresults = q.list();
		for (Object[] items : qresults) {
			String fullword = (String) items[1];
			UUID entity = UuidUtils.byteToUUID((byte[]) items[0]);

//			double value = 1 - ngram.distance(string.toLowerCase(), fullword);
			double value = 1 - (distance.distance(string.toLowerCase(), fullword.toLowerCase()) / Math.max(string.length(), fullword.length()));
			IntelSearchResultItem existing = results.get(entity);
//			if (existing == null) {
//				results.put(entity, new IntelSearchResult(entity, fullword, value));
//			} else if (existing.matchRate < value) {
//				existing.matchRate = value;
//				existing.result = fullword;
//			}
		}

		
		for (String gram : createGrams(string)){
			
			q = session
					.createSQLQuery("SELECT distinct entity_uuid, word from "
							+ MapTable + " WHERE word like :gram");
			
			q.setParameter("gram", "%" + gram + "%");
			
			qresults = q.list();
			for (Object[] items : qresults) {
				String fullword = (String) items[1];
				UUID entity = UuidUtils.byteToUUID((byte[]) items[0]);

				double value = 1 - ngram.distance(string.toLowerCase(), fullword);
				IntelSearchResultItem existing = results.get(entity);
//				if (existing == null) {
//					results.put(entity, new IntelSearchResult(entity, fullword, value));
//				} else if (existing.matchRate < value) {
//					existing.matchRate = value;
//					existing.result = fullword;
//				}
			}
			
		}
		ArrayList<IntelSearchResultItem> sorted = new ArrayList<IntelSearchResultItem>(
				results.values());
//		sorted.sort((a, b) -> -1 * Double.compare(a.matchRate, b.matchRate));
		return sorted;
	}

	public boolean addEntity(UUID uuid, String string, Session session) {
//		if (string.length() < gramSize)
//			return false;

		
//		Collection<String> grams = createGrams(string);

		// Entry itemEntry = new Entry(uuid, string, grams);
//		grams.forEach(gram -> {
			SQLQuery q = session.createSQLQuery("INSERT INTO " + MapTable
					+ " values (?,?,?,?,?)");

			q.setParameter(0, UuidUtils.uuidToByte(uuid));
			q.setParameter(1, UuidUtils.uuidToByte(uuid));
			q.setParameter(2, string);
			q.setParameter(3, tester.doubleMetaphone(string, false));
			q.setParameter(4, tester.doubleMetaphone(string, true));
			
			q.executeUpdate();
			// List<Entry> e = map.get(gram);
			// if (e == null){
			// e = new ArrayList<Entry>();
			// map.put(gram, e);
			// }
			// e.add(itemEntry);
//		});

		return true;
	}

	// class Entry{
	// private UUID entityUuid;
	// private String fullString;
	// //private Collection<String> grams;
	//
	// public Entry(UUID entityUuid, String fullString, Collection<String>
	// grams){
	// this.entityUuid = entityUuid;
	// this.fullString = fullString;
	// //this.grams = grams;
	// }
	//
	// public UUID getEntityUuid(){
	// return this.entityUuid;
	// }
	//
	// public String getFullString(){
	// return this.fullString;
	// }
	// // public Collection<String> getGrams(){
	// // return this.grams;
	// //}
	// }

}
