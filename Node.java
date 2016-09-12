
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Random;
import java.util.Scanner;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.logging.*;


class RequestComparator implements Comparator<Request>{
	public int compare(Request o1, Request o2) {
		if(o1.sclock	!=	o2.sclock)
			return (o1.sclock-o2.sclock);
		else
			return (o1.nodeId-o2.nodeId);
	}
}
public class Node implements Runnable {
	
	public static Logger NodeLogger = Logger.getLogger(Node.class.toString());
	public Handler NodeFile;

    private String configPath;
    private static int identifier;

    public static String hostname;
    private int port;
    public static int interreq_delay;
    public static int cs_exec_delay;
    public static int max_requests;
    public static int number_of_nodes;
    private String[] all_nodes;

    
    private Queue<String> neighbors = new LinkedList<>();
    private String[] neighborlist;
    public static int number_neighbors;
    
    public static long startTime;
    public static int msgCount;
    public static boolean outstandingRequest;
    public static boolean done;
    public static volatile PriorityQueue<Request> requestQueue ;
    public static volatile boolean inCs;
    public static volatile boolean[] permissions;
    public static volatile boolean[] doneList;
    private static volatile int sClock;
    public static volatile int request_curr =0;
	private ServerSocket server_socket = null;
    
    public Node(int id, String configPath){
        this.identifier = id;
        this.configPath = configPath;
        try {
			NodeFile = new FileHandler("MachineFile-"+InetAddress.getLocalHost().getHostName().replace(".utdallas.edu","")+".log");
			NodeFile.setFormatter(new SimpleFormatter());
		    NodeLogger.addHandler(NodeFile);
		    NodeLogger.info("Entered the Machine Constructor");
		} catch (SecurityException e) {
			NodeLogger.severe("SecurityException while opening NodeFile" );
			e.printStackTrace();
		} catch (UnknownHostException e) {
			NodeLogger.severe("UnknownHostException while naming NodeFile");
			e.printStackTrace();
		} catch (IOException e) {
			NodeLogger.severe("IOException while opening NodeFile");
			e.printStackTrace();
		}
       
    }
    public synchronized static void setRequests(){
    	request_curr++;
    }
    public synchronized static void setsClock(){
    	sClock++;
    }
    public synchronized static void setMsgCount(){
    	msgCount++;
    }
    public static int getsClock(){
    	return sClock;
    }
    public static int getIdentifier(){
    	return identifier;
    }
	
    
    private void fileparser(String path, int id) throws Exception {
        Scanner scan_path = new Scanner(new File(path));
        identifier = id;
        String nextLine = scan_path.nextLine().trim();
        while (nextLine.equals("") || nextLine.charAt(0) == '#') {
            nextLine = scan_path.nextLine().trim();
        }
        if (nextLine.contains("#")) {
            number_of_nodes = Integer.parseInt(nextLine.split("#")[0].trim().split("\\s+")[0]);
        } else {
            number_of_nodes = Integer.parseInt(nextLine.trim().split("\\s+")[0]);
            interreq_delay = Integer.parseInt(nextLine.trim().split("\\s+")[1]);
            cs_exec_delay = Integer.parseInt(nextLine.trim().split("\\s+")[2]);
            max_requests = Integer.parseInt(nextLine.trim().split("\\s+")[3]);
        }
        this.permissions = new boolean[number_of_nodes];
        this.requestQueue = new PriorityQueue<Request>(max_requests,new RequestComparator());
        this.doneList = new boolean[number_of_nodes];
		this.all_nodes = new String[number_of_nodes];
		nextLine = scan_path.nextLine().trim();
		while (nextLine.equals("") || nextLine.charAt(0) == '#') {
			nextLine = scan_path.nextLine().trim();
		}
		for (int i = 0; i < number_of_nodes; i++) {
			if (nextLine.contains("#")) {
				this.all_nodes[i] = nextLine.split("#")[0].trim();
			} else {
				this.all_nodes[i] = nextLine.trim();

			}
			
			if (identifier == i) {
				String[] info = all_nodes[i].split("\\s+");
				hostname = info[1];
				port = Integer.parseInt(info[2]);

			}
			nextLine = scan_path.nextLine();
		} 
    }
    //display class only for debugging
    public void display() throws Exception{
        System.out.println("number of nodes: "+number_of_nodes);
        System.out.println("Host name of machine: "+hostname);
        System.out.println("Server side running on port: "+port);
        System.out.println("Interrequest delay: " + interreq_delay);
        System.out.println("cs exec delay: "+ cs_exec_delay);
        System.out.println("All nodes in system: ");
        for (int i= 0;i< number_of_nodes;i++)
            System.out.println(all_nodes[i]);
    }
    
    
    
