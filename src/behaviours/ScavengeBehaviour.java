package behaviours;

import jade.core.Agent;

public class ScavengeBehaviour extends AbstractFSMBehaviour {

	private static final long serialVersionUID = 4218564434332765049L;

	public ScavengeBehaviour(Agent a) {
		super(a);
	}

	@Override
	public void action() {
		String id 				= myAgent.getCurrentRoom();
		String myTreasureType	= myAgent.getTreasureType().toString();
		String treasureType 	= myAgent.getDedale().getTreasureType(id).toString();
		int treasureValue		= myAgent.getDedale().getTreasureValue(id);

		myAgent.addLogEntry("my treasure type is    : " + myTreasureType.toUpperCase());
		myAgent.addLogEntry("room treasure type is  : " + treasureType.toUpperCase());
		myAgent.addLogEntry("room treasure value is : " + treasureValue);

		if(myTreasureType.equalsIgnoreCase(treasureType)) {
			if(myAgent.getBackPackFreeSpace() > 0) {
				int value 		= myAgent.pick();
				myAgent.getDedale().scavenge(id, value);

				myAgent.addLogEntry("correct treasure type, I'm grabbing " + value);
				myAgent.addLogEntry("my backpack remaining space is   : " + myAgent.getBackPackFreeSpace());

			}else { myAgent.addLogEntry("my backpack is full"); }

		}else { myAgent.addLogEntry("incompatible room treasure type"); }
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
