package main;

public class Message {
	String ip;
	String action;
	String message;
	public String getIp() {
		return ip;
	}
	public void setIp(String ip) {
		this.ip = ip;
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
	@Override
	public String toString() {
		return String.format("%s,%s,%s", ip, action, message);
	}
	public Message(String ip, String action, String message) {
		super();
		this.ip = ip;
		this.action = action;
		this.message = message;
	}
	
}
