package node;

public class VSM {
	
	public void sendVSC() {
		
		/* TODO: 
		 * 1º - Espera até terminar mudança de vista caso esteja a meio da mesma
		 * 2º - Enviar mensagem em multicast udp
		 * 3º - Correr recv para esta mensagem devido à propriedade de self-delivery
		*/
		
	}
	
	public String recvVSC() {
		
		/* TODO:
		 * 1º - Chamar receive da Network Emulation - é bloqueante até receber alguma coisa
		 * 2º - Enviar ack relativo a esta mensagem em multicast UDP
		 * 3º - Esperar
		*/
		
		return null;
	}
}
