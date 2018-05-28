package behaviours;

import java.io.IOException;
import java.io.Serializable;
import java.util.Set;

import agents.AbstractAgent.Topic;
import jade.core.AID;
import jade.core.Agent;
import jade.lang.acl.ACLMessage;

public class SendDedaleMessageBehaviour extends AbstractFSMBehaviour {

	private static final long serialVersionUID = -1021535900682401009L;

	public SendDedaleMessageBehaviour(Agent a) {
		super(a);
	}

	@Override
	public void action() {

		Set<AID> receiver = myAgent.getSendingMapToAgents();
		if( receiver.isEmpty() ) {
			myAgent.addLogEntry("no near agents to share with");
			return;
		}

		String topic = Topic.DEDALE.getValue();
		myAgent.addLogEntry("sending ' " + topic + "' message");

		ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
		msg.setSender(myAgent.getAID());
		msg.setConversationId(topic);

		try { msg.setContentObject((Serializable) myAgent.getDedale());}
		catch (IOException e) { e.printStackTrace(); }

		myAgent.addLogEntry("Receiver :");
		for (AID aid:receiver) {
			msg.addReceiver(aid); 
			this.myAgent.addLogEntry("    " + aid.getLocalName());
		}

		myAgent.sendMessage(msg);
	}

	@Override
	public boolean done() {
		myAgent.trace(getBehaviourName(), false);
		return true;
	}

	@Override
	public int onEnd() {
		return 0;
	}
}
