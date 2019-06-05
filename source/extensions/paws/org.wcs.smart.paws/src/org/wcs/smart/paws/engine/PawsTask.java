package org.wcs.smart.paws.engine;

import java.time.LocalDate;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

public class PawsTask {

	private String taskId;
	private String status;
	private String endpoint;
	private LocalDate timestamp;
	
	private PawsTask(String taskId, String status, String endpoint, LocalDate timestamp) {
		this.taskId = taskId;
		this.status = status;
		this.endpoint = endpoint;
		this.timestamp = timestamp;
		
	}
	
	public String getTaskId() { return this.taskId; }
	public String getStatus() { return this.status; }
	public String getEndPoint() { return this.endpoint; }
	public LocalDate getTimestamp() { return this.timestamp; }
	
	
	public static PawsTask parse(String json) throws Exception {
		JSONParser parse = new JSONParser();
		JSONObject item = (JSONObject) parse.parse(json);
		
		String tid = (String) item.get("TaskId");
		if (tid == null) throw new Exception("Task Id not provided.");
		String status = (String) item.get("Status");
		String endpoint = (String) item.get("Endpoint");

		LocalDate d = null;
		try {
			d = LocalDate.parse((String)item.get("Timestamp"));
		}catch (Exception ex) {
			ex.printStackTrace();
		}
		return new PawsTask(tid, status, endpoint, d);
	}
}
