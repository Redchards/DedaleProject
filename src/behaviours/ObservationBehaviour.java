package behaviours;

import java.util.List;

import env.Attribute;
import env.Couple;
import jade.core.Agent;
import knowledge.Dedale;

public class ObservationBehaviour extends AbstractFSMBehaviour {

	private static final long serialVersionUID = -161418389634735439L;

	public enum Outcome{
		EMPTY_ROOM(0),
		TREASURE_ROOM(1);

		private int value;
		private Outcome(int value) { this.value = value; }
		public int getValue() { return this.value; }
	}

	private Outcome	outcome; 

	public ObservationBehaviour(Agent a) {
		super(a);
		outcome = Outcome.EMPTY_ROOM;
	}

	@Override
	public void action() {

		String myPosition	= myAgent.getCurrentPosition();
		Dedale dedale 		= myAgent.getDedale();
		
		myAgent.newCycle();

		myAgent.addLogEntry("cycle        : " + myAgent.getCycleCounter());
		myAgent.addLogEntry("observing at : " + myPosition);
		
		if (myPosition.isEmpty()){ throw new IllegalArgumentException("Position is empty"); }

		List<Couple<String,List<Attribute>>> obs = myAgent.observe();

		if(dedale.integrateObservation(myPosition, obs)) {
			myAgent.addLogEntry("this room is a treasure room !");
			outcome = Outcome.TREASURE_ROOM;
		}else{
			myAgent.addLogEntry("this room is empty") ;
			outcome = Outcome.EMPTY_ROOM;
		}
	}

	@Override
	public boolean done() {
		myAgent.trace(getBehaviourName(), true);
		return true;
	}

	@Override
	public int onEnd() {
		return outcome.getValue();
	}
}
