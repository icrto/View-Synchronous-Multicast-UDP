package node;

import java.util.ArrayList;

public class VSM {

	private ArrayList<VSMMessage> msgBuffer;
	
	private boolean changingView = false;
	
	private View currentView;
	
	private Group group;
	
	public void sendVSC() {
		updateView();
		while(changingView);
		
		
		/* TODO: 
		 * 2º - Enviar mensagem em multicast udp
		 * 3º - Correr recv para esta mensagem devido à propriedade de self-delivery
		*/
		
	}
	
	public String recvVSC() {
		
		/* TODO:
		 * 1 - retornar msg presente em buffer de stable msgs ou esperar até ter alguma coisa
		*/
		
		return null;
	}
	
	private void updateView() {
		View retrievedView = group.retrieveCurrentView();
		if(currentView.equals(retrievedView)) return;
		else {
			changingView = false;
			// BIG TODO: Mudar vista !!!!!!!!!!!!
		}
	}
}
