package main;

import java.awt.BorderLayout;
import java.awt.EventQueue;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.RowFilter;
import javax.swing.SwingConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import javax.swing.tree.TreeModel;

import server.ServerActionData;

import javax.swing.JButton;
import javax.swing.JTable;

public class WatchClient {

	private JFrame frame;
	private JTextField textIP;
	private JTextField textPort;
	private JLabel lblStatus;
	private JButton btnConnect;
	private Hashtable<String, Thread> threadHolder = new Hashtable<String, Thread>();
	private Hashtable<String, WatchService> watcherHolder = new Hashtable<String, WatchService>();
	private DefaultTableModel tableModel = new DefaultTableModel(0, 0);
	
	private static final String LOGCAT_FILENAME = "client_logcat.txt";
	private static final String LOGCAT_PARENT_PATH = System.getProperty("user.home") + "/FolderObserver/logcat/client/";
	
	//establish socket connection to server
	private ObjectOutputStream oos;
	private ObjectInputStream ois;
	private Socket socket;
	private String ip;
	private JTable tableLog = new JTable();
	private TableRowSorter<TableModel> rowSorter;
	private JTextField textFieldLogFilter;
	private String currentPathObserving = "";
	private Thread receiverThread;
	
	private FileTreeModel folderModel = new FileTreeModel(new File(System.getProperty("user.home")));
	static final ClassLoader loader = Thread.currentThread().getContextClassLoader();

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					WatchClient window = new WatchClient();
					window.frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}
	
	private void writeLog(String filePath, String line, boolean isAppend) {
		try {
			//String absPath = loader.getResource(filePath).getFile();
			File parent = new File(filePath);
			parent.mkdirs();
			File f = new File(filePath + LOGCAT_FILENAME);
			if (!f.exists()) {
				f.createNewFile();
			}
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
	
	private void connect(String ip, int port) {
		try {
			InetAddress host = InetAddress.getLocalHost();
			this.ip = host.getHostName();
			socket = new Socket(ip, port);
			try {
				ActionData message = new ActionData(convertMillisecondToDate(System.currentTimeMillis()), Action.LOGIN, this.ip, currentPathObserving, folderModel);
				
				sendActionToServer(message);
				receiveMessageFromServer();
				
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
				showDialog(frame, "Connection Error", "Please try connect again");
			}
			
		} catch (IOException e) {
		  	e.printStackTrace();
			showDialog(frame, "Cannot connect to server", e.getMessage());
		}
        
	}
	
	private void sendActionToServer(ActionData message) throws IOException, ClassNotFoundException {
		
		//write to socket using ObjectOutputStream
		if (oos == null) {
			oos = new ObjectOutputStream(socket.getOutputStream());
		}
		
        oos.writeObject(message);
	}
	
	private void receiveMessageFromServer() throws IOException, ClassNotFoundException {
		//read the server response message
        if (ois == null) {
        	ois = new ObjectInputStream(socket.getInputStream());
		}
        
        receiverThread = new Thread(new Runnable() {
			
			@Override
			public void run() {
				while (true) {
		        	ServerActionData serverAction;
					try {
						serverAction = (ServerActionData) ois.readObject();
						String actString = serverAction.getAction();
			        	System.out.println("receiveMessageFromServer - reading server action " + actString);
			        	
			        	if (actString.equals(Action.SERVER_LOGIN_RESPONSE)) {
			        		System.out.println("receiveMessageFromServer - update views");
			        		ActionData action = new ActionData(convertMillisecondToDate(System.currentTimeMillis()), actString, ip, serverAction.getMessage(), null);
			        		lblStatus.setText(serverAction.getMessage());
							btnConnect.setText("Disconnect");
							addRowLog(action);
			        		writeLog(LOGCAT_PARENT_PATH, action.toString(), true);
			        		
			        	} else if (actString.equals(Action.SERVER_CHANGE_FOLDER)) {
			        		ActionData action = new ActionData(convertMillisecondToDate(System.currentTimeMillis()), actString, ip, "Changed observable folder to " + serverAction.getMessage(), null);
			        		addRowLog(action);
			        		writeLog(LOGCAT_PARENT_PATH, action.toString(), true);
			        		startNewRegisterFolder(serverAction.getMessage());
			        	} else if (actString.equals(Action.SERVER_LOGOUT_RESPONSE)) {
			        		ActionData action = new ActionData(convertMillisecondToDate(System.currentTimeMillis()), actString, ip, serverAction.getMessage(), null);
			        		addRowLog(action);
			        		writeLog(LOGCAT_PARENT_PATH, action.toString(), true);
			        		disconnect();
			        		break;
			        	}
					} catch (ClassNotFoundException | IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
		        	
				}
			}
		});
        receiverThread.start();
        
	}
	
	private void disconnect() {
		System.out.println("disconnect");
		receiverThread.interrupt();
		if (socket.isConnected()) {
			//if (oos != null && ois != null) {
			//	oos.close();
			//	ois.close();
			//}
			
			ois = null;
			oos = null;
			btnConnect.setText("Connect");
			
		}
	}
	
	private void initFolderObersever() {
		int defaultPosition = 15;
		JTree tree = new JTree(folderModel);
		//System.out.print(tree.getPathForRow(defaultPosition).getLastPathComponent());
		File file = new File(tree.getPathForRow(defaultPosition).getLastPathComponent().toString());
		while (!file.isDirectory()) {
			file = new File(tree.getPathForRow(++defaultPosition).getLastPathComponent().toString());
		}
		String parent = file.getParent();
		System.out.println("current: " + file.getPath() + ", currentAbsolute: " + file.getAbsolutePath() + ", parent=" + parent);
		currentPathObserving = file.getPath();
		// register parent path
		registerFolder(parent, true);
		// register current
		registerFolder(file.getPath(), false);
		
	}
	
	private void startNewRegisterFolder(String path) {
		// disconnect old watchers
		threadHolder.forEach((dir, thread) -> {
			thread.interrupt();
		});
		watcherHolder.forEach((dir, watcher) -> {
			try {
				watcher.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		});
		
		File file = new File(path);
		String parent = file.getParent();
		currentPathObserving = file.getPath();
		// register parent path
		registerFolder(parent, true);
		// register current
		registerFolder(file.getPath(), false);
	}
	
	private void registerFolder(String path, Boolean isParent) {
		Runnable watchingRunnable = new Runnable() {

			@Override
			public void run() {
				WatchService watcher;
				boolean isParentWatching = isParent;
				String livePath = path;
				try {
					watcher = FileSystems.getDefault().newWatchService();
					Path dir = Paths.get(path);
			        dir.register(watcher, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_DELETE,
			                StandardWatchEventKinds.ENTRY_MODIFY);
			        
			        watcherHolder.put(path, watcher);
			 
			        System.out.println("Watch Service registered for dir: " + dir.getFileName() + ", oldLivePath=" + livePath);
			 
			        WatchKey key = null;
			        while (true) {
			            try {
			                // System.out.println("Waiting for key to be signalled...");
			                key = watcher.take();
			            } catch (InterruptedException ex) {
			                System.out.println("InterruptedException: " + ex.getMessage());
			                return;
			            }
			            
			            List<WatchEvent<?>> events = key.pollEvents();
			            for (WatchEvent<?> event : events) {
			                // Retrieve the type of event by using the kind() method.
			                WatchEvent.Kind<?> kind = event.kind();
			                WatchEvent<Path> ev = (WatchEvent<Path>) event;
			                Path filePath = ev.context();
			                
			                //(new File(absPath.toString() + "/")).isDirectory();
			                String fileName = filePath.toString().replace(".", ",");
			                int splitResult = fileName.split(",").length;
			                boolean isFile = fileName.contains(",") && splitResult == 2;
			                String fileCheckMsg;
			                if (isFile) {
			                	fileCheckMsg = "File";
			                } else {
			                	fileCheckMsg = "Folder"; 
			                }
			               
			                if(isRenameFile(events)) {
			                	WatchEvent<Path> delEv = (WatchEvent<Path>) events.get(1);
			                	Path delFileNamePaths = delEv.context();
			                	String message = String.format("%s %s was renamed to %s\n", fileCheckMsg, delFileNamePaths, filePath.getFileName());
			                	System.out.println(message);
			                	ActionData action = new ActionData(convertMillisecondToDate(System.currentTimeMillis()), Action.RENAME, ip, message, null);
			                	try {
			                		sendActionToServer(action);
								} catch (ClassNotFoundException e) {
									e.printStackTrace();
								}
			                	System.out.println("sent action to server");
			                	addRowLog(action);
			                	writeLog(LOGCAT_PARENT_PATH, action.toString(), true);
			                	
			                	if (isParentWatching) {
									// if this is parent renamed, interrupt current thread and start new thread with new path
			                		currentPathObserving = livePath + "/" + filePath.getFileName().toString();
			                		registerFolder(currentPathObserving, false);
			                		interruptObservePath(livePath + "/" + delFileNamePaths.getFileName().toString());
								}
			                	break;
			                }
			               
			                if (kind == StandardWatchEventKinds.ENTRY_CREATE) {
			                	if (isParentWatching == true) {
			                		System.out.printf("%s %s was created in parent\n", fileCheckMsg, filePath.getFileName());
			                	} else {
			                		System.out.printf("%s %s was created\n", fileCheckMsg, filePath.getFileName());
			                		String message = String.format("%s %s was created\n", fileCheckMsg, filePath.getFileName());
				                	ActionData action = new ActionData(convertMillisecondToDate(System.currentTimeMillis()), Action.CREATE, ip, message, null);
				                	try {
				                		sendActionToServer(action);
									} catch (ClassNotFoundException e) {
										e.printStackTrace();
									}
				                	addRowLog(action);
				                	writeLog(LOGCAT_PARENT_PATH, action.toString(), true);
			                	}
			                } else if (kind == StandardWatchEventKinds.ENTRY_MODIFY) {
			                	if (isParentWatching == true) {
			                		System.out.printf("%s %s was modified in parent\n", fileCheckMsg, filePath.getFileName());
			                	} else {
			                		System.out.printf("%s %s was modified\n", fileCheckMsg, filePath.getFileName());
			                		String message = String.format("%s %s was modified\n", fileCheckMsg, filePath.getFileName());
				                	ActionData action = new ActionData(convertMillisecondToDate(System.currentTimeMillis()), Action.MODIFY, ip, message, null);
				                	try {
				                		sendActionToServer(action);
									} catch (ClassNotFoundException e) {
										e.printStackTrace();
									}
				                	addRowLog(action);
				                	writeLog(LOGCAT_PARENT_PATH, action.toString(), true);
			                	}
			                	
			                } else if (kind == StandardWatchEventKinds.ENTRY_DELETE) {
			                	if (isParentWatching == true) {
			                		System.out.printf("%s %s was deleted in parent\n", fileCheckMsg, filePath.getFileName());
			                	} else {
			                		System.out.printf("%s %s was deleted\n", fileCheckMsg, filePath.getFileName());
			                		String message = String.format("%s %s was deleted\n", fileCheckMsg, filePath.getFileName());
				                	ActionData action = new ActionData(convertMillisecondToDate(System.currentTimeMillis()), Action.DELETE, ip, message, null);
				                	try {
				                		sendActionToServer(action);
									} catch (ClassNotFoundException e) {
										e.printStackTrace();
									}
				                	addRowLog(action);
				                	writeLog(LOGCAT_PARENT_PATH, action.toString(), true);
			                	}
			                	
			                }
			            }
			 
			            boolean valid = key.reset();
			            if (!valid) {
			                break;
			            }
			        }
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			
		};
		
		Thread thread = new Thread(watchingRunnable);
		thread.start();
		
		interruptObservePath(path);
		threadHolder.put(path, thread);
	}
	
	private boolean isRenameFile(List<WatchEvent<?>> events) {
		return events.size() == 2 
				&& events.get(0).kind() == StandardWatchEventKinds.ENTRY_CREATE 
				&& events.get(1).kind() == StandardWatchEventKinds.ENTRY_DELETE;
	}
	
	private void interruptObservePath(String path) {
		System.out.println("interruptObservePath path=" + path);
		if (threadHolder.containsKey(path)) {
			threadHolder.get(path).interrupt();
		}
		
	}
	
	private void showDialog(JFrame frame, String title, String content) {
		JPanel panel = new JPanel(new BorderLayout(5, 5));
	    JPanel label = new JPanel(new GridLayout(0, 1, 2, 2));
	    label.add(new JLabel(content, SwingConstants.RIGHT));
	    panel.add(label, BorderLayout.WEST);
		JOptionPane.showMessageDialog(frame, panel, title, JOptionPane.PLAIN_MESSAGE);
	}
	
	private void handleClickEvents() {
		btnConnect.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				if (btnConnect.getText().equals("Connect")) {
					String ip = textIP.getText().toString();
					int port = Integer.parseInt(textPort.getText().toString());
					
					connect(ip, port);
				} else {
					ActionData message = new ActionData(convertMillisecondToDate(System.currentTimeMillis()), Action.LOGOUT, ip, "Logging out", null);
					try {
						sendActionToServer(message);
					} catch (ClassNotFoundException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					} catch (IOException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
				}
				
			}
			
		});
	}

	/**
	 * Create the application.
	 */
	public WatchClient() {
		initialize();
		setupFilterEvents();
		handleClickEvents();
		initFolderObersever();
		initLogTable();
	}
	
	private void initLogTable() {
		String[] header = new String[] { "No.", "Time", "Action", "Description"};
		tableModel.setColumnIdentifiers(header);
		tableLog.setModel(tableModel);
		rowSorter = new TableRowSorter<>(tableLog.getModel());
		tableLog.setRowSorter(rowSorter);
	}
	
	private void setupFilterEvents() {
		textFieldLogFilter.getDocument().addDocumentListener(new DocumentListener() {
			
			@Override
			public void removeUpdate(DocumentEvent e) {
				String text = textFieldLogFilter.getText();
				if (text.trim().length() == 0) {
					rowSorter.setRowFilter(null);
				} else {
					rowSorter.setRowFilter(RowFilter.regexFilter("(?i)" + text));
				}
			}
			
			@Override
			public void insertUpdate(DocumentEvent e) {
				String text = textFieldLogFilter.getText();
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
	}
	
	private void addRowLog(ActionData action) {
		int rowCount = tableModel.getRowCount();
		tableModel.addRow(new Object[] { String.valueOf(++rowCount), action.createAt, action.action, action.message });
	}
	
	private String convertMillisecondToDate(long millis) {
		Calendar calendar = Calendar.getInstance();
		calendar.setTimeInMillis(millis);

		int year = calendar.get(Calendar.YEAR);
		int month = calendar.get(Calendar.MONTH) + 1;
		int day = calendar.get(Calendar.DAY_OF_MONTH);
		int hour = calendar.get(Calendar.HOUR);
		int min = calendar.get(Calendar.MINUTE);
		int sec = calendar.get(Calendar.SECOND);
		int millisc = calendar.get(Calendar.MILLISECOND);
		
		return "" + year + "-" + month + "-" +  day + " " + hour + ":" + min + ":" + sec + ":" + millisc;
		
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		frame = new JFrame();
		frame.setBounds(100, 100, 713, 537);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.getContentPane().setLayout(null);
		
		JLabel lblNewLabel = new JLabel("IP");
		lblNewLabel.setBounds(254, 79, 42, 16);
		frame.getContentPane().add(lblNewLabel);
		
		JLabel lblPort = new JLabel("PORT");
		lblPort.setBounds(254, 107, 42, 16);
		frame.getContentPane().add(lblPort);
		
		textIP = new JTextField();
		textIP.setBounds(308, 74, 130, 26);
		frame.getContentPane().add(textIP);
		textIP.setColumns(10);
		
		textPort = new JTextField();
		textPort.setColumns(10);
		textPort.setBounds(308, 102, 130, 26);
		frame.getContentPane().add(textPort);
		
		btnConnect = new JButton("Connect");
		btnConnect.setBounds(282, 135, 117, 29);
		frame.getContentPane().add(btnConnect);
		
		JLabel lblNewLabel_1 = new JLabel("Let's connect to your server");
		lblNewLabel_1.setBounds(254, 46, 184, 16);
		frame.getContentPane().add(lblNewLabel_1);
		
		lblStatus = new JLabel("Requesting...");
		lblStatus.setBounds(254, 176, 184, 16);
		frame.getContentPane().add(lblStatus);
		
		//frame.getContentPane().add(tableLog);
		JScrollPane tableScroller = new JScrollPane(tableLog);
		tableScroller.setBounds(6, 240, 701, 263);
		frame.getContentPane().add(tableScroller);
		
		JLabel lblNewLabel_2 = new JLabel("Logcat Filter");
		lblNewLabel_2.setBounds(6, 214, 88, 16);
		frame.getContentPane().add(lblNewLabel_2);
		
		textFieldLogFilter = new JTextField();
		textFieldLogFilter.setBounds(87, 209, 301, 26);
		frame.getContentPane().add(textFieldLogFilter);
		textFieldLogFilter.setColumns(10);
	}
}
