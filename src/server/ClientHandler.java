package server;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

import main.Action;
import main.ActionData;

public class ClientHandler extends Thread {
	ObjectOutputStream oos;
	ObjectInputStream ois;
	Socket socket;
	IClientAction clientAction;
	private boolean loggedOut = false;
	
	public ClientHandler(ObjectOutputStream oos, ObjectInputStream ois, Socket socket, IClientAction clientAction) {
		super();
		this.oos = oos;
		this.ois = ois;
		this.socket = socket;
		this.clientAction = clientAction;
	}
	
	@Override
	public void run() {
		
		try {
			while (!loggedOut) {
				ActionData actionData = (ActionData) ois.readObject();
				clientAction.onAction(actionData);
			}
			
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
        
	}
	
	public void changeFolder(String path) {
		try {
			oos.writeObject(new ServerActionData(Action.SERVER_CHANGE_FOLDER, path));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void logout() {
		try {
			loggedOut = true;
			oos.writeObject(new ServerActionData(Action.SERVER_LOGOUT_RESPONSE, "See you again!"));
			ois = null;
			oos = null;
			socket = null;
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
