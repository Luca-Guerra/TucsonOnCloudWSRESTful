package proxy;

import models.User;
import base.RegistryAccessLayer;
import alice.tucson.api.SynchACC;
import alice.tucson.api.TucsonAgentId;
import alice.tucson.api.TucsonMetaACC;
import alice.tucson.api.TucsonTupleCentreId;
import alice.tucson.api.exceptions.TucsonInvalidAgentIdException;
import alice.tucson.api.exceptions.TucsonInvalidTupleCentreIdException;

public class NodeAccessLayer {
	 private TucsonAgentId aid = null;
	 private SynchACC acc = null;
	 private RegistryAccessLayer RAL = null;
	 private final String NODE_IP = "127.0.0.1";
	 private User user = null;
	 
	 public NodeAccessLayer(String username, String password) throws TucsonInvalidAgentIdException{
		 RAL = new RegistryAccessLayer();
		 aid = new TucsonAgentId("CloudAgent");
		 acc = TucsonMetaACC.getContext(aid);
		 
		 user = RAL.AuthUser(username, password);
	 }
	 
	 public TucsonTupleCentreId GetTupleCenter(String tuple_center_name) {
		 try {
			return new TucsonTupleCentreId(tuple_center_name, NODE_IP, user.Nport);
		} catch (TucsonInvalidTupleCentreIdException e) {
			return null;
		}
	 }
}
