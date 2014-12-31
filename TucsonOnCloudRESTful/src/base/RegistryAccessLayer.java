package base;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import models.User;

public class RegistryAccessLayer {
	private String registryPath = "C:\\Users\\Luca\\Desktop\\Registry.xml";
	
	public int activeAccounts() {
		List<User> au = getUsers();
		return au == null ? 0 : au.size();
    }
		
	public List<User> getUsers(){
		List<User> users = new ArrayList<User>();
		Document doc = GetRegistry();
		Element root = doc.getDocumentElement();
		NodeList accounts = root.getElementsByTagName("account");
		for(int i=0; accounts.item(i) != null; i++){
			User u = new User();
			NodeList children = accounts.item(i).getChildNodes();
			u.Username 	= children.item(0).getTextContent();
			u.Password 	= children.item(1).getTextContent();
			u.Nport 	= children.item(2).getTextContent();
			users.add(u);
		}
    	return users;
    }
	
	public boolean addNewUser(String username, String password) {
		Document doc = GetRegistry();
		Element el = doc.getDocumentElement();
		// Creo il nodo account
		Element account = doc.createElement("account");
		Element u = doc.createElement("username");
		u.setTextContent(username);
		Element p = doc.createElement("password");
		Element port = doc.createElement("port");
		port.setTextContent("NP");
		p.setTextContent(password);
		account.appendChild(u);
		account.appendChild(p);
		account.appendChild(port);
		el.appendChild(account);
		try {
			// Creo il transformer per XML
			Transformer transformer = TransformerFactory.newInstance().newTransformer();
			File f = new File(registryPath);
			StreamResult sr = new StreamResult(f);
	    	transformer.transform(new DOMSource(doc), sr);
		} catch (TransformerException e) {
			return false;
		}
    	return true;
	  }
	
	public User AuthUser(String username, String password)
	{
		List<User> users = getUsers();
		for(User user : users)
			if(user.Username.equals(username) && user.Password.equals(password))
				return user;
		
		return null;
	}
 
	public User GetUser(String username){
		List<User> users = getUsers();
		for(User user : users)
			if(user.Username.equals(username))
				return user;
		return null;
	}
	
	public boolean AttachPort(String port){
		// Ottengo il registro
		Document doc = GetRegistry();
		Element root = doc.getDocumentElement();
		// Prendo l'insieme di tutti i nodi account
		NodeList accounts = root.getElementsByTagName("account");
		// Cerco l'account senza porta assegnata
		for(int i=0; accounts.item(i) != null; i++){
			NodeList account = accounts.item(i).getChildNodes();
			if(account.item(2).getTextContent().equals("NP")){
				account.item(2).setTextContent(port);
				System.out.println("Assegnata");
				Transformer transformer;
				try {
					transformer = TransformerFactory.newInstance().newTransformer();
					transformer.transform(new DOMSource(doc), new StreamResult(new File(registryPath)));
				} catch (TransformerFactoryConfigurationError | TransformerException e) {
					e.printStackTrace();
				} 	
				return true;
			}
		}
		return false;
	}
	
	// Metodi privati di utilità per la classe
	
	private synchronized Document GetRegistry(){
		// Ricavo il file XML del registro
		File file = new File(registryPath);
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db;
		try {
			db = dbf.newDocumentBuilder();
		} catch (ParserConfigurationException e) {
			// Nel caso in cui ci siano problemi con la creazione del builder devo ritornare null ERRORE GRAVE
			return null;
		}
		try {
			return db.parse(file);
		} catch (SAXException | IOException e) {
			System.out.println("Eccezzione nel parsing del documento");
			/*
			 *  Se mal formattato ritorno un documento nuovo 
			 *  (questo approccio è problematico nel caso in cui siano già presenti dei nodi TuCSoN attivi)
			 */
			return null;
		}
	}
}