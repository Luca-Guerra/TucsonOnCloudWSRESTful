package proxy;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import base.RegistryAccessLayer;
import alice.logictuple.LogicTuple;
import alice.logictuple.exceptions.InvalidLogicTupleException;
import alice.tucson.api.exceptions.TucsonInvalidAgentIdException;

@WebServlet(description = "Proxy.", urlPatterns = { "/Proxy" })
public class Proxy extends HttpServlet {
	
	private static final long serialVersionUID = 1L;
    private DocumentBuilder builder = null;
    private NodeAccessLayer NAL = null;
    private RegistryAccessLayer RAL = null;
    
    public Proxy() {
        super();
        try {
			builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
		} catch (Exception e) {
			System.out.println("Problemi creazione Document builder.");
			e.printStackTrace();
		}
    }

	protected synchronized void doPost(HttpServletRequest request, HttpServletResponse response) {
		try {
			InputStream is = request.getInputStream();
			HttpSession session = request.getSession();
	        response.setContentType("text/xml;charset=UTF-8");
	        OutputStream os = response.getOutputStream();
	        Document data = builder.parse(is);
	        is.close();
        	Document answer = operations(data, session);
        	Transformer transformer = TransformerFactory.newInstance().newTransformer();
        	transformer.transform(new DOMSource(answer), new StreamResult(os));
        	os.close();
		} catch (Exception e) {
			System.out.println("Problemi metodo doPost");
			System.out.println(e);
		}
	}
	
	protected void doGet(HttpServletRequest request, HttpServletResponse response) {
		try {
			/*
			 * Returns the current session associated with this request, 
			 * or if the request does not have a session, creates one.
			 */
			request.getSession();
	        response.setContentType("text/html");
	        OutputStream os = response.getOutputStream();
	        String answer = "<html>"
			        		+ "<body>"
			        		+ "<h2>TuCSoN Proxy</h2>"
			        		+ "<h3>Primitive gestite:</h3>"
			        		+ "<ul>"
			        		+ "<li>out</li>"
			        		+ "<li>rd</li>"
			        		+ "<li>rdp</li>"
			        		+ "<li>in</li>"
			        		+ "<li>inp</li>"
			        		+ "<li>get</li>"
			        		+ "<li>set</li>"
			        		+ "</ul>"
			        		+ "</body>"
			        		+ "</html>";
        	os.write(answer.getBytes());
        	os.close();
		} catch (Exception e) {
			System.out.println("problemi doGet");
			System.out.println(e);
		}
	}
	
