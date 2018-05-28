package behaviours;

import java.util.Date;

import agents.AbstractAgent;
import agents.AbstractAgent.Topic;
import jade.core.Agent;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

public abstract class AbstractReceiveMessageBehaviour extends AbstractFSMBehaviour {

	private static final long serialVersionUID = 9145217906640473961L;

	public enum Outcome{
		NO_MESSAGE(0),			// No message received during this cycle
		NO_MORE_MESSAGE(1),		// At least one message received
		MESSAGE_RECEIVED(2);	// Message reçu

		private int value;
		private Outcome(int value) { this.value = value; }
		public int getValue() { return this.value; }
	}

	protected Outcome	outcome;
	protected Topic		topic;
	protected boolean	bUseTimeout;	// Is the message is affected by a timeout ?

	public AbstractReceiveMessageBehaviour(Agent a) {
		super(a);
		outcome = Outcome.NO_MESSAGE;
	}
	
	public abstract void handleMessage(ACLMessage msg);

	@Override
	public void action() {
		outcome = Outcome.NO_MESSAGE;

		boolean	flush = true; // Pour nettoyer la boite aux lettres des messages non valide;

		myAgent.addLogEntry("checking if I received a " + topic.getValue() + " message");

		MessageTemplate mt 	= MessageTemplate.MatchConversationId(topic.getValue());
        ACLMessage 		msg = null;

        // Bouclage sur les messages reçus tant que l'agent n'a pas reçus de messages valide (Pas trop ancien, et pas d'un agent terminé, ou alors pas de message)
        while(flush) {
        	msg = myAgent.blockingReceive(mt, AbstractAgent.WAIT_FOR_MESSAGE_DURATION);

        	if(msg == null) flush = false;
        	else {

				Date postDate			= new Date(msg.getPostTimeStamp());
				Date currentDate		= new Date();
				long elapsedTime		= currentDate.getTime() - postDate.getTime();

				myAgent.addLogEntry("received message : ");
				myAgent.addLogEntry("    message posted   : " + postDate.toString());
				myAgent.addLogEntry("    message received : " + currentDate.toString());
				myAgent.addLogEntry("    elapsed time     : " + elapsedTime);

				if((!bUseTimeout) || (elapsedTime <= AbstractAgent.MESSAGE_TIMEOUT && bUseTimeout)) {

					myAgent.incrementMessageReceived(topic);
					flush = false;
					handleMessage(msg);

					myAgent.addLogEntry("    Valid message !");
					myAgent.addLogEntry("        Topic : " + topic.getValue());
					myAgent.addLogEntry("        From  : " + msg.getSender().getLocalName());

					outcome = Outcome.MESSAGE_RECEIVED;

				}else { myAgent.addLogEntry("    Non valid message (Too old)"); }
        	}
        }
	}

	@Override
	public boolean done() {
		if(outcome != Outcome.MESSAGE_RECEIVED) {
			if(myAgent.getMessageReceived(topic) > 0) {
				outcome = Outcome.NO_MORE_MESSAGE;
				myAgent.addLogEntry("No more message received ");
			}else {
				outcome = Outcome.NO_MESSAGE;
				myAgent.addLogEntry("No message received ");
			}
		}

		switch(outcome) {
			case NO_MESSAGE:
				myAgent.trace(getBehaviourName(), false); break;
			case NO_MORE_MESSAGE:
				if(topic == Topic.DEDALE)
					myAgent.trace(getBehaviourName(), false);
				else 
					myAgent.trace(getBehaviourName(), true);
				break;
			default:
				break;
		}
		return true;
	}	
	
	@Override
	public int onEnd() {
		return outcome.getValue();
	}
}
