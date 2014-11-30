package proxy;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
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
    private static DocumentBuilder builder = null;
    private static NodeAccessLayer NAL = null;
    private static RegistryAccessLayer RAL = null;
    
    public Proxy() throws ParserConfigurationException {
        super();
        builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
    }

	protected void doPost(HttpServletRequest request, HttpServletResponse response) {
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
		} catch (SAXException | ParserConfigurationException | TransformerException | IOException e) {
			System.out.println(e);
		}
	}
	
    private Document operations(Document data, HttpSession session) throws ParserConfigurationException {  
		// Il nome del tag radice determina l'operazione da eseguire 
	 	Element root = data.getDocumentElement();
	 	System.out.println("Documento ricevuto:");
	 	StampDocument(data);
	 	System.out.println();
        String operation = root.getTagName();
        Document answer = builder.newDocument();
    	System.out.println(session.getId());
    	
        switch (operation) {
        
        	case "log-in":{
        		String username = root.getElementsByTagName("username").item(0).getTextContent();
        		String password = root.getElementsByTagName("password").item(0).getTextContent();
        		RAL = new RegistryAccessLayer();
        		if(RAL.AuthUser(username, password) == null){
        			answer.appendChild(answer.createElement("log-in fails"));
        			break;
        		}
        		session.setAttribute("username", username);
        		System.out.println("Utente: " + username + " loggato correttamente");
        		answer.appendChild(answer.createElement("logged"));
        		break;
        	}
        /*
         *  Writes Tuple in the target tuple space; 
         *  after the operation is successfully executed, Tuple is returned as a completion
         */
	        case "out": {
	        	System.out.println("Username:" + (String) session.getAttribute("username"));
	        	System.out.println("Operation: out");
	        	Element tuple_node = (Element)root.getElementsByTagName("tuple").item(0);
	        	System.out.println("Template:" + tuple_node.getAttribute("value"));
	            initNodeAccessLayer(session);
				LogicTuple tuple;
				try {
					tuple = LogicTuple.parse(tuple_node.getAttribute("value"));
				} catch (InvalidLogicTupleException e) {
					answer.appendChild(answer.createElement("problem"));
					return answer;
				}
				System.out.println("Realizzo la out");
				LogicTuple response = NAL.out("default", tuple);
	            System.out.println("Risposta:" + response.toString());
	            answer.appendChild(answer.createElement("ok"));
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
	        	session.setAttribute("status", "pending");
            	System.out.println("Username:" + (String) session.getAttribute("username"));
	        	System.out.println("Operation: rd");
	        	Element tuple_node = (Element)root.getElementsByTagName("tuple").item(0);
	        	System.out.println("Template:" + tuple_node.getAttribute("value"));
	            initNodeAccessLayer(session);
				LogicTuple template;
				try {
					template = LogicTuple.parse(tuple_node.getAttribute("value"));
				} catch (InvalidLogicTupleException e) {
					answer.appendChild(answer.createElement("problem"));
					return answer;
				}
	            LogicTuple response = NAL.rd("default", template);
	            session.setAttribute("status", "ready");
	            session.setAttribute("rd", response.toString());
	            System.out.println("Risposta:" + response.toString());
	            if(response.toString().equals(tuple_node.getAttribute("value")))
	            	answer.appendChild(answer.createElement("found"));
	            else
	            	answer.appendChild(answer.createElement("not-found"));
            	break;
        	}
	        
	        case "rd-result": {
            	if(session.getAttribute("status").equals("ready"))
            		answer.appendChild(answer.createElement(session.getAttribute("rd").toString()));
            	session.removeAttribute("rd");
            	session.setAttribute("status", "ready");
            	break;
            }
         /*
          * Predicative (non-suspensive) version of rd(TupleTemplate); 
          * if a matching Tuple is not found, the execution fails
      	  * (operation outcome is FAILURE) and TupleTemplate is returned;
          */
            case "rdp": {
	        	System.out.println("Username:" + (String) session.getAttribute("username"));
	        	System.out.println("Operation: rdp");
	        	Element tuple_node = (Element)root.getElementsByTagName("tuple").item(0);
	        	System.out.println("Template:" + tuple_node.getAttribute("value"));
	            initNodeAccessLayer(session);
				LogicTuple template;
				try {
					template = LogicTuple.parse(tuple_node.getAttribute("value"));
				} catch (InvalidLogicTupleException e) {
					answer.appendChild(answer.createElement("problem"));
					return answer;
				}
	            LogicTuple response = NAL.rdp("default", template);
	            System.out.println("Risposta:" + response.toString());
	            if(response.toString().equals(tuple_node.getAttribute("value")))
	            	answer.appendChild(answer.createElement("found"));
	            else
	            	answer.appendChild(answer.createElement("not-found"));
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
            	// Imposto lo stato del WS per questa sessione in pending
            	session.setAttribute("status", "pending");
            	System.out.println("Username:" + (String) session.getAttribute("username"));
	        	System.out.println("Operation: in");
	        	Element tuple_node = (Element)root.getElementsByTagName("tuple").item(0);
	        	System.out.println("Template:" + tuple_node.getAttribute("value"));
				LogicTuple template;
				try {
					template = LogicTuple.parse(tuple_node.getAttribute("value"));
				} catch (InvalidLogicTupleException e) {
					answer.appendChild(answer.createElement("problem"));
					return answer;
				}
				System.out.println("Risposta: I do that! :)");
				// Ritorno l'ack di inizio lavoro
	            answer.appendChild(answer.createElement("ack"));
	            
	            // Le prossime tre righe vanno fatte eseguire da un thread! ;)
	            new AsynchAgent(session, template).start();
            	break;
            }
            
            case "in-result": {
            	System.out.println("richiesta risultato in");
            	if(session.getAttribute("status").equals("ready")){
            		answer.appendChild(answer.createElement("found"));
            		session.removeAttribute("in");
            	}else
            		answer.appendChild(answer.createElement("not-found"));
            	
            	break;
            }
        /*
         * Predicative (non-suspensive) version of in(TupleTemplate); 
         * if a matching Tuple is not found, the execution fails
 	     * no tuple is removed from the target tuple space and
     	 * TupleTemplate is returned;
         */         
            case "inp": {
            	System.out.println("Username:" + (String) session.getAttribute("username"));
	        	System.out.println("Operation: inp");
	        	Element tuple_node = (Element)root.getElementsByTagName("tuple").item(0);
	        	System.out.println("Template:" + tuple_node.getAttribute("value"));
	            initNodeAccessLayer(session);
				LogicTuple template;
				try {
					template = LogicTuple.parse(tuple_node.getAttribute("value"));
				} catch (InvalidLogicTupleException e) {
					answer.appendChild(answer.createElement("problem"));
					return answer;
				}
	            LogicTuple response = NAL.rdp("default", template);
	            System.out.println("Risposta:" + response.toString());
	            if(response.toString().equals(tuple_node.getAttribute("value")))
	            	answer.appendChild(answer.createElement("found"));
	            else
	            	answer.appendChild(answer.createElement("not-found"));
            	break;
            }
    	/*
    	 * Reads all the Tuples in the target tuple space and returns
	 	 * them as a list; if no tuple occurs in the target tuple space at
 	 	 * execution time, the empty list is returned and the execution
 	 	 * succeeds anyway
    	 */ 
            case "get":{
                break;
            }
        /*
         * Overwrites the target tuple spaces with the list of
     	 * Tuples; when the execution is completed, the list of
     	 * Tuples is successfully returned
         */
            case "set":{
                break;
            }
        }
        return answer;
    }
	 
	private void initNodeAccessLayer(HttpSession session) {
		 try {
			 System.out.println("autenticazione in corso...");
			 String username = session.getAttribute("username").toString();
			 System.out.println("autenticato utente");
			 NAL = new NodeAccessLayer(username, "synchAgent");
		 } catch (TucsonInvalidAgentIdException e) {
			System.out.println(e);
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
}
