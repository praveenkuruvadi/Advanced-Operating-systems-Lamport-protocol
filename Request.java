
public class Request {
	public volatile int nodeId;
	public volatile int sclock=0;
	
	// send request, only once
	public Request(int nodeId){
		this.nodeId= nodeId;
	}
	
	
	// received request
	public Request(int nodeId, int sclock){
		this.nodeId= nodeId;
		this.sclock = sclock;
	}

}
