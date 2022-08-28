package server;

public class ClientData {
	String dir;
	String ip;
	public ClientData(String dir, String ip) {
		this.dir = dir;
		this.ip = ip;
	}
	
	public String getDir() {
		return dir;
	}
	
	public void setDir(String dir) {
		this.dir = dir;
	}
	
	public String getIp() {
		return ip;
	}
	
	public void setIp(String ip) {
		this.ip = ip;
	}
	
	@Override
	public String toString() {
		return ip + ": " + dir;
	}
	
	
}
