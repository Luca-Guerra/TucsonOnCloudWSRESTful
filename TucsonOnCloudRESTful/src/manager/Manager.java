package manager;

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
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import base.RegistryAccessLayer;

@WebServlet(description = "Gestore dei nodi Tucson", urlPatterns = { "/Manager" })
public class Manager extends HttpServlet {
	
	private static final long serialVersionUID = 1L;
    private static DocumentBuilder builder = null;
    private static Transformer transformer = null;
    private static CloudifyAccessLayer CAL = null;
    private RegistryAccessLayer RAL = null;
    
    public Manager() throws ParserConfigurationException, TransformerConfigurationException, TransformerFactoryConfigurationError {
        super();
        builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        transformer = TransformerFactory.newInstance().newTransformer();
        CAL = new CloudifyAccessLayer("C:\\cloudify\\gigaspaces-cloudify-2.7.0-ga\\bin\\cloudify.bat", "127.0.0.1");
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
        	transformer.transform(new DOMSource(answer), new StreamResult(os));
        	os.close();
        }
        catch (Exception ex){ 
            System.out.println(ex);
        }
	}
	
	private Document operations(Document data, HttpSession session) throws ParserConfigurationException, IOException {
	        Element root = data.getDocumentElement();
	        String operation = root.getTagName();
	        Document answer = builder.newDocument();
	        switch (operation) {
        		// Crea un nuovo nodo per l'utente richiesto
	            case "new-node": {
	            	System.out.println("OP: new-node");
	                String username = root.getElementsByTagName("username").item(0).getTextContent();
	                String password = root.getElementsByTagName("password").item(0).getTextContent();
	                String result = CAL.newNode(username, password);
	                Element node = answer.createElement(result);
	                answer.appendChild(node);
	                break;
	            }
	            // Cloudify richiama questa operazione appena individuata la porta in cui bindare un nuovo nodo TuCSoN
	            case "take-port": {
	            	String port = root.getAttribute("port").toString();
	            	System.out.println("Nuova porta consegnata");
	            	boolean result = RAL.AttachPort(port);
	            	Element response = null;
	            	if(result)
	            		response = answer.createElement("ok");
	            	else
	            		response = answer.createElement("error");
	                answer.appendChild(response);
	            	break;
	            }
	            // Test per controllare lo stato del servizio
	            case "service-test": {
	            	Element ack = answer.createElement("ack");
	                answer.appendChild(ack);
	                break;
	            }
	        }
	        return answer;
    }
}
