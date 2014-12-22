package proxy;

import java.util.List;

import models.User;
import base.RegistryAccessLayer;
import alice.logictuple.LogicTuple;
import alice.tucson.api.SynchACC;
import alice.tucson.api.TucsonAgentId;
import alice.tucson.api.TucsonMetaACC;
import alice.tucson.api.TucsonTupleCentreId;
import alice.tucson.api.exceptions.TucsonInvalidAgentIdException;
import alice.tucson.api.exceptions.TucsonInvalidTupleCentreIdException;
import alice.tucson.api.exceptions.TucsonOperationNotPossibleException;
import alice.tucson.api.exceptions.UnreachableNodeException;
import alice.tuplecentre.api.exceptions.OperationTimeOutException;

// Tramite questa classe rendiamo trasparente 
// l'accesso al nodo associato al registro.
public class NodeAccessLayer {
	
	// La classe deve rimanere statica, in questo modo avremo sempre e solo un unica istanza 
	// dell'ACC, in caso contrario più istanze di acc con stesso aid manderebbero in stallo
	// il nodo.
	static public class WsACC
	{
		static private TucsonAgentId aid = null;
		static private SynchACC acc = null;
		static SynchACC GetACC(){
			if(acc == null)
			{
				try {
					aid = new TucsonAgentId("wsAgent");
				} catch (TucsonInvalidAgentIdException e) {
					e.printStackTrace();
				}
			    acc = TucsonMetaACC.getContext(aid);
			}
			return acc;
		}
	}
	
	private RegistryAccessLayer RAL = null;
	private final String NODE_IP = "localhost";
	private User user = null;
	 
	public NodeAccessLayer(String username) throws TucsonInvalidAgentIdException {
		try {
			RAL = new RegistryAccessLayer();
			user = RAL.GetUser(username);
		} catch(Exception e) {
			System.out.println(e);
		}
	}
	 
	 public List<LogicTuple> get(String tuple_centre_name){
		 TucsonTupleCentreId tid = getTupleCenter(tuple_centre_name);
		 try {
			return WsACC.GetACC().get(tid, null).getLogicTupleListResult();
		} catch (TucsonOperationNotPossibleException | UnreachableNodeException
				| OperationTimeOutException e) {
			return null;
		}
	 }
	 
	 public List<LogicTuple> set(String tuple_centre_name, LogicTuple tuple){
		 TucsonTupleCentreId tid = getTupleCenter(tuple_centre_name);
		 try {
			return WsACC.GetACC().set(tid, tuple, null).getLogicTupleListResult();
		} catch (TucsonOperationNotPossibleException | UnreachableNodeException
				| OperationTimeOutException e) {
			return null;
		}
	 }
	 
	 public LogicTuple out(String tuple_centre_name, LogicTuple tuple) {
		 return exec("out", tuple_centre_name, tuple);
	 }
	 
	 public LogicTuple rdp(String tuple_centre_name, LogicTuple tuple) {
		 return exec("rdp", tuple_centre_name, tuple);
	 }
	 
	 public LogicTuple rd(String tuple_centre_name, LogicTuple tuple) {
		 return exec("rd", tuple_centre_name, tuple);
	 }
	 
	 public LogicTuple inp(String tuple_centre_name, LogicTuple tuple) {
		 return exec("inp", tuple_centre_name, tuple);
	 }
	 
	 public LogicTuple in(String tuple_centre_name, LogicTuple tuple) {
			return exec("in", tuple_centre_name, tuple);
	 }
	 
	 private TucsonTupleCentreId getTupleCenter(String tuple_centre_name) {
		 try {
			return new TucsonTupleCentreId(tuple_centre_name, NODE_IP, user.Nport);
		} catch (TucsonInvalidTupleCentreIdException e) {
			return null;
		}
	 }
 
	 private LogicTuple exec(String op, String tuple_centre_name, LogicTuple tuple){
		// Ottengo il tuple centre ID 
		TucsonTupleCentreId tid = getTupleCenter(tuple_centre_name);
	 	try {
			switch(op) {
				case "out": { return WsACC.GetACC().out(tid, tuple, null).getLogicTupleResult(); }
				case "in":  { return WsACC.GetACC().in(tid, tuple, null).getLogicTupleResult();  }
				case "inp": { return WsACC.GetACC().inp(tid, tuple, null).getLogicTupleResult(); }
				case "rd":  { return WsACC.GetACC().rd(tid, tuple, null).getLogicTupleResult();  }
				case "rdp": { return WsACC.GetACC().rdp(tid, tuple, null).getLogicTupleResult(); }
				default:	{ return null; }
			}
		} catch (TucsonOperationNotPossibleException | 
				 UnreachableNodeException | 
				 OperationTimeOutException e) {
			return null;
		}
	 }
}
