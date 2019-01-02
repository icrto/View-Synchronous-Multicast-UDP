package node;

import java.io.IOException;

public class Main {

	public static void main(String[] args) throws IOException {
		//Group g = new Group();
		
		VSM vsm = new VSM(1, "228.0.0.4", 6790, 0, 0, 0);
		vsm.start();
		
		vsm.sendVSM("Teste");
		vsm.sendVSM("Teste");
		vsm.sendVSM("Teste");
		vsm.sendVSM("Teste");
		
		System.out.println(vsm.recvVSM());
		System.out.println(vsm.recvVSM());
		System.out.println(vsm.recvVSM());
		System.out.println(vsm.recvVSM());

	}

}
