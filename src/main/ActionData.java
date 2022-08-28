package main;

import java.io.Serializable;

public class ActionData implements Serializable {
	String createAt;
	String action;
	String clientIP;
	String message;
	FileTreeModel folderTree;
	
	public ActionData(String createAt, String action, String clientIP, String message, FileTreeModel folderTree) {
		super();
		this.createAt = createAt;
		this.action = action;
		this.clientIP = clientIP;
		this.message = message;
		this.folderTree = folderTree;
	}
	public String getCreateAt() {
		return createAt;
	}
	public void setCreateAt(String createAt) {
		this.createAt = createAt;
	}
	public String getAction() {
		return action;
	}
	public void setAction(String action) {
		this.action = action;
	}
	public String getClientIP() {
		return clientIP;
	}
	public void setClientIP(String clientIP) {
		this.clientIP = clientIP;
	}
	public String getMessage() {
		return message;
	}
	public void setMessage(String message) {
		this.message = message;
	}
	public FileTreeModel getFolderTree() {
		return folderTree;
	}
	public void setFolderTree(FileTreeModel folderTree) {
		this.folderTree = folderTree;
	}
	@Override
	public String toString() {
		return  String.format("%s,%s,%s,%s", createAt, action, clientIP, message);
	}
	
	
}
