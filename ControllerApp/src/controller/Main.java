package controller;

public class Main {

	public static void main(String[] args) {
		int nrNodes;
		
		while(args.length < 1 || ((nrNodes = (int)Integer.parseInt(args[0])) < 1)) {
			System.out.println("Usage:");
			System.out.println("java -jar controller.jar <number of nodes>");
			System.exit(-1);
		}

		Controller contr = new Controller(nrNodes);
		System.out.println("Controller App Started");
		System.out.println("Usage:");
		System.out.println("join <node id1> <node id2> ... <node idn>");
		System.out.println("leave <node id> <node id2> ... <node idn>");
		System.out.println(contr.getCurrentView().toString());
		//contr.sendNewView();
		
		//loop to read command line input
		while(true) {
			contr.processInput();
		}

	}
}
