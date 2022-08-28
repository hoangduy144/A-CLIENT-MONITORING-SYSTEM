package server;

import java.awt.EventQueue;

import javax.swing.JFrame;
import javax.swing.JLabel;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.awt.Color;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import java.awt.List;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;

import main.Action;
import main.ActionData;
import main.FileTreeModel;

import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.JTable;

public class WatchServer {

	private JFrame frame;
	private JLabel lblIP;
	private JLabel lblPort;
	private JLabel lblLogcat;
	private JButton btnStop;
	private JTable listClient = new JTable();
	private DefaultListModel<String> statusModel = new DefaultListModel<String>();
	private DefaultListModel<String> clientModel = new DefaultListModel<String>();
	private Hashtable<String, FileTreeModel> folderHolder = new Hashtable<String, FileTreeModel>();
	private Hashtable<String, ClientHandler> roomHash = new Hashtable<String, ClientHandler>();
	private DefaultTableModel logModel = new DefaultTableModel(0, 0);
	private DefaultTableModel clientTableModel = new DefaultTableModel();
	private TableRowSorter<TableModel> rowSorter;
	private TableRowSorter<TableModel> clientRowSorter;
	private JButton btnDirChange;
	private JTree tree;
	private JFrame folderFrame;
	private JButton btnChange;
	
	//static final ClassLoader loader = Thread.currentThread().getContextClassLoader();
	private static final String LOGCAT_FILENAME = "server_logcat.txt";
	private static final String LOGCAT_PARENT_PATH = System.getProperty("user.home") + "/FolderObserver/logcat/server/";
	
	//static ServerSocket variable
    private static ServerSocket server;
    //socket server port on which it will listen
    private static int PORT = 9876;
    
    private Thread connectingThread = null;
    
