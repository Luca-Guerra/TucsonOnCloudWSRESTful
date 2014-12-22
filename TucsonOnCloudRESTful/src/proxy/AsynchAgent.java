package proxy;

import java.util.Random;

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
			 Print("autenticazione in corso...", session.getId());
			 String username = session.getAttribute("username").toString();
			 Print("autenticato utente", session.getId());
			 NAL = new NodeAccessLayer(username);
		 } catch (TucsonInvalidAgentIdException e) {
			Print(e.getMessage(), session.getId());
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
		Print("eseguo l'operazione: " + operation, session.getId());
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
				Print("non è presente una primitiva valida.", session.getId());
				break;
				}
		}
        	
	}

	private void setResponse(LogicTuple response) {
		Print("risposta:" + response.toString(), session.getId());
		// Salvo il risultato dell'operazione in sessione
        session.setAttribute(operation, response.toString());	
        // Imposto lo stato della sessione a ready, quindi è possibile leggere il risultato
        // NB: deve essere posto dopo il set del risultato per evitare problemi di concorrenza sulla sessione
 		session.setAttribute("status", "ready");
	}
	
	private void Print(String msg, String session_id){
		System.out.println("Agent(" + session_id + "): " + msg);
	}
	
}
