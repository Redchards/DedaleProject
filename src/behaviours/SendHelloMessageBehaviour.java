package behaviours;

import agents.AbstractAgent.Topic;
import jade.core.AID;
import jade.core.Agent;
import jade.lang.acl.ACLMessage;

public class SendHelloMessageBehaviour extends AbstractFSMBehaviour {

	private static final long serialVersionUID = 6055101536491319665L;

	public SendHelloMessageBehaviour(Agent a) {
		super(a);
	}

	@Override
	public void action() {
		String topic = Topic.HELLO.getValue();

		this.myAgent.addLogEntry("sending ' " + topic + "' message");

		ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
		msg.setSender(myAgent.getAID());
		msg.setConversationId(topic);
		msg.setContent(myAgent.getType().toString() + ";" +myAgent.getCurrentRoom());

		myAgent.addLogEntry("Receiver :");
		for (AID aid:myAgent.getAgentsAID(null)) {
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
