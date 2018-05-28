package behaviours;

import java.util.Map;

import jade.core.Agent;

public class EmptyBackpackBehaviour extends AbstractFSMBehaviour {

	private static final long serialVersionUID = 4255285995220916005L;

	public EmptyBackpackBehaviour(Agent a) {
		super(a);
	}

	@Override
	public void action() {
		int remainingFreeSpace = myAgent.getBackPackFreeSpace();
		int backpackCapacity   = myAgent.getBackpackCapacity();

		myAgent.addLogEntry("my backpack capacity is             :" + backpackCapacity);
		myAgent.addLogEntry("my backpack remaining free space is :" + remainingFreeSpace);

		if(!myAgent.isBackpackEmpty()) {
			this.myAgent.addLogEntry("I should empty my backpack");
			Map<String, String> tankers = myAgent.getKnownNearTankers();

			if(!tankers.isEmpty()) {
				for(Map.Entry<String, String> tanker:tankers.entrySet()) {
					String name	= tanker.getKey();
					String pos 	= tanker.getValue();

					if(myAgent.isNextToMe(pos)) {
						myAgent.addLogEntry("    tanker nearby at : " + pos);
						myAgent.addLogEntry("    trying to empty backpack to " + name);
					}

					Boolean res = myAgent.emptyMyBackPack(name);
					if(res) { this.myAgent.addLogEntry("    successfully emptied my backpack"); }
					else { this.myAgent.addLogEntry("    couldn't emptied my backpack"); }
				}
			}else { this.myAgent.addLogEntry("no tanker nearby"); }
		}
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
