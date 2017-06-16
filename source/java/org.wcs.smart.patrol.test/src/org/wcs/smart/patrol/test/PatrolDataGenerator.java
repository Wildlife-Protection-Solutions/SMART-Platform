package org.wcs.smart.patrol.test;

import java.sql.Time;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.ca.Station;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.AttributeTreeNode;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.ca.datamodel.CategoryAttribute;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.observation.model.Waypoint;
import org.wcs.smart.observation.model.WaypointObservation;
import org.wcs.smart.observation.model.WaypointObservationAttribute;
import org.wcs.smart.patrol.PatrolHibernateManager;
import org.wcs.smart.patrol.model.Patrol;
import org.wcs.smart.patrol.model.PatrolLeg;
import org.wcs.smart.patrol.model.PatrolLegDay;
import org.wcs.smart.patrol.model.PatrolLegMember;
import org.wcs.smart.patrol.model.PatrolMandate;
import org.wcs.smart.patrol.model.PatrolTransportType;
import org.wcs.smart.patrol.model.PatrolType;
import org.wcs.smart.patrol.model.PatrolWaypoint;
import org.wcs.smart.patrol.model.PatrolWaypointSource;
import org.wcs.smart.patrol.model.Team;
import org.wcs.smart.patrol.model.Track;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;

public class PatrolDataGenerator {

