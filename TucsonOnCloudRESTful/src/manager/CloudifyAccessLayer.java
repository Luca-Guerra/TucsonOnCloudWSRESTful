package manager;

import java.io.IOException;
import java.io.InputStream;

import base.RegistryAccessLayer;

public class CloudifyAccessLayer {
	
    private String batchPath = null;
    private String cloudAddress = null;
    private RegistryAccessLayer RAL = null;
    
    public CloudifyAccessLayer(String bathPath, String cloudAddress) {
    	this.batchPath = bathPath;
    	this.cloudAddress = cloudAddress;
    	RAL = new RegistryAccessLayer();
    }
    
	public String newNode(String username, String password) throws IOException {
	    System.out.println("I'm starting the service and creating a Node for:" + username);
	    boolean result = false;
	    boolean first_node = (RAL.activeAccounts() == 0);
	    if(!RAL.addNewUser(username, password))
    		return "Problems";
	    if(first_node) 
			result = execCmd("connect " + cloudAddress + ";use-application TuCSoNCloud;install-service C:/cloudify/gigaspaces-cloudify-2.7.0-ga/recipes/apps/TuCSoNCloud/Node");
	    else {
			if(RAL.getUsers().contains(username))
				return "Username already in use";
			int c = RAL.activeAccounts();
			result = execCmd("connect " + cloudAddress + ";use-application TuCSoNCloud;set-instances Node " + c);
		}
	    if(result)
	    	return "OK";
	    return "Problems";	
	}
 	
	private boolean execCmd(String cmd) {
		  int result = 0;
		  try {
				Process proc = Runtime.getRuntime().exec("cmd /c "+ batchPath + " \""+cmd+"\"");
				InputStream in = proc.getInputStream();
			    int c;
			    while ((c = in.read()) != -1) {
			      System.out.print((char) c);
			    }
			    in.close();
			    result = proc.waitFor();
		  } catch (InterruptedException | IOException e) { result = -1;}
		  if(result == 0)
			  return true;
		  return false;
	  }

}