    private Runnable socketRunnable = new Runnable() {

		@Override
		public void run() {
			//create the socket server object
	        try {
				server = new ServerSocket(PORT);
				
		        while (true) {
		            statusModel.addElement("Waiting for the client request");
		            System.out.println("Waiting for the client request");
		            //creating socket and waiting for client connection
		            Socket socket = server.accept();
		            System.out.println("Client was accepted");
		           
		            //read from socket to ObjectInputStream object
		            ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
		            ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
		            
		            //convert ObjectInputStream object to String
		            ActionData actionData = (ActionData) ois.readObject();
		            //String[] msg = messages.split(",");
		            String createAt = actionData.getCreateAt();
		            String action = actionData.getAction();
		            String clientIP = actionData.getClientIP();
		            String message = actionData.getMessage();
		            System.out.println(message + ", action=" + action);
		            
		            if (action.equals(Action.LOGIN)) {
		            	folderHolder.put(clientIP, actionData.getFolderTree());
		            	addRowLog(clientIP, createAt, action, message);
		            	addRowClient(clientIP);
		            	writeLog(LOGCAT_PARENT_PATH, actionData.toString(), true);
		            	// ClientData cData = new ClientData(message, clientIP);
			            clientModel.addElement(clientIP);
			          //create ObjectOutputStream object
			            
			            //write object to Socket
			            oos.writeObject(new ServerActionData(Action.SERVER_LOGIN_RESPONSE, "Hello, you're accepted."));
			            startCommunicateEnvironment(clientIP, socket, ois, oos);
		            }
		        }
		    
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	        
			
		}
    	
    };
    private JTextField textFieldClient;
    private JTable tableLog;
    private JTextField textFieldLogcatFilter;
    
    private void readHistoriesLog() {
		try {
			File parent = new File(LOGCAT_PARENT_PATH);
			parent.mkdirs();
			File f = new File(LOGCAT_PARENT_PATH + LOGCAT_FILENAME);
			if (!f.exists()) {
				f.createNewFile();
			}
			InputStream bin = new FileInputStream(LOGCAT_PARENT_PATH + LOGCAT_FILENAME);
			if (bin != null) {
				System.out.println("readHistoriesLog from " + bin.toString());
				BufferedReader reader = new BufferedReader(new InputStreamReader(bin, "utf8"));
				
				while (reader.ready()) {
					String line = reader.readLine();
					String[] messages = line.split(",");
					if (messages.length == 4) {
						addRowLog(messages[2], messages[0], messages[1],  messages[3]);
					}
				}
				
				reader.close();
				bin.close();
			}
		} catch(Exception e ) {
			e.printStackTrace();
		}
	}
    
    private void startCommunicateEnvironment(String clientIP, Socket socket, ObjectInputStream ois, ObjectOutputStream oos) {
    	
    	ClientHandler talking = new ClientHandler(oos, ois, socket, new IClientAction() {
			
			@Override
			public void onAction(ActionData action) {
				String kindAction = action.getAction();
				switch (kindAction) {
				case Action.LOGOUT: {
	            	addRowLog(action.getClientIP(), action.getCreateAt(), kindAction, action.getMessage());
	            	removeRowClient(clientIP);
	            	writeLog(LOGCAT_PARENT_PATH, action.toString(), true);
	            	
		            for (int i = 0; i < clientModel.getSize(); i++) {
		            	if (clientModel.get(i).contains(clientIP)) {
							clientModel.remove(i);
							folderHolder.remove(clientIP);
							break;
						}
		            }
		            logout(clientIP);
		            break;
				}
			
				default: {
					addRowLog(action.getClientIP(), action.getCreateAt(), kindAction, action.getMessage());
					writeLog(LOGCAT_PARENT_PATH, action.toString(), true);
					break;
				}
			}
				
			}
		});
    	roomHash.put(clientIP, talking);
    	talking.start();
    }
    
    private void showTree(String ip) {
    	tree = new JTree(folderHolder.get(ip));
    	// The JTree can get big, so allow it to scroll.
	    JScrollPane scrollpane = new JScrollPane(tree);
	    
	    // Display it all in a window and make the window appear
	    folderFrame = new JFrame("Folder Chooser");
	    folderFrame.getContentPane().add(scrollpane, "Center");
	    folderFrame.setSize(400,600);
	    folderFrame.setVisible(true);
    }
    
    private void logout(String ip) {
    	roomHash.get(ip).logout();
    	roomHash.remove(ip);
    }
    
    private void initLogTable() {
		String[] header = new String[] { "No.", "Client IP", "Time", "Action", "Description"};
		logModel.setColumnIdentifiers(header);
		tableLog.setModel(logModel);
		rowSorter = new TableRowSorter<>(tableLog.getModel());
		tableLog.setRowSorter(rowSorter);
	}
    
    private void initClientTable() {
    	String[] header = new String[] { "Client IP"};
    	clientTableModel.setColumnIdentifiers(header);
    	listClient.setModel(clientTableModel);
    	clientRowSorter = new TableRowSorter<>(listClient.getModel());
    	listClient.setRowSorter(clientRowSorter);
		JScrollPane clientScroller = new JScrollPane(listClient);
		clientScroller.setBounds(642, 53, 228, 278);
		frame.getContentPane().add(clientScroller);
		
		
		listClient.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
			
			@Override
			public void valueChanged(ListSelectionEvent e) {
				btnDirChange.setEnabled(true);
			}
		});
    }
    
    private void addRowLog(String clientIP, String createAt, String action, String message) {
		int rowCount = logModel.getRowCount();
		logModel.addRow(new Object[] { String.valueOf(++rowCount), clientIP, createAt, action, message });
	}
    
    private void addRowClient(String ip) {
		clientTableModel.addRow(new Object[] { ip });
	}
    
    private void removeRowClient(String ip) {
    	for (int i = 0; i < clientTableModel.getRowCount(); i++) {
    		if (clientTableModel.getValueAt(i, 0) == ip) {
    			clientTableModel.removeRow(i);
    			return;
    		}
    		
    	}
    	
    }
    
    private void writeLog(String filePath, String line, boolean isAppend) {
		try {
			//PrintWriter writer = new PrintWriter(new File(loader.getResource(filePath).getFile()));
			//writer.append(line);
			//writer.append("\n");
			//writer.close();
			
			FileWriter fw = new FileWriter(filePath + LOGCAT_FILENAME, isAppend);
			fw.write(line);
			fw.write("\n");
			fw.close();
			
			System.out.println("wrote data to " + filePath + LOGCAT_FILENAME);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
    
    private void setupFilterEvents() {
		textFieldLogcatFilter.getDocument().addDocumentListener(new DocumentListener() {
			
			@Override
			public void removeUpdate(DocumentEvent e) {
				String text = textFieldLogcatFilter.getText();
				if (text.trim().length() == 0) {
					rowSorter.setRowFilter(null);
				} else {
					rowSorter.setRowFilter(RowFilter.regexFilter("(?i)" + text));
				}
			}
			
			@Override
			public void insertUpdate(DocumentEvent e) {
				String text = textFieldLogcatFilter.getText();
				if (text.trim().length() == 0) {
					rowSorter.setRowFilter(null);
				} else {
					rowSorter.setRowFilter(RowFilter.regexFilter("(?i)" + text));
				}
			}
			
			@Override
			public void changedUpdate(DocumentEvent e) {
				
			}
		});
		
		textFieldClient.getDocument().addDocumentListener(new DocumentListener() {
			
			@Override
			public void removeUpdate(DocumentEvent e) {
				String text = textFieldClient.getText();
				if (text.trim().length() == 0) {
					clientRowSorter.setRowFilter(null);
				} else {
					clientRowSorter.setRowFilter(RowFilter.regexFilter("(?i)" + text));
				}
			}
			
			@Override
			public void insertUpdate(DocumentEvent e) {
				String text = textFieldClient.getText();
				if (text.trim().length() == 0) {
					clientRowSorter.setRowFilter(null);
				} else {
					clientRowSorter.setRowFilter(RowFilter.regexFilter("(?i)" + text));
				}
			}
			
			@Override
			public void changedUpdate(DocumentEvent e) {
				
			}
		});
	}
    
    private void generateServerConnectionInfo() {
    	InetAddress idd;
		try {
			idd = InetAddress.getLocalHost();
			String ip = idd.getHostAddress();
			lblIP.setText(ip);
			lblPort.setText("" + PORT);
			
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
    
    private void handleClickEvents() {
    	btnStop.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				if (btnStop.getText().equals("Stop Server")) {
					stopConnection();
					btnStop.setText("Start Server");
				} else {
					suspendConnecting();
					btnStop.setText("Stop Server");
				}
			}
    		
    	});
    	
    	btnDirChange.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				String selectedClient = listClient.getValueAt(listClient.getSelectedRow(), 0).toString();
				System.out.println(selectedClient);
				showTree(selectedClient);
			}
		});
    	
    	btnChange.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				if (tree != null && tree.isSelectionEmpty() == false) {
					String pathString = tree.getSelectionPath().getLastPathComponent().toString();
					String selectedClient = listClient.getValueAt(listClient.getSelectedRow(), 0).toString();
					roomHash.get(selectedClient).changeFolder(pathString);
					folderFrame.setVisible(false);
				}
			}
		});
    }
    
	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					WatchServer window = new WatchServer();
					window.frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the application.
	 */
	public WatchServer() {
		initialize();
		generateServerConnectionInfo();
		suspendConnecting();
		handleClickEvents();
		initLogTable();
		initClientTable();
		setupFilterEvents();
		readHistoriesLog();
	}
	
	private void suspendConnecting() {
		connectingThread = new Thread(socketRunnable);
		connectingThread.start();
	}
	
	private void stopConnection() {
		connectingThread.interrupt();
		roomHash.forEach((ip, thread) -> 
		{
			removeRowClient(ip);
			thread.logout();
			thread.interrupt();
		});
		roomHash.clear();
		server = null;
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {	
		frame = new JFrame();
		frame.setBounds(100, 100, 876, 726);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.getContentPane().setLayout(null);
		
		JLabel lblIntro = new JLabel("Use this port, IP to connect your client...");
		lblIntro.setBounds(16, 17, 284, 25);
		frame.getContentPane().add(lblIntro);
		
		JLabel lblNewLabel = new JLabel("IP");
		lblNewLabel.setBounds(59, 54, 61, 16);
		frame.getContentPane().add(lblNewLabel);
		
		JLabel lblPortText = new JLabel("PORT");
		lblPortText.setBounds(59, 74, 61, 16);
		frame.getContentPane().add(lblPortText);
		
		lblIP = new JLabel("123456789");
		lblIP.setBackground(new Color(255, 255, 0));
		lblIP.setBounds(121, 54, 166, 16);
		frame.getContentPane().add(lblIP);
		
		lblPort = new JLabel("123456789");
		lblPort.setBounds(121, 74, 166, 16);
		frame.getContentPane().add(lblPort);
		
		lblLogcat = new JLabel("Logcat Filter");
		lblLogcat.setBounds(6, 352, 101, 16);
		frame.getContentPane().add(lblLogcat);
		
		btnStop = new JButton("Stop Server");
		btnStop.setEnabled(false);
		btnStop.setBounds(69, 102, 117, 29);
		frame.getContentPane().add(btnStop);
		
		tableLog = new JTable();
		JScrollPane listScroller = new JScrollPane(tableLog);
		listScroller.setBounds(6, 380, 864, 312);
		frame.getContentPane().add(listScroller);
		
		textFieldClient = new JTextField();
		textFieldClient.setBounds(642, 16, 228, 26);
		frame.getContentPane().add(textFieldClient);
		textFieldClient.setColumns(10);
		
		btnDirChange = new JButton("Show Folder");
		btnDirChange.setEnabled(false);
		btnDirChange.setBounds(753, 339, 117, 29);
		frame.getContentPane().add(btnDirChange);
		
		textFieldLogcatFilter = new JTextField();
		textFieldLogcatFilter.setBounds(100, 347, 251, 26);
		frame.getContentPane().add(textFieldLogcatFilter);
		textFieldLogcatFilter.setColumns(10);
		
		btnChange = new JButton("Change");
		btnChange.setBounds(638, 339, 78, 29);
		frame.getContentPane().add(btnChange);
	}
}
