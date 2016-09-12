

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;


public class serverThread implements Runnable{
	
	private int id;
	private ServerSocket soc_server;
    private String[] all_nodes;
    private int port;

    public serverThread(ServerSocket soc_server,int id,String[] all_nodes){
		this.soc_server = soc_server;
		this.id = id;
		this.all_nodes = all_nodes;
	}
	@Override
	public void run() {
		Node.NodeLogger.info("Starting serverthread");
		while (true) {
            try {
				runserver(soc_server.accept());
			} catch (IOException e) {
				e.printStackTrace();
			}
        }
	}
	private void runserver(Socket s) {
		try{
			ObjectInputStream input;
		input = new ObjectInputStream(s.getInputStream());
		//while(true){
		Object obj=null;
		try {
			obj = input.readObject();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		if (obj instanceof Message) {
			Message msg = (Message) obj;
			if(msg.getType().equals("req")){
				System.out.println(Node.hostname+ " Request received from " +msg.getfromid());
				if(Node.inCs){
					Request req = new Request(msg.getfromid(),msg.getsClock());
					Node.NodeLogger.info("adding req from" +msg.getfromid());
					Node.requestQueue.add(req);
				}
				else if(Node.outstandingRequest){
					if(Node.getsClock()>msg.getsClock()){
						Message msgReply = new Message("reply", Node.getsClock(),msg.getfromid(), Node.getIdentifier());
						Message msgReq = new Message("req",Node.getsClock(),msg.getfromid(), Node.getIdentifier());
						String[] temp_info = all_nodes[msg.getfromid()].split(" ");
						int port_temp = Integer.parseInt(temp_info[2]);
						int nodeid_temp = Integer.parseInt(temp_info[0]);
						String hostname_temp = temp_info[1];
						clientThread c = new clientThread(nodeid_temp,hostname_temp,port_temp);
						try {
							Node.setMsgCount();
							Node.setMsgCount();
							c.connect(hostname_temp, port_temp, msgReply);
							c.connect(hostname_temp, port_temp, msgReq);
						} catch (Exception e) {
							e.printStackTrace();
						}
						Node.permissions[msg.getfromid()]= false;
					}
					else if(Node.getsClock()==msg.getsClock() && Node.getIdentifier() >msg.getfromid()){
						Message msgReply = new Message("reply", Node.getsClock(),msg.getfromid(), Node.getIdentifier());
						Message msgReq = new Message("req",Node.getsClock(),msg.getfromid(), Node.getIdentifier());
						String[] temp_info = all_nodes[msg.getfromid()].split(" ");
						int port_temp = Integer.parseInt(temp_info[2]);
						int nodeid_temp = Integer.parseInt(temp_info[0]);
						String hostname_temp = temp_info[1];
						clientThread c = new clientThread(nodeid_temp,hostname_temp,port_temp);
						try {
							Node.setMsgCount();
							Node.setMsgCount();
							c.connect(hostname_temp, port_temp, msgReply);
							c.connect(hostname_temp, port_temp, msgReq);
						} catch (Exception e) {
							e.printStackTrace();
						}
						Node.permissions[msg.getfromid()]= false;
					}
					else{
						Request req = new Request(msg.getfromid(),msg.getsClock());
						Node.NodeLogger.info("adding req from" +msg.getfromid());
						Node.requestQueue.add(req);
					}
				}
				else if(!Node.outstandingRequest || Node.done){
					Message msgReply = new Message("reply", Node.getsClock(),msg.getfromid(), Node.getIdentifier());
					String[] temp_info = all_nodes[msg.getfromid()].split(" ");
					int port_temp = Integer.parseInt(temp_info[2]);
					int nodeid_temp = Integer.parseInt(temp_info[0]);
					String hostname_temp = temp_info[1];
					clientThread c = new clientThread(nodeid_temp,hostname_temp,port_temp);
					try {
						Node.setMsgCount();
						c.connect(hostname_temp, port_temp, msgReply);
					} catch (Exception e) {
						e.printStackTrace();
					}
					Node.permissions[msg.getfromid()]= false;
				}
			}
			else if((msg.getType()).equals("release")){
				//set particular flag to true in permissions array
				Node.permissions[msg.getfromid()]=true;
			}
			else if((msg.getType()).equals("reply")){
				//set particular flag to true in permissions array
				Node.permissions[msg.getfromid()]=true;
			}
			else if((msg.getType()).equals("enterCS") || ((msg.getType()).equals("exitCS"))){
				if(Node.getIdentifier() == 0){
					writeCsLog(msg);
				}
			}
			else if(msg.getType().equals("done")){
				Node.doneList[msg.getfromid()]=true;
				writeCsStats(msg);
				if(doneListChecker()){
					Message finaldone = new Message("dummymsg", 0,0,0);
					writeCsStats(finaldone);
				}
			}

		writercv(msg);
		}
		//}
		}
		catch(Exception e){
			//System.out.println("EOF of client message- server restart");
			try {
				s.close();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}
		
		
	}
	
	public static boolean doneListChecker(){
		for(int i=0;i<Node.number_of_nodes;i++){
			if(Node.doneList[i]==false)
				return false;
		}
		return true;
	}
	
	private void writercv(Message msg) {
		Writer writer;
		try {
			FileOutputStream FoutStream = new FileOutputStream(
					"receivefile-" + Node.hostname + ".txt", true);
			try {
				writer = new BufferedWriter(
						new OutputStreamWriter(FoutStream, "UTF-8"));
					
				writer.append(msg.getType()+" message Hostname: " + Node.getIdentifier() + " received from: " + msg.getfromid()+" csClock value: " + msg.getsClock());
				writer.append("\n");

				writer.close();
			} catch (IOException ioe) {
				ioe.printStackTrace();
			} finally {
				FoutStream.close();
			}
		} catch (Exception e) {
		}
	}
	
	public static void writeCsLog(Message msg) {
		Writer writer;
		try {
			FileOutputStream FoutStream = new FileOutputStream(
					"CsLog_root" + ".txt", true);
			try {
				writer = new BufferedWriter(
						new OutputStreamWriter(FoutStream, "UTF-8"));
					
				writer.append("Hostname: "+ msg.getfromid()+" csClock: " + msg.getsClock()+" "+msg.getType());
				writer.append("\n");
				writer.close();
			} catch (IOException ioe) {
				ioe.printStackTrace();
			} finally {
				FoutStream.close();
			}
		} catch (Exception e) {
		}
	}
	
	public static void writeCsStats(Message msg) {
		Writer writer;
		try {
			FileOutputStream FoutStream = new FileOutputStream(
					"CsStats_root" + ".txt", true);
			try {
				writer = new BufferedWriter(
						new OutputStreamWriter(FoutStream, "UTF-8"));
				if(msg.getType().equals("done")){
					writer.append("Done from: "+ msg.getfromid()+" at csClock: " + msg.getsClock()+" having sent: "+msg.gettoid());
					writer.append("\n");
					writer.close();
				}
				else{
					writer.append("Total time: "+(System.currentTimeMillis()-Node.startTime)+"msec");
					writer.append("\n");
					writer.close();
				}
			} catch (IOException ioe) {
				ioe.printStackTrace();
			} finally {
				FoutStream.close();
			}
		} catch (Exception e) {
		}
	}

}
