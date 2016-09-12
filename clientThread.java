

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.ConnectException;
import java.net.Socket;

public class clientThread{
	
	private int idnode;
	private String hostnode;
	private int portnode;
	private Socket soc_Client = null;
	
	public clientThread(int idnode, String hostnode, int portnode ){
		this.idnode = idnode;
		this.hostnode = hostnode;
		this.portnode = portnode;
	}

	
	public void connect(String h, int port, Message msg) throws Exception{

		//while (true) {
			try {
				Node.NodeLogger.info("sent from: "+Node.hostname+" to "+h );
				soc_Client = new Socket(h, port);
				ObjectOutputStream output = new ObjectOutputStream(soc_Client.getOutputStream());
				System.out.println("sent from: "+Node.hostname+" to "+h);
				output.writeObject(msg);
				output.flush();
				soc_Client.close();
				writesend(msg);

				//break;
			} catch (ConnectException e) {
				Node.NodeLogger.info("Reconnect again");
				System.out.println("Server " + h + " is not responding");
	            System.out.println("Trying again after 1 sec");
	            long start = System.currentTimeMillis();
	            long end = start +  1000;
	            while (System.currentTimeMillis() < end) {
	            }
	            this.connect(h, port,msg);

			}
	}
	private void writesend(Message msg) {
		Writer writer;
		try {
			FileOutputStream FoutStream = new FileOutputStream(
					"sendfile-" + Node.hostname + ".txt", true);
			try {
				writer = new BufferedWriter(
						new OutputStreamWriter(FoutStream, "UTF-8"));
					
				writer.append(msg.getType()+" message Hostname: " + Node.getIdentifier() + " sent to: " + msg.gettoid());
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

}