	public void generatePatrols(){
		Job j = new Job("generating patrol data"){

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				Session s = HibernateManager.openSession();
				try{
					s.beginTransaction();
					generatePatrols(s);
					s.getTransaction().commit();
				}catch (Exception ex){
					ex.printStackTrace();
					s.getTransaction().rollback();
				}finally{
					s.close();
				}
				return Status.OK_STATUS;
			}
		};
		j.schedule();
		
	}
	
	public void generatePatrols(Session session){
	
		Random random = new Random();
		
		int numberPatrols = 20;
		int daysPerPatrol = 3;
		
		int numEmployees = 4;
		
		int waypoints = 100;
		
		List<String> words = getStringValues();
		List<PatrolMandate> mandates = session.createCriteria(PatrolMandate.class)
				.add(Restrictions.eq("conservationArea", SmartDB.getCurrentConservationArea()))
				.list();
		
		List<Team> teams = session.createCriteria(Team.class)
				.add(Restrictions.eq("conservationArea", SmartDB.getCurrentConservationArea()))
				.list();
		
		List<Station> stations = session.createCriteria(Station.class)
				.add(Restrictions.eq("conservationArea", SmartDB.getCurrentConservationArea()))
				.list();
		
		List<Employee> employees = session.createCriteria(Employee.class)
				.add(Restrictions.eq("conservationArea", SmartDB.getCurrentConservationArea()))
				.list();
		
		List<Category> categories = session.createCriteria(Category.class)
				.add(Restrictions.eq("conservationArea", SmartDB.getCurrentConservationArea()))
				.list();
		categories.forEach(c->{
			if (c.getAttributes().size() > 0){
				c.getAttributes().forEach(a -> {
					if (a.getAttribute().getAttributeList() != null){
						a.getAttribute().getActiveListItems().forEach(z->z.getNames());
					}
				});
			}
		});
		
		
		List<PatrolTransportType> transportstypes = session.createCriteria(PatrolTransportType.class)
			.add(Restrictions.eq("conservationArea", SmartDB.getCurrentConservationArea()))
			.list();
			
		HashMap<PatrolType.Type, List<PatrolTransportType>> transports = new HashMap<PatrolType.Type, List<PatrolTransportType>>();
		for (PatrolTransportType t : transportstypes){
			List<PatrolTransportType> types = transports.get(t.getPatrolType());
			if (types == null){
				types = new ArrayList<PatrolTransportType>();
				transports.put(t.getPatrolType(), types);
			}
			types.add(t);
		}
		
		for (int i = 0; i < numberPatrols; i ++){
			System.out.println("Generating: " + i + "/" + numberPatrols);
			Patrol p = new Patrol();
			p.setConservationArea(SmartDB.getCurrentConservationArea());
			p.setArmed(Math.random() > 0.5);
			
			p.setComment(generateString(words, random));
			p.setObjective(generateString(words, random));
			
//			p.setId("TRK_" + i + "_" + PatrolHibernateManager.generatePatrolId(p, session));
			p.setId(PatrolHibernateManager.generatePatrolId(p, session));
			
			
			p.setTeam(teams.get( random.nextInt(teams.size() - 1) ));
			p.setStation(stations.get(random.nextInt(stations.size() - 1)));
			PatrolType.Type pt = PatrolType.Type.values()[random.nextInt(PatrolType.Type.values().length - 1)];
			p.setPatrolType(pt);
			
			PatrolLeg pl = new PatrolLeg();
			p.setLegs(new ArrayList<PatrolLeg>());
			p.getLegs().add(pl);
			pl.setPatrol(p);
			pl.setId("Leg 1");
			pl.setMandate(mandates.get( random.nextInt(mandates.size() - 1) ));
			
			Date startDate = new Date(random.nextInt(3) + 2013-1900, random.nextInt(11),  random.nextInt(28), 0, 0, 0);
			Calendar temp = Calendar.getInstance();
			temp.setTime(startDate);
			temp.add(Calendar.DAY_OF_MONTH, daysPerPatrol-1);
			Date endDate = temp.getTime();
			
			p.setStartDate(startDate);
			p.setEndDate(endDate);
			
			pl.setMembers(new ArrayList<PatrolLegMember>());
			while(pl.getMembers().size() < numEmployees){
				Employee next = employees.get(random.nextInt(employees.size() - 1));
				
				PatrolLegMember mem = new PatrolLegMember();
				mem.setMember(next);
				mem.setPatrolLeg(pl);
				
				if (!pl.getMembers().contains(mem)){
					pl.getMembers().add(mem);
				}
			}
			pl.getMembers().get(0).setIsLeader(true);
			pl.setLeader(pl.getMembers().get(0));
			pl.setPatrolLegDays(new ArrayList<PatrolLegDay>());
			pl.setStartDate(startDate);
			pl.setEndDate(endDate);
			
			List<PatrolTransportType> types = transports.get(p.getPatrolType());
			if (types.size() == 1){
				pl.setType(types.get(0));
			}else{
				pl.setType(types.get(random.nextInt(types.size() - 1)));
			}
			
			temp.setTime(startDate);
			
			session.save(p);
			session.flush();
			session.clear();
			
			double cx = (random.nextInt(5238) + 112700) / 10000.0;
			double cy = -1 * ((random.nextInt(10093) + 350) / 10000.0);
			
			for (int k = 0; k < daysPerPatrol; k++){
				System.out.println("patrol day: " + k + "/" + daysPerPatrol);
				PatrolLegDay pld = new PatrolLegDay();
				pld.setDate(temp.getTime());
				temp.add(Calendar.DAY_OF_MONTH, 1);
				
				pld.setPatrolLeg(pl);
				pl.getPatrolLegDays().add(pld);
				pld.setRestMinutes(random.nextInt(120));
				pld.setStartTime(new Time(random.nextInt(4) + 7, random.nextInt(59), random.nextInt(59)));
				pld.setEndTime(new Time(random.nextInt(4) + 15, random.nextInt(59), random.nextInt(59)));
				
				pld.setWaypoints(new ArrayList<PatrolWaypoint>());
				
				session.save(pld);
				session.flush();
				session.clear();
				
				Coordinate[] trackPnts = new Coordinate[waypoints];

				for (int x = 0; x < waypoints; x ++){
//					System.out.println(x + ":" + waypoints);
					PatrolWaypoint pw = new PatrolWaypoint();
					pw.setPatrolLegDay(pld);
					pld.getWaypoints().add(pw);
					
					
					Waypoint wp = new Waypoint();
					pw.setWaypoint(wp);
					
					wp.setConservationArea(p.getConservationArea());
					
					long range = pld.getStartTime().getTime() - pld.getEndTime().getTime()- 10000;
					long time = (long)((range / (waypoints * 1.0)) * x + pld.getStartTime().getTime()) + pld.getDate().getTime();
					
					wp.setDateTime( new Date(time));
					
					wp.setId(x+1);
					wp.setSourceId(PatrolWaypointSource.PATROL_WP_SOURCE_ID);
					wp.setObservations(new ArrayList<WaypointObservation>());
					
					cx += ((random.nextInt(1000)) / 100000.0) * (random.nextInt(10) <= 5 ? -1 : 1);
					cy += ((random.nextInt(1000)) / 100000.0) * (random.nextInt(10) <= 5 ? -1 : 1);
					
					trackPnts[x] = new Coordinate(cx,cy);
					
					wp.setX(cx);
					wp.setY(cy);
					
					int size = random.nextInt(8);
					for (int y = 0; y < size; y ++){
						WaypointObservation ob = new WaypointObservation();
						ob.setWaypoint(wp);
						wp.getObservations().add(ob);
					
						Category cat = categories.get(  random.nextInt(categories.size()-1)  );
						ob.setCategory(cat);
						
						if (Math.random() < 0.5){
							ob.setObserver(pl.getMembers().get(random.nextInt(pl.getMembers().size()-1)).getMember());
						}
				
						ob.setAttributes(new ArrayList<WaypointObservationAttribute>());
						for (CategoryAttribute a : cat.getAttributes()){
							WaypointObservationAttribute attributeValue = generateValue(a.getAttribute(), words, random, session);
							if (attributeValue != null){
								ob.getAttributes().add(attributeValue);
								attributeValue.setObservation(ob);
							}
						}
					
					}
					
					session.save(wp);
					session.flush();
					session.save(pw);
					session.flush();
					session.clear();
				}
				GeometryFactory gf = new GeometryFactory();
//				Coordinate[] trackPoints = new Coordinate[1200];
//				for (int z = 0; z<trackPoints.length; z++){
//					double cx = (random.nextInt(5238) + 112700) / 10000.0;
//					double cy = -1 * ((random.nextInt(10093) + 350) / 10000.0);
//					trackPoints[z] = new Coordinate(cx, cy);
//				}
//				LineString ls = gf.createLineString(trackPoints);
				
				LineString ls = gf.createLineString(trackPnts);
				
				Track t = new Track();
				t.setPatrolLegDay(pld);
				t.setLineString(ls);
				pld.setTrack(t);
				
				session.save(t);
				session.flush();
				session.clear();
				
			}
			
//			session.saveOrUpdate(p);
			session.flush();
			session.clear();
			
			session.getTransaction().commit();
			session.getTransaction().begin();
		}
		
		System.out.println("done");
	}
	
	private static String generateString(List<String> strings, Random random){
		StringBuilder sb= new StringBuilder();
		int numWords = random.nextInt(15);
		for (int i = 0; i < numWords; i ++){
			sb.append(strings.get( random.nextInt(strings.size() - 1)) + " ");
		}
		return sb.toString();
	}
	private static WaypointObservationAttribute generateValue(Attribute a, List<String> strings, Random random, Session session){
		WaypointObservationAttribute value = new WaypointObservationAttribute();
		value.setAttribute(a);
		
		if (a.getType() == Attribute.AttributeType.BOOLEAN){
			value.setNumberValue( Math.random() > 0.5 ? 1.0 : 0.0 );
		}else if (a.getType() == Attribute.AttributeType.DATE){
			value.setDateValue(new Date());
		}else if (a.getType() == Attribute.AttributeType.LIST){
			if (a.getAttributeList().size() == 0) return null;
			int index = 0;
			if(a.getAttributeList().size() > 1){
				index = random.nextInt(a.getAttributeList().size() - 1);
			}
			value.setAttributeListItem(a.getAttributeList().get(index));
			
		}else if (a.getType() == Attribute.AttributeType.NUMERIC){
			value.setNumberValue(Math.random() * 100 * Math.random() + Math.random());
		}else if (a.getType() == Attribute.AttributeType.TEXT){
			value.setStringValue(generateString(strings, random));
		}else if (a.getType() == Attribute.AttributeType.TREE){
			List<AttributeTreeNode> nodes = session.createCriteria(AttributeTreeNode.class)
					.add(Restrictions.eq("attribute", a))
					.list();
			int index = random.nextInt(nodes.size() - 1);
			value.setAttributeTreeNode(nodes.get(index));
		}
		
		return value;
	}
	
	public static List<String> getStringValues(){
		String[] words = {"a", "ability", "able", "about", "above", "accept", "according", "account", "across", "act", "action", "activity", "actually", "add", "address", "administration", "admit", "adult", "affect", "after", "again", "against", "age", "agency", "agent", "ago", "agree", "agreement", "ahead", "air", "all", "allow", "almost", "alone", "along", "already", "also", "although", "always", "American", "among", "amount", "analysis", "and", "animal", "another", "answer", "any", "anyone", "anything", "appear", "apply", "approach", "area", "argue", "arm", "around", "arrive", "art", "article", "artist", "as", "ask", "assume", "at", "attack", "attention", "attorney", "audience", "author", "authority", "available", "avoid", "away", "baby", "back", "bad", "bag", "ball", "bank", "bar", "base", "be", "beat", "beautiful", "because", "become", "bed", "before", "begin", "behavior", "behind", "believe", "benefit", "best", "better", "between", "beyond", "big", "bill", "billion", "bit", "black", "blood", "blue", "board", "body", "book", "born", "both", "box", "boy", "break", "bring", "brother", "budget", "build", "building", "business", "but", "buy", "by", "call", "camera", "campaign", "can", "cancer", "candidate", "capital", "car", "card", "care", "career", "carry", "case", "catch", "cause", "cell", "center", "central", "century", "certain", "certainly", "chair", "challenge", "chance", "change", "character", "charge", "check", "child", "choice", "choose", "church", "citizen", "city", "civil", "claim", "class", "clear", "clearly", "close", "coach", "cold", "collection", "college", "color", "come", "commercial", "common", "community", "company", "compare", "computer", "concern", "condition", "conference", "Congress", "consider", "consumer", "contain", "continue", "control", "cost", "could", "country", "couple", "course", "court", "cover", "create", "crime", "cultural", "culture", "cup", "current", "customer", "cut", "dark", "data", "daughter", "day", "dead", "deal", "death", "debate", "decade", "decide", "decision", "deep", "defense", "degree", "Democrat", "democratic", "describe", "design", "despite", "detail", "determine", "develop", "development", "die", "difference", "different", "difficult", "dinner", "direction", "director", "discover", "discuss", "discussion", "disease", "do", "doctor", "dog", "door", "down", "draw", "dream", "drive", "drop", "drug", "during", "each", "early", "east", "easy", "eat", "economic", "economy", "edge", "education", "effect", "effort", "eight", "either", "election", "else", "employee", "end", "energy", "enjoy", "enough", "enter", "entire", "environment", "environmental", "especially", "establish", "even", "evening", "event", "ever", "every", "everybody", "everyone", "everything", "evidence", "exactly", "example", "executive", "exist", "expect", "experience", "expert", "explain", "eye", "face", "fact", "factor", "fail", "fall", "family", "far", "fast", "father", "fear", "federal", "feel", "feeling", "few", "field", "fight", "figure", "fill", "film", "final", "finally", "financial", "find", "fine", "finger", "finish", "fire", "firm", "first", "fish", "five", "floor", "fly", "focus", "follow", "food", "foot", "for", "force", "foreign", "forget", "form", "former", "forward", "four", "free", "friend", "from", "front", "full", "fund", "future", "game", "garden", "gas", "general", "generation", "get", "girl", "give", "glass", "go", "goal", "good", "government", "great", "green", "ground", "group", "grow", "growth", "guess", "gun", "guy", "hair", "half", "hand", "hang", "happen", "happy", "hard", "have", "he", "head", "health", "hear", "heart", "heat", "heavy", "help", "her", "here", "herself", "high", "him", "himself", "his", "history", "hit", "hold", "home", "hope", "hospital", "hot", "hotel", "hour", "house", "how", "however", "huge", "human", "hundred", "husband", "I", "idea", "identify", "if", "image", "imagine", "impact", "important", "improve", "in", "include", "including", "increase", "indeed", "indicate", "individual", "industry", "information", "inside", "instead", "institution", "interest", "interesting", "international", "interview", "into", "investment", "involve", "issue", "it", "item", "its", "itself", "job", "join", "just", "keep", "key", "kid", "kill", "kind", "kitchen", "know", "knowledge", "land", "language", "large", "last", "late", "later", "laugh", "law", "lawyer", "lay", "lead", "leader", "learn", "least", "leave", "left", "leg", "legal", "less", "let", "letter", "level", "lie", "life", "light", "like", "likely", "line", "list", "listen", "little", "live", "local", "long", "look", "lose", "loss", "lot", "love", "low", "machine", "magazine", "main", "maintain", "major", "majority", "make", "man", "manage", "management", "manager", "many", "market", "marriage", "material", "matter", "may", "maybe", "me", "mean", "measure", "media", "medical", "meet", "meeting", "member", "memory", "mention", "message", "method", "middle", "might", "military", "million", "mind", "minute", "miss", "mission", "model", "modern", "moment", "money", "month", "more", "morning", "most", "mother", "mouth", "move", "movement", "movie", "Mr", "Mrs", "much", "music", "must", "my", "myself", "name", "nation", "national", "natural", "nature", "near", "nearly", "necessary", "need", "network", "never", "new", "news", "newspaper", "next", "nice", "night", "no", "none", "nor", "north", "not", "note", "nothing", "notice", "now", "n't", "number", "occur", "of", "off", "offer", "office", "officer", "official", "often", "oh", "oil", "ok", "old", "on", "once", "one", "only", "onto", "open", "operation", "opportunity", "option", "or", "order", "organization", "other", "others", "our", "out", "outside", "over", "own", "owner", "page", "pain", "painting", "paper", "parent", "part", "participant", "particular", "particularly", "partner", "party", "pass", "past", "patient", "pattern", "pay", "peace", "people", "per", "perform", "performance", "perhaps", "period", "person", "personal", "phone", "physical", "pick", "picture", "piece", "place", "plan", "plant", "play", "player", "PM", "point", "police", "policy", "political", "politics", "poor", "popular", "population", "position", "positive", "possible", "power", "practice", "prepare", "present", "president", "pressure", "pretty", "prevent", "price", "private", "probably", "problem", "process", "produce", "product", "production", "professional", "professor", "program", "project", "property", "protect", "prove", "provide", "public", "pull", "purpose", "push", "put", "quality", "question", "quickly", "quite", "race", "radio", "raise", "range", "rate", "rather", "reach", "read", "ready", "real", "reality", "realize", "really", "reason", "receive", "recent", "recently", "recognize", "record", "red", "reduce", "reflect", "region", "relate", "relationship", "religious", "remain", "remember", "remove", "report", "represent", "Republican", "require", "research", "resource", "respond", "response", "responsibility", "rest", "result", "return", "reveal", "rich", "right", "rise", "risk", "road", "rock", "role", "room", "rule", "run", "safe", "same", "save", "say", "scene", "school", "science", "scientist", "score", "sea", "season", "seat", "second", "section", "security", "see", "seek", "seem", "sell", "send", "senior", "sense", "series", "serious", "serve", "service", "set", "seven", "several", "sex", "sexual", "shake", "share", "she", "shoot", "short", "shot", "should", "shoulder", "show", "side", "sign", "significant", "similar", "simple", "simply", "since", "sing", "single", "sister", "sit", "site", "situation", "six", "size", "skill", "skin", "small", "smile", "so", "social", "society", "soldier", "some", "somebody", "someone", "something", "sometimes", "son", "song", "soon", "sort", "sound", "source", "south", "southern", "space", "speak", "special", "specific", "speech", "spend", "sport", "spring", "staff", "stage", "stand", "standard", "star", "start", "state", "statement", "station", "stay", "step", "still", "stock", "stop", "store", "story", "strategy", "street", "strong", "structure", "student", "study", "stuff", "style", "subject", "success", "successful", "such", "suddenly", "suffer", "suggest", "summer", "support", "sure", "surface", "system", "table", "take", "talk", "task", "tax", "teach", "teacher", "team", "technology", "television", "tell", "ten", "tend", "term", "test", "than", "thank", "that", "the", "their", "them", "themselves", "then", "theory", "there", "these", "they", "thing", "think", "third", "this", "those", "though", "thought", "thousand", "threat", "three", "through", "throughout", "throw", "thus", "time", "to", "today", "together", "tonight", "too", "top", "total", "tough", "toward", "town", "trade", "traditional", "training", "travel", "treat", "treatment", "tree", "trial", "trip", "trouble", "true", "truth", "try", "turn", "TV", "two", "type", "under", "understand", "unit", "until", "up", "upon", "us", "use", "usually", "value", "various", "very", "victim", "view", "violence", "visit", "voice", "vote", "wait", "walk", "wall", "want", "war", "watch", "water", "way", "we", "weapon", "wear", "week", "weight", "well", "west", "western", "what", "whatever", "when", "where", "whether", "which", "while", "white", "who", "whole", "whom", "whose", "why", "wide", "wife", "will", "win", "wind", "window", "wish", "with", "within", "without", "woman", "wonder", "word", "work", "worker", "world", "worry", "would", "write", "writer", "wrong", "yard", "yeah", "year", "yes", "yet", "you", "young", "your", "yourself"};
		return Arrays.asList(words);
	}
}
