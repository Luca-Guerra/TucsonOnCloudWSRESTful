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

@WebServlet(description = "Esegue le primitive Tucson per i diversi client ai giusti nodi.", urlPatterns = { "/Proxy" })
public class Proxy extends HttpServlet {
	private static final long serialVersionUID = 1L;
    private static DocumentBuilder builder = null;
    
    public Proxy() throws ParserConfigurationException {
        super();
        builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
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
	        //name of operation is message root
	        Element root = data.getDocumentElement();
	        String operation = root.getTagName();
	        Document answer = builder.newDocument();
	        switch (operation) {
	            case "op1":
	                System.out.println("Eseguo l'operazione prova");
	                answer.appendChild(answer.createElement("ok"));
	                break;
	            case "op2":
	                //
	                break;
	            // case ....
	        }
	        return answer;
	    }
}
