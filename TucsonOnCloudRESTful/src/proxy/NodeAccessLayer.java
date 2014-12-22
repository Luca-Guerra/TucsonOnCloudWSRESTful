package proxy;

import java.util.List;
import java.util.Random;
import java.util.UUID;

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
	 private TucsonAgentId aid = null;
	 private SynchACC acc = null;
	 private RegistryAccessLayer RAL = null;
	 private final String NODE_IP = "localhost";
	 private User user = null;
	 
	 public NodeAccessLayer(String username) throws TucsonInvalidAgentIdException {
		 try {
			 RAL = new RegistryAccessLayer();
			 UUID uuid = UUID.randomUUID();
			 aid = new TucsonAgentId("agent" + uuid);
			 acc = TucsonMetaACC.getContext(aid);
			 user = RAL.GetUser(username);
		 } catch(Exception e) {
			 System.out.println(e);
		 }
	 }
	 
	 public List<LogicTuple> get(String tuple_centre_name){
		 TucsonTupleCentreId tid = getTupleCenter(tuple_centre_name);
		 try {
			return acc.get(tid, null).getLogicTupleListResult();
		} catch (TucsonOperationNotPossibleException | UnreachableNodeException
				| OperationTimeOutException e) {
			return null;
		}
	 }
	 
	 public List<LogicTuple> set(String tuple_centre_name, LogicTuple tuple){
		 TucsonTupleCentreId tid = getTupleCenter(tuple_centre_name);
		 try {
			return acc.set(tid, tuple, null).getLogicTupleListResult();
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
				case "out": { return acc.out(tid, tuple, null).getLogicTupleResult(); }
				case "in":  { return acc.in(tid, tuple, null).getLogicTupleResult();  }
				case "inp": { return acc.inp(tid, tuple, null).getLogicTupleResult(); }
				case "rd":  { return acc.rd(tid, tuple, null).getLogicTupleResult();  }
				case "rdp": { return acc.rdp(tid, tuple, null).getLogicTupleResult(); }
				default:	{ return null; }
			}
		} catch (TucsonOperationNotPossibleException | 
				 UnreachableNodeException | 
				 OperationTimeOutException e) {
			return null;
		}
	 }
}