    private Document operations(Document data, HttpSession session) { 
    	try {
			// Il nome del tag radice determina l'operazione da eseguire 
		 	Element root = data.getDocumentElement();
		 	Print("Documento ricevuto", session.getId());
		 	//StampDocument(data);
		 	System.out.println();
	        String operation = root.getTagName();
	        
	        // Preparo il documento di risposta
	        Document answer = builder.newDocument();
	        Element result = answer.createElement("result"); 
	        Element value = answer.createElement("value");
	    	
	    	// Controllo se (per questa sessione) è possibile richiedere altre operazioni
	    	if(!CanOperate(session, operation)) {
	    		value.setTextContent("WS: pending state");
	    		result.appendChild(value);
	    		answer.appendChild(result);
	    		return answer;
	    	}
	    	
	        switch (operation) {
	        	
	        	case "log-in":{
	        		String username = root.getElementsByTagName("username").item(0).getTextContent();
	        		String password = root.getElementsByTagName("password").item(0).getTextContent();
	        		RAL = new RegistryAccessLayer();
	        		Print("Autorizzazione in corso ...", session.getId());
	        		if(RAL.AuthUser(username, password) == null){
	        			value.setTextContent("log-in fails");
	        			break;
	        		}
	        		session.setAttribute("username", username);
	        		Print("Utente = " + username + " loggato correttamente", session.getId());
	        		value.setTextContent("logged");
	        		break;
	        	}
	        /*
	         *  Writes Tuple in the target tuple space; 
	         *  after the operation is successfully executed, Tuple is returned as a completion
	         */
		        case "out": {
		        	Element tuple_node = (Element)root.getElementsByTagName("tuple").item(0);
	            	Print("Username = " + (String) session.getAttribute("username") + "; Operation = out; Template = " + tuple_node.getTextContent() , session.getId());
	
		            InitNodeAccessLayer(session);
					LogicTuple tuple;
					try {
						tuple = LogicTuple.parse(tuple_node.getTextContent());
					} catch (InvalidLogicTupleException e) {
						value.setTextContent("Problem");
						break;
					}
					String tuple_centre = root.getAttribute("tc").toString();
					LogicTuple response = NAL.out(tuple_centre, tuple);
					Print("Risposta " + response.toString(), session.getId());
		            value.setTextContent(response.toString());
		            break;
		        }
	        /*
	         * Looks for a tuple matching TupleTemplate in the target tuple space;
	         * if a matching Tuple is found when the operation is served, the
	     	 * execution succeeds by returning Tuple; otherwise, the execution is
	 		 * suspended to be resumed and successfully completed when a matching
		 	 * Tuple will be finally found in and returned from the target tuple space
	         */
		        case "rd": {
		        	// Imposto lo stato del WS per questa sessione in sospenso
	            	session.setAttribute("status", "pending");
		        	
		        	Element tuple_node = (Element)root.getElementsByTagName("tuple").item(0);
	            	Print("Username = " + (String) session.getAttribute("username") + "; Operation = rd; Template = " + tuple_node.getTextContent() , session.getId());
	
					LogicTuple tuple;
					try {
						tuple = LogicTuple.parse(tuple_node.getTextContent());
					} catch (InvalidLogicTupleException e) {
						value.setTextContent("Problem");
						return answer;
					}
					String tuple_centre = root.getAttribute("tc").toString();
					// Ritorno l'ack di inizio lavoro
					value.setTextContent("ack");
		            
		            // AsychAgent è un thread con il compito di disaccoppiare il flusso di controllo
		            // in questo modo posso ritornare subito un ack al client
		            AsynchAgent agent = new AsynchAgent(session);
		            agent.setOperation("rd", tuple_centre, tuple);
		            agent.start();
	            	break;
	        	}
		        
		        case "rd-result": {
		        	Print("richiesta risultato in", session.getId());
	            	if(session.getAttribute("status").equals("ready")){
	            		String res = session.getAttribute("rd").toString();
	            		value.setTextContent(res);
	            		session.removeAttribute("rd");
	            	}else
	            		value.setTextContent("not-found");
	            	
	            	break;
	            }
	         /*
	          * Predicative (non-suspensive) version of rd(TupleTemplate); 
	          * if a matching Tuple is not found, the execution fails
	      	  * (operation outcome is FAILURE) and TupleTemplate is returned;
	          */
	            case "rdp": {
		        	Element tuple_node = (Element)root.getElementsByTagName("tuple").item(0);
	            	Print("Username = " + (String) session.getAttribute("username") + "; Operation = rdp; Template = " + tuple_node.getTextContent() , session.getId());
	
		            InitNodeAccessLayer(session);
					LogicTuple tuple;
					try {
						tuple = LogicTuple.parse(tuple_node.getTextContent());
					} catch (InvalidLogicTupleException e) {
						value.setTextContent("Problem");
						break;
					}
					String tuple_centre = root.getAttribute("tc").toString();
		            LogicTuple response = NAL.rdp(tuple_centre, tuple);
		            Print("Response = " + response.toString(), session.getId());
		            if(response.toString().equals(tuple_node.getTextContent()))
		            	value.setTextContent(response.toString());
		            else
		            	value.setTextContent("not-found");
	            	break;
	            }
	        /*
	         * Looks for a tuple matching TupleTemplate in the target tuple
	     	 * space; if a matching Tuple is found when the operation is served, the
	 		 * execution succeeds by removing and returning Tuple; 
	 		 * otherwise, the execution is suspended to be resumed and successfully completed when
	 		 * a matching Tuple will be finally found in, removed and returned from
	 		 * the target tuple space
	         */  
	            case "in": {
	            	// Imposto lo stato del WS per questa sessione in sospenso
	            	session.setAttribute("status", "pending");
	            	
		        	Element tuple_node = (Element)root.getElementsByTagName("tuple").item(0);
	            	Print("Username = " + (String) session.getAttribute("username") + "; Operation = in; Template = " + tuple_node.getTextContent() , session.getId());
	
					LogicTuple template;
					try {
						template = LogicTuple.parse(tuple_node.getTextContent());
					} catch (InvalidLogicTupleException e) {
						value.setTextContent("Problem");
						break;
					}
					String tuple_centre = root.getAttribute("tc").toString();
					// Ritorno l'ack di inizio lavoro
					value.setTextContent("ack");
		            
		            // AsychAgent è un thread con il compito di disaccoppiare il flusso di controllo
		            // in questo modo posso ritornare subito un ack al client
		            AsynchAgent agent = new AsynchAgent(session);
		            agent.setOperation("in", tuple_centre, template);
		            agent.start();
	            	break;
	            }
	            
	            case "in-result": {
	            	if(session.getAttribute("status").equals("ready")){
	            		String res = session.getAttribute("in").toString();
	            		value.setTextContent(res);
	            		session.removeAttribute("in");
	            		Print("Template trovato! :)", session.getId());
	            	}else{
	            		value.setTextContent("not-found");
	            		Print("Template non ancora trovato.", session.getId());
	            	}
	            	break;
	            }
	        /*
	         * Predicative (non-suspensive) version of in(TupleTemplate); 
	         * if a matching Tuple is not found, the execution fails
	 	     * no tuple is removed from the target tuple space and
	     	 * TupleTemplate is returned;
	         */         
	            case "inp": {
		        	Element tuple_node = (Element)root.getElementsByTagName("tuple").item(0);
	            	Print("Username = " + (String) session.getAttribute("username") + "; Operation = inp; Template = " + tuple_node.getTextContent() , session.getId());
	
		            InitNodeAccessLayer(session);
					LogicTuple template;
					try {
						template = LogicTuple.parse(tuple_node.getTextContent());
					} catch (InvalidLogicTupleException e) {
						value.setTextContent("Problem");
						break;
					}
					String tuple_centre = root.getAttribute("tc").toString();
		            LogicTuple response = NAL.inp(tuple_centre, template);
		            Print("Response = " + response.toString(), session.getId());
		            if(response.toString().equals(tuple_node.getAttribute("value")))
		            	value.setTextContent(response.toString());
		            else
		            	value.setTextContent("not-found");
	            	break;
	            }
	    	/*
	    	 * Reads all the Tuples in the target tuple space and returns
		 	 * them as a list; if no tuple occurs in the target tuple space at
	 	 	 * execution time, the empty list is returned and the execution
	 	 	 * succeeds anyway
	    	 */ 
	            case "get":{
	            	Print("Username = " + (String) session.getAttribute("username"), session.getId());
		        	Print("Operation = get", session.getId());
		            InitNodeAccessLayer(session);
					
					String tuple_centre = root.getAttribute("tc").toString();
		            List<LogicTuple> tuples = NAL.get(tuple_centre);
		            Element x_tuples = answer.createElement("tuples");
		            for(LogicTuple tuple:tuples) {
		            	Element x_tuple = answer.createElement("tuple");
		            	x_tuple.setTextContent(tuple.toString());
		            	x_tuples.appendChild(x_tuple);
		            }
		            Print("get done!", session.getId());
		            value.appendChild(x_tuples);
	            	break;
	            }
	        /*
	         * Overwrites the target tuple spaces with the list of
	     	 * Tuples; when the execution is completed, the list of
	     	 * Tuples is successfully returned
	         */
	            case "set":{
		        	Element tuple_node = (Element)root.getElementsByTagName("tuple").item(0);
	            	Print("Username = " + (String) session.getAttribute("username") + "; Operation = set; Template = " + tuple_node.getTextContent() , session.getId());
	
		            InitNodeAccessLayer(session);
					LogicTuple template;
					try {
						template = LogicTuple.parse(tuple_node.getTextContent());
					} catch (InvalidLogicTupleException e) {
						value.setTextContent("Problem");
						break;
					}
	            	String tuple_centre = root.getAttribute("tc").toString();
		            List<LogicTuple> tuples = NAL.set(tuple_centre, template);
		            Element x_tuples = answer.createElement("tuples");
		            for(LogicTuple tuple:tuples){
		            	Element x_tuple = answer.createElement("tuple");
		            	x_tuple.setTextContent(tuple.toString());
		            	x_tuples.appendChild(x_tuple);
		            }
		            Print("set done!", session.getId());
		            value.appendChild(x_tuples);
	            	break;
	            }
	        }
	        result.appendChild(value);
			answer.appendChild(result);
	        return answer;
    	}catch(Exception e){
    		System.out.println("Problemi metodo operations");
    	}
    	return null;
    }
	 
