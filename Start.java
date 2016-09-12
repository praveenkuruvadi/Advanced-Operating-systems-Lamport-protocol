
public class Start {
	
	private int id;
	private String configPath;
	
	public Start(int id, String configPath){
		this.id = id;
		this.configPath = configPath;
	}

	private void startNode() {
		Node n = new Node( this.id, this.configPath);
		Thread t = new Thread(n);
		t.start();
	}
	
	public static void main(String[] args){
		int no_of_node = Integer.parseInt(args[0]);
		String config_path = args[1];
		Start start = new Start(no_of_node, config_path);
		start.startNode();
	}

}
