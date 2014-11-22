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
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import alice.tucson.api.SynchACC;
import alice.tucson.api.TucsonAgentId;
import alice.tucson.api.TucsonMetaACC;
import alice.tucson.api.exceptions.TucsonInvalidAgentIdException;
import base.RegistryAccessLayer;

@WebServlet(description = "Esegue le primitive Tucson per i diversi client ai giusti nodi.", urlPatterns = { "/Proxy" })
public class Proxy extends HttpServlet {
	private static final long serialVersionUID = 1L;
    private static DocumentBuilder builder = null;
    private static RegistryAccessLayer RAL = null;
    
    public Proxy() throws ParserConfigurationException, TucsonInvalidAgentIdException {
        super();
        builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        RAL = new RegistryAccessLayer();
    }

	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		InputStream is = request.getInputStream();
        HttpSession session = request.getSession();
        response.setContentType("text/xml;charset=UTF-8");
        OutputStream os = response.getOutputStream();
        try {
        	Document data = builder.parse(is);
        	is.close();
        	Document answer = operations(data,session);
        	Transformer transformer = TransformerFactory.newInstance().newTransformer();
        	transformer.transform(new DOMSource(answer), new StreamResult(os));
        	os.close();
        }
        catch (Exception ex){ 
            System.out.println(ex);
        }
	}
	
	 private Document operations(Document data, HttpSession session) throws ParserConfigurationException {  
		// Il nome del tag radice determina l'operazione da eseguire 
	 	Element root = data.getDocumentElement();
        String operation = root.getTagName();
        Document answer = builder.newDocument();
        switch (operation) {
        /*
         *  Writes Tuple in the target tuple space; 
         *  after the operation is successfully executed, Tuple is returned as a completion
         */
            case "out":
            	
                System.out.println("Inserisco la tupla" + "tupla con valore:" + "valore");
                
                answer.appendChild(answer.createElement("ok"));
                break;
        /*
         * Looks for a tuple matching TupleTemplate in the target tuple space;
         * if a matching Tuple is found when the operation is served, the
     	 * execution succeeds by returning Tuple; otherwise, the execution is
 		 * suspended to be resumed and successfully completed when a matching
	 	 * Tuple will be finally found in and returned from the target tuple space
         */
            case "rd":
                break;
         /*
          * Predicative (non-suspensive) version of rd(TupleTemplate); 
          * if a matching Tuple is not found, the execution fails
      	  * (operation outcome is FAILURE) and TupleTemplate is returned;
          */
            case "rdp":
            	break;
        /*
         * Looks for a tuple matching TupleTemplate in the target tuple
     	 * space; if a matching Tuple is found when the operation is served, the
 		 * execution succeeds by removing and returning Tuple; 
 		 * otherwise, the execution is suspended to be resumed and successfully completed when
 		 * a matching Tuple will be finally found in, removed and returned from
 		 * the target tuple space
         */  
            case "in":
                break;
        /*
         * Predicative (non-suspensive) version of in(TupleTemplate); 
         * if a matching Tuple is not found, the execution fails
 	     * no tuple is removed from the target tuple space and
     	 * TupleTemplate is returned;
         */         
            case "inp":
            	break;
    	/*
    	 * Reads all the Tuples in the target tuple space and returns
	 	 * them as a list; if no tuple occurs in the target tuple space at
 	 	 * execution time, the empty list is returned and the execution
 	 	 * succeeds anyway
    	 */ 
            case "get":
                //
                break;
        /*
         * Overwrites the target tuple spaces with the list of
     	 * Tuples; when the execution is completed, the list of
     	 * Tuples is successfully returned
         */
            case "set":
                //
                break;
        }
        return answer;
	    }
}
