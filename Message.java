

import java.io.Serializable;

public class Message implements Serializable{
    private String type;
    private int toid;
    private int fromid;
    private int sClock;
    
    public Message(String type,int sClock, int toid,int fromid){
    	this.type = type;
    	this.sClock = sClock;
    	this.toid = toid;
    	this.fromid = fromid;
    }
    
    public String getType(){
    	return type;
    }

    
    public int gettoid(){
    	return toid;
    }

    public int getfromid(){
    	return fromid;
    }
    public int getsClock(){
    	return sClock;
    }
}
