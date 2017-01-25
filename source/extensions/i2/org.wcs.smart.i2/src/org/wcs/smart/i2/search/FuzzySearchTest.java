package org.wcs.smart.i2.search;

import java.io.InputStream;
import java.io.Serializable;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import net.sf.ehcache.config.CacheConfiguration;

import org.apache.commons.codec.language.DoubleMetaphone;

public class FuzzySearchTest {
	private static final Pattern SPLIT_PATTERN = Pattern.compile("\\s+");

	private static final DoubleMetaphone DOUBLE_METAPHONE = new DoubleMetaphone();

	public static void main(String[] args) {
		
		String str = "Popularity";
		System.out.println(DOUBLE_METAPHONE.doubleMetaphone(str, true));
		System.out.println(DOUBLE_METAPHONE.doubleMetaphone(str, false));
		if (true) return;
		// // NGram ngram = new NGram(3);
		// // System.out.println(1 - ngram.distance("boy", "Boy"));
		// // System.out.println(1 - ngram.distance("boy", "boy"));
		// // System.out.println(1 - ngram.distance("boy", "boys"));
		// // System.out.println(1 - ngram.distance("boy", "cowboys"));
		DoubleMetaphone tester = new DoubleMetaphone();
		// // System.out.println(tester.doubleMetaphone("Boy", false));
		// // System.out.println(tester.doubleMetaphone("Boy", true));
		// // if (true) return;
		//
		// DoubleMetaphoneSearch map = new DoubleMetaphoneSearch();
		//
		// System.out.println("read words");
		//
		// // int cnt = 0;

		CacheManager manager = CacheManager.getInstance();
		Cache map = new Cache(new CacheConfiguration("fuzzysearchcache", 500)
				.overflowToDisk(true).eternal(true));
		
		Cache map2 = new Cache(new CacheConfiguration("fuzzysearchcache2", 500)
		.overflowToDisk(true).eternal(true));		

		manager.addCache(map);
		manager.addCache(map2);

		// Cache c = new Cache("fuzzysearchcache", 10000,
		// MemoryStoreEvictionPolicy.LFU, true, null, false, null, null, false)
		InputStream is = FuzzySearchTest.class.getClassLoader()
				.getResourceAsStream("org/wcs/smart/i2/search/words.txt");
		// Set<String> phones = new HashSet<>();
		// Map<String, List<Object[]>> map = new HashMap<>();
		long cnt = 0;
		try {
			Scanner s = new Scanner(is).useDelimiter("\\n");
			Scanner s2 = new Scanner(is).useDelimiter("\\n");
			Scanner s3 = new Scanner(is).useDelimiter("\\n");
			Pattern p = Pattern.compile("\\s");

			while (s.hasNext()) {

				String text1 = s.next().trim();
				String text2 = s2.next().trim();
				String text3 = s3.next().trim();
				String text = text1 + " " + text2 + " " + text3;
				for (int i = 0; i < 3; i++) {

					String[] words = SPLIT_PATTERN.split(text);
					String[][] metas = new String[words.length][3];
					int k = 0;
					for (String w : words) {
						String word = w;
						String m1 = DOUBLE_METAPHONE.doubleMetaphone(w, true);
						String m2 = DOUBLE_METAPHONE.doubleMetaphone(w, false);

						Element element = map.get(m1);
						Set<String> lwords = null;
						if (element != null){
							lwords = (Set<String>) element.getValue();
						}
						if (lwords== null){
							lwords = new HashSet<String>();
							map.put(new Element(m1, lwords));
						}
						lwords.add(word);
						if (!m1.equals(m2)){
							element = map.get(m2);
							lwords = null;
							if (element != null){
								lwords = (Set<String>) element.getValue();
							}
							if (lwords== null){
								lwords = new HashSet<String>();
								map.put(new Element(m2, lwords));
							}
							lwords.add(word);
						}
						
						element = map2.get(word);
						List<Object[]> entities = null;
						if (element != null){
							entities = (List<Object[]>) map2.get(word).getValue();
						}
						if (entities == null){
							entities = new ArrayList<Object[]>();
							map2.put(new Element(word, entities));
						}
						entities.add(new Object[]{UUID.randomUUID(), text});
						if (entities.size() > 100){
							System.out.println("larget entity list");
						}
						
						k++;
					}

					cnt++;
				}
				if (cnt % 1000 == 0) {
					System.out.println(cnt);
					System.gc();
					System.out
							.println("allocated memory: "
									+ NumberFormat
											.getInstance()
											.format((Runtime.getRuntime()
													.totalMemory() - Runtime
													.getRuntime().freeMemory()) / 1000000));
				}
			}

			// }
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		System.gc();
		System.out.println("allocated memory: "
				+ NumberFormat.getInstance().format(
						(Runtime.getRuntime().totalMemory() - Runtime
								.getRuntime().freeMemory()) / 1000000));
		System.out.println(map.getKeys().size());
		System.out.println(map.getDiskStoreSize());
		System.out.println(map.getMemoryStoreSize());
		// System.out.println(phones.size() + ":" + cnt);
		// System.out.println(n + ":" + tester.doubleMetaphone(n, true) + ":" +
		// tester.doubleMetaphone(n, false));
		// // UUID uuid= UUID.randomUUID();
		// // map.addEntity(uuid, n);
		// //// for (int i = 0; i < 10; i ++){
		// //// map.addEntity(uuid, n + "es" + i);
		// //// map.addEntity(uuid, n + "ed"+ i);
		// //// map.addEntity(uuid, "un" + n+ i);
		// //// }
		// //
		// // uuid= UUID.randomUUID();
		// // map.addEntity(uuid, n);
		// // for (int i = 0; i < 2; i ++){
		// // map.addEntity(uuid, n + "es" + i);
		// //// map.addEntity(uuid, n + "ed"+ i);
		// //// map.addEntity(uuid, "un" + n+ i);
		// // }
		// //
		// //// cnt ++;
		// //// if (cnt % 10000 == 0){
		// //// cnt = 0;
		// //// System.out.println(n);
		// //// System.gc();
		// //// System.out.println("allocated memory: " +
		// NumberFormat.getInstance().format((Runtime.getRuntime().totalMemory()
		// - Runtime.getRuntime().freeMemory()) / 1000000));
		// //// }
		// }
		// }finally{
		// try {
		// is.close();
		// } catch (IOException e) {
		// // TODO Auto-generated catch block
		// e.printStackTrace();
		// }
		// }
		// // System.out.println("build map done");
		// //
		// // searchString("Boy", map);
		// // searchString("tral", map);
		// // searchString("Emily", map);
		// //
		// //
		// // System.out.println("allocated memory: " +
		// NumberFormat.getInstance().format((Runtime.getRuntime().totalMemory()
		// - Runtime.getRuntime().freeMemory()) / 1000000));
		// }
		//
		// public static void searchString(String search, DoubleMetaphoneSearch
		// map){
		// List<IntelSearchResult> results = map.search(search);
		//
		// System.out.println("Searched String:" + search);
		// System.out.println("Size:" + results.size());
		// int i = 0;
		// for (IntelSearchResult r : results){
		// System.out.println(i + ": " + r.result + ": " + r.matchRate);
		// i++;
		// if (i > 1000) break;
		// }
	}

	private static class SearchItem implements Serializable {
		public UUID uuid;
		public String text;

		public String type;
		public String[][] metas;

		public SearchItem(UUID uuid, String text, String type) {
			this.uuid = uuid;
			this.text = text;
			this.type = type;

			// todo make words unique??
			// String[] words = SPLIT_PATTERN.split(text);
			// metas = new String[words.length][3];
			// int i = 0;
			// for (String w : words){
			// metas[i][0] = w;
			// metas[i][1] = DOUBLE_METAPHONE.doubleMetaphone(w, true);
			// metas[i][2] = DOUBLE_METAPHONE.doubleMetaphone(w, false);
			// i++;
			// }
		}
	}
}