	@Override
	public void run() {
		// TODO Auto-generated method stub
		try {
			fileparser(configPath, identifier);
			initialize(number_of_nodes);
			runServer(hostname,port);
			while(request_curr <max_requests){
				csEnter();
				Thread.sleep(randomGenerator(interreq_delay));
				//this.setRequests();
				request_curr++;
			}
			csLeave();
			done=true;
			if(getIdentifier() !=0){
				String[] temp_info = all_nodes[0].split(" ");
				int port_temp = Integer.parseInt(temp_info[2]);
				int nodeid_temp = Integer.parseInt(temp_info[0]);
				String hostname_temp = temp_info[1];
				Message msg = new Message("done", this.getsClock(),this.msgCount, this.getIdentifier());
				clientThread c = new clientThread(nodeid_temp,hostname_temp,port_temp);
				try {
					this.setMsgCount();
					c.connect(hostname_temp, port_temp, msg);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			else{
				Message msg = new Message("done", this.getsClock(),this.msgCount, this.getIdentifier());
				serverThread.writeCsStats(msg);
				this.doneList[0]=true;
				if(serverThread.doneListChecker()){
					Message finaldone = new Message("dummymsg", 0,0,0);
					serverThread.writeCsStats(finaldone);
				}
			}
			System.out.println("Done with all requests "+this.hostname);
			NodeLogger.info("Done with all requests "+this.hostname);
			while(true){
				
			}
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void csEnter(){
		Request req = new Request(this.getIdentifier(),this.getsClock());
		requestQueue.add(req);
		outstandingRequest = true;
		NodeLogger.info("Sending request to needed keys");
		for(int i=0;i<number_of_nodes;i++){
			if(i!=getIdentifier()){
				if(permissions[i] != true){
					String[] temp_info = all_nodes[i].split(" ");
					int port_temp = Integer.parseInt(temp_info[2]);
					int nodeid_temp = Integer.parseInt(temp_info[0]);
					String hostname_temp = temp_info[1];
					Message msg = new Message("req", this.getsClock(),nodeid_temp, identifier);
					clientThread c = new clientThread(nodeid_temp,hostname_temp,port_temp);
					try {
						this.setMsgCount();
						c.connect(hostname_temp, port_temp, msg);
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		}
		NodeLogger.info("In Csenter waiting on reply, permissions are: ");
		int []permCopy= new int[number_of_nodes];
		for(int i=0;i<number_of_nodes;i++){
			if(permissions[i]==true)
				permCopy[i]=1;
			else
				permCopy[i]=0;
		}
		NodeLogger.info(Arrays.toString(permCopy));
		while(!permissionChecker()){
		}
		csExecute();
	}
	
	public void csExecute(){
		inCs = true;
		setsClock();
		if(getIdentifier() !=0){
			String[] temp_info = all_nodes[0].split(" ");
			int port_temp = Integer.parseInt(temp_info[2]);
			int nodeid_temp = Integer.parseInt(temp_info[0]);
			String hostname_temp = temp_info[1];
			Message msg = new Message("enterCS", this.getsClock(),nodeid_temp, this.getIdentifier());
			clientThread c = new clientThread(nodeid_temp,hostname_temp,port_temp);
			try {
				this.setMsgCount();
				c.connect(hostname_temp, port_temp, msg);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		else{
			Message msg = new Message("enterCS", this.getsClock(),0, this.getIdentifier());
			serverThread.writeCsLog(msg);
		}
		// send message to root to log clock at enter
		try {
			Thread.sleep(randomGenerator(cs_exec_delay));
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		NodeLogger.info("Executing CS");
		//send message to root to log clock at exit
		
		setsClock();
		if(getIdentifier() !=0){
			String[] temp_info = all_nodes[0].split(" ");
			int port_temp = Integer.parseInt(temp_info[2]);
			int nodeid_temp = Integer.parseInt(temp_info[0]);
			String hostname_temp = temp_info[1];
			Message msg = new Message("exitCS", this.getsClock(),nodeid_temp, this.getIdentifier());
			clientThread c = new clientThread(nodeid_temp,hostname_temp,port_temp);
			try {
				this.setMsgCount();
				c.connect(hostname_temp, port_temp, msg);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		else{
			Message msg = new Message("exitCS", this.getsClock(),0, this.getIdentifier());
			serverThread.writeCsLog(msg);
		}
		//remove your request from top of priority queue
		Request reqrem = new Request(getIdentifier(),(getsClock()-2));
		requestQueue.remove(reqrem);
		inCs = false;
		outstandingRequest= false; 
		csLeave();
	}
	public void csLeave(){
		//CHECK THIS
		// clear defer queue and reset your permissions array (keys) accordingly
		//define these messages as release messages
		while(requestQueue.peek() !=null){
			if((requestQueue.peek().nodeId)== getIdentifier())
				requestQueue.remove();
			else{
			
			Request req = requestQueue.poll();
			Message msg = new Message("release", getsClock(),req.nodeId, getIdentifier());
			String[] temp_info = all_nodes[req.nodeId].split(" ");
			int port_temp = Integer.parseInt(temp_info[2]);
			int nodeid_temp = Integer.parseInt(temp_info[0]);
			String hostname_temp = temp_info[1];
			clientThread c = new clientThread(nodeid_temp,hostname_temp,port_temp);
			try {
				this.setMsgCount();
				c.connect(hostname_temp, port_temp, msg);
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			permissions[req.nodeId]= false;
			}
		}
		NodeLogger.info("Sent release messages");
		//}
		permissions[getIdentifier()]= true;
	}
	



	private void runServer(String hostname, int port) {
		try {
			server_socket = new ServerSocket(port);
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		serverThread s = new serverThread(server_socket, this.getIdentifier(), this.all_nodes);
        Thread t = new Thread(s);
        t.start();
		
	}

	private void initialize(int number_of_nodes) {
		done=false;
		outstandingRequest = false;
		msgCount =0;
		startTime = System.currentTimeMillis();
		for(int i=0;i<number_of_nodes;i++){
			if(i<identifier)
				permissions[i]= false;
			else
				permissions[i]=true;
			doneList[i]=false;
		}
		permissions[this.getIdentifier()]= true;
		sClock=0;
		//distribution of keys to be done here dont know how to distribute initially
	}
	public boolean permissionChecker(){
		for(int i=0;i<number_of_nodes;i++){
			if(permissions[i]==false)
				return false;
		}
		return true;
	}
	
	public int randomGenerator(int x){
		Random random = new Random();
		int min = (int) (x-(0.2)*x);
		int max = (int)(x+(0.2)*x);
		return random.nextInt((max - min) + 1) + min;
	}
}
