package server;

import java.io.Serializable;

import main.Action;

public class ServerActionData implements Serializable {
	String action;
	String message;
	public ServerActionData(String action, String message) {
		super();
		this.action = action;
		this.message = message;
	}
	public String getAction() {
		return action;
	}
	public void setAction(String action) {
		this.action = action;
	}
	public String getMessage() {
		return message;
	}
	public void setMessage(String message) {
		this.message = message;
	}
	
}
