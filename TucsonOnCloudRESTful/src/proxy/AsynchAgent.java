package proxy;

import javax.servlet.http.HttpSession;

import alice.logictuple.LogicTuple;
import alice.tucson.api.exceptions.TucsonInvalidAgentIdException;

// Il compito di questa classe è quello di realizzare le primitive bloccanti
// creando un nuovo flusso di controllo.
public class AsynchAgent extends Thread {
	
    private NodeAccessLayer NAL;
    HttpSession session;
    LogicTuple template;
    private String operation;
    private String tuple_center;
    
	public AsynchAgent(HttpSession session) {
		try {
			 this.session = session;
			 System.out.println("Agente: autenticazione in corso...");
			 String username = session.getAttribute("username").toString();
			 System.out.println("Agente: autenticato utente");
			 NAL = new NodeAccessLayer(username, "asychAgent");
		 } catch (TucsonInvalidAgentIdException e) {
			System.out.println(e);
		 }
	}
	
	// Setto l'operazione che il thread dovrà eseguire al run
	public void setOperation(String op, String tc, LogicTuple t){
		operation = op;
		tuple_center = tc;
		template = t;
	}
	
	@Override
	public void run() {
		LogicTuple response;
		System.out.println("Agente: eseguo l'operazione: " + operation);
        // Eseguo l'operazione pre-impostata
		switch(operation){
			case "in": { 
				response = NAL.in(tuple_center, template);
				setResponse(response);
				break; 
				}
			case "rd": { 
				response = NAL.rd(tuple_center, template);
				setResponse(response);
				break; 
				}
			default : {
				System.out.println("Agente: non è presente una primitiva valida.");
				break;
				}
		}
        	
	}

	private void setResponse(LogicTuple response) {
		System.out.println("Agente: risposta:" + response.toString());
		// Salvo il risultato dell'operazione in sessione
        session.setAttribute(operation, response.toString());	
        // Imposto lo stato della sessione a ready, quindi è possibile leggere il risultato
        // NB: deve essere posto dopo il set del risultato per evitare problemi di concorrenza sulla sessione
 		session.setAttribute("status", "ready");
	}
	
}