	private void InitNodeAccessLayer(HttpSession session) {
		 try {
			 Print("autenticazione in corso...", session.getId());
			 String username = session.getAttribute("username").toString();
			 Print("autenticato utente", session.getId());
			 // Il nuovo agente tucson ha un nome univoco per sessione
			 NAL = new NodeAccessLayer(username);
		 } catch (TucsonInvalidAgentIdException e) {
			Print(e.getMessage(), session.getId());
		 }
	 }
	
	private void StampDocument(Document doc){
		StreamResult result =  new StreamResult(System.out);
        Transformer transformer;
		try {
			transformer = TransformerFactory.newInstance().newTransformer();
			transformer.transform(new DOMSource(doc), result);
		} catch (TransformerException | TransformerFactoryConfigurationError e) {
			e.printStackTrace();
		}
	}
	
	private boolean CanOperate(HttpSession session, String operation) {
		// Se lo stato non è presente possiamo operare
		if(session.getAttribute("status") == null)
			return true;
		
		// Se è presente uno stato e questo non è pending possiamo operare
		if(!session.getAttribute("status").equals("pending"))
			return true;
		
		// Se è presente uno stato questo è pending ma le operazioni sono richiesta risultato 
		// di operazioni bloccanti possiamo operare
		if(operation.equals("in-result") || operation.equals("rd-result"))
			return true;
		
		return false;
	}
	
	private void Print(String msg, String session_id){
		System.out.println(session_id + ": " + msg);
	}
}
