package behaviours;

import java.util.Stack;

import agents.AbstractAgent.Strategy;
import env.EntityType;
import jade.core.Agent;

public class PlanningBehaviour extends AbstractFSMBehaviour {

	private static final long serialVersionUID = -8659063277931099247L;

	public PlanningBehaviour(Agent a) {
		super(a);
	}

	@Override
	public void action() {
		

		if(myAgent.needPathPlanning()) {
			Stack<String> 	plannedMoves 	= new Stack<String>();
			Strategy 		strat 			= myAgent.computeStrategy();

			myAgent.donePathPlanning();
			myAgent.addLogEntry("planning needed");
			myAgent.addLogEntry("strategy used : " + strat.toString());

			switch(strat) {
				case GOTO:
					plannedMoves = myAgent.getDedale().getShortestPathFromTo(myAgent.getCurrentRoom(), myAgent.getObjective(), myAgent.getBlockedRooms());
					break;

				case EXPLORATION:
					plannedMoves = myAgent.getDedale().getShortestPathForExploration(myAgent.getCurrentRoom(), myAgent.getBlockedRooms());
					break;

				case RANDOM_WALK:
					plannedMoves = myAgent.getDedale().getRandomWalk(myAgent.getCurrentRoom(), myAgent.getBlockedRooms());
					break;

				case RENDEZ_VOUS:
					int offset = 0;
					if(myAgent.getType() != EntityType.AGENT_TANKER) offset++;

					plannedMoves = myAgent.getDedale().getShortestPathForRendezVous(myAgent.getCurrentRoom(), offset, myAgent.getBlockedRooms());
					break;

				case TREASURE_HUNT:
					plannedMoves = myAgent.getDedale().getShortestPathForTreasureHunt(
							myAgent.getCurrentRoom(),
							myAgent.getTreasureType(),
							myAgent.getBackPackFreeSpace(),
							myAgent.getBackpackCapacity(),
							myAgent.getBlockedRooms());
					break;
				
				default:
					plannedMoves = myAgent.getDedale().idle();
					break;
			}
			myAgent.addLogEntry("planned moves : " + plannedMoves);
			myAgent.setPlannedMoves(plannedMoves);

		}else {
			myAgent.addLogEntry("no planning needed"); 
			myAgent.addLogEntry("planned moves : " + myAgent.getPlannedMoves());
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
