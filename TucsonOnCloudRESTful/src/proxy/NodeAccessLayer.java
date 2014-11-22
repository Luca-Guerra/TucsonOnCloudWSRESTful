package proxy;

import models.User;
import base.RegistryAccessLayer;
import alice.logictuple.LogicTuple;
import alice.tucson.api.ITucsonOperation;
import alice.tucson.api.SynchACC;
import alice.tucson.api.TucsonAgentId;
import alice.tucson.api.TucsonMetaACC;
import alice.tucson.api.TucsonTupleCentreId;
import alice.tucson.api.exceptions.TucsonInvalidAgentIdException;
import alice.tucson.api.exceptions.TucsonInvalidTupleCentreIdException;
import alice.tucson.api.exceptions.TucsonOperationNotPossibleException;
import alice.tucson.api.exceptions.UnreachableNodeException;
import alice.tuplecentre.api.exceptions.OperationTimeOutException;

public class NodeAccessLayer {
	 private TucsonAgentId aid = null;
	 private SynchACC acc = null;
	 private RegistryAccessLayer RAL = null;
	 private final String NODE_IP = "127.0.0.1";
	 private User user = null;
	 
	 public NodeAccessLayer(String username, String password) throws TucsonInvalidAgentIdException {
		 RAL = new RegistryAccessLayer();
		 aid = new TucsonAgentId("CloudAgent");
		 acc = TucsonMetaACC.getContext(aid);
		 
		 user = RAL.AuthUser(username, password);
	 }
	 
	 public TucsonTupleCentreId GetTupleCenter(String tuple_centre_name) {
		 try {
			return new TucsonTupleCentreId(tuple_centre_name, NODE_IP, user.Nport);
		} catch (TucsonInvalidTupleCentreIdException e) {
			return null;
		}
	 }
	 
	 public LogicTuple out(String tuple_centre_name, LogicTuple tuple) {
		// Ottengo il tuple centre ID 
		TucsonTupleCentreId tid = GetTupleCenter(tuple_centre_name);
	 	ITucsonOperation op;
		try {
			// Realizzo l'operazione out
			op = acc.out(tid, tuple, null);
		} catch (TucsonOperationNotPossibleException | 
				 UnreachableNodeException | 
				 OperationTimeOutException e) {
			return null;
		}
		return op.getLogicTupleResult();
	 }
	 
	 public LogicTuple rdp(String tuple_centre_name, LogicTuple tuple) {
		// Ottengo il tuple centre ID 
			TucsonTupleCentreId tid = GetTupleCenter(tuple_centre_name);
		 	ITucsonOperation op;
			try {
				// Realizzo l'operazione out
				op = acc.rdp(tid, tuple, null);
			} catch (TucsonOperationNotPossibleException | 
					 UnreachableNodeException | 
					 OperationTimeOutException e) {
				return null;
			}
			return op.getLogicTupleResult();
	 }
}
