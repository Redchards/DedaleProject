package behaviours;

import java.util.HashSet;
import java.util.Set;
import java.util.Stack;

import agents.AbstractAgent;
import agents.AbstractAgent.DeadlockState;
import agents.AbstractAgent.State;
import jade.core.Agent;
import knowledge.Dedale;

public class DeadlockBehaviour extends AbstractFSMBehaviour {

	private static final long serialVersionUID = -3529771931105597899L;

	public enum Outcome{
		NONE(-1),			// Je ne suis pas bloqué
		UNKNOWN(0),			// Je ne connais pas mon état
		ALTERNATIVE(1),		// J'ai été bloqué, mais j'ai trouvé une alternative
		SOFT_BLOCKED(2),	// Je peut bouger, mais ne vas pas permettre de dégager le passage
		HARD_BLOCKED(3),	// Je ne peut pas bouger 
		FALSE_ALERT(4),		// Personne ne semble me bloquer
		RESET(5);			// J'ai un souci, faisons table rase

		private int value;
		private Outcome(int value) { this.value = value; }
		public int getValue() { return this.value; }
	}

	private Outcome 		outcome;
	private Stack<String> 	nextMoves;	// Stockage juste d'une salle, le but étant de réagir localement à un blocage, et voir si le planificateur global s'en sort
	private int				solvingAttempt;
	private String			lastPos;
	private boolean			isIdle;

	public DeadlockBehaviour(Agent a) {
		super(a);
		outcome 		= Outcome.UNKNOWN;
		nextMoves 		= new Stack<String>();
		solvingAttempt 	= 0;
		isIdle			= false;
		lastPos			= "";
	}

	@Override
	public void action() {
		
		// ~~~~~~~~~~~~~~~~~~~~~
		// ~~~~~ New cycle ~~~~~
		// ~~~~~~~~~~~~~~~~~~~~~

		// ~~~ THIS AGENT ~~~
		String myPosition 		= myAgent.getCurrentRoom();
		String myNextMove 		= myAgent.getNextMove();
		String myDestination 	= myAgent.getDestination();

		if (myNextMove.isEmpty())
			isIdle = true;
		else
			isIdle = false;
		
		// ~~~ OTHER AGENT ~~~
		State 		obstructingAgent	= null;						// L'agent qui me bloque
		Set<State> 	obstructedAgent		= new HashSet<State>();		// Les agents que je bloque
		Set<String> occupiedRooms 		= new HashSet<String>(); 	// Salles occupées par les agents

		if(!lastPos.equalsIgnoreCase(myPosition))
			solvingAttempt = 0;
		else
			solvingAttempt++;
			
		nextMoves = new Stack<String>();
		
		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// ~~~~~ STEP I - FETCHING INFO ~~~~~
		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		myAgent.addLogEntry("Remaining attempt : " + (AbstractAgent.FAILED_DEADLOCK_SOLVING_MAX_ATTEMPT - solvingAttempt));
		myAgent.addLogEntry("~~~ STEP I ~~~");
		myAgent.addLogEntry("finding near agent");
		
		for(State nearAgent:myAgent.getKnownNearAgents().values()) {

			String agentName 		= nearAgent.agentName;
			String agentPos 		= nearAgent.currentPosition;
			String agentNextMove 	= nearAgent.nextMove;

			myAgent.addLogEntry("    " + agentName + " is near me, at : " + agentPos);
			
			occupiedRooms.add(agentPos);

			if(myAgent.isNextToMe(agentPos)) {
				myAgent.addLogEntry("        " + agentName + " is adjacent to me");

				if(agentPos.equals(myNextMove)) {
					obstructingAgent = nearAgent;
					myAgent.addLogEntry("        " + agentName + " is obstructing my next move");
				}

				if(agentNextMove.equals(myPosition)) {
					obstructedAgent.add(nearAgent);
					myAgent.addLogEntry("        " + agentName + " is obstructed by me");
				}
			}
		}

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// ~~~~~ STEP II - Determine possible move ~~~~~
		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		myAgent.addLogEntry("~~~ STEP II ~~~");
		myAgent.addLogEntry("how stuck am I ?");
		
		Set<String> neighbourhood 	= myAgent.getDedale().getNodeNeighbours(myPosition);
		Set<String> freeRooms 		= new HashSet<String>(neighbourhood);
		freeRooms.removeAll(occupiedRooms);

		myAgent.addLogEntry("    I'm at 		: " + myPosition );
		myAgent.addLogEntry("    neighbourhood 	: " + neighbourhood);
		myAgent.addLogEntry("    occupied rooms	: " + occupiedRooms );

		// <===== HARD-BLOCKED =====>
		if(freeRooms.isEmpty()) {
			myAgent.addLogEntry("    I can't move anywhere");
			outcome = Outcome.HARD_BLOCKED;
			return;
		}
		myAgent.addLogEntry("    I can move to these rooms : " + freeRooms);

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// ~~~~~ STEP III - Determine adequate action ~~~~~
		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		myAgent.addLogEntry("~~~ STEP III ~~~");

		// ~~~~~~~~~~~~~~~~
		// ~~~~~ IDLE ~~~~~
		// ~~~~~~~~~~~~~~~~
		if(isIdle) {
			myAgent.addLogEntry("current status: idle");

			// <===== FALSE ALERT =====>
			if(obstructedAgent.isEmpty()) {
				myAgent.addLogEntry("no agent is blocked by me");
				outcome = Outcome.FALSE_ALERT;
				return;
			}
			myAgent.addLogEntry("agent blocked by me: ");
			for(State blockedAgent:obstructedAgent) {
				myAgent.addLogEntry("    " + blockedAgent.agentName);
			}


			Stack<String> myAltRoute = new Stack<String>();
			myAgent.addLogEntry("computing path to move out of the way");
			for(State blockedAgent:obstructedAgent) {
				//TODO Improved computation, so it takes into account all paths
				Stack<String> res = myAgent.getDedale().getPathToParkingRoom(myPosition, blockedAgent.plannedMoves, occupiedRooms);
				if(!res.isEmpty())
					myAltRoute = res;
			}

			if(myAltRoute.isEmpty()) {
				myAgent.addLogEntry("    no parking room found to clear a path");
				myAgent.addLogEntry("    I can still move here : " + freeRooms);
				nextMoves.add(Dedale.randomNode(freeRooms));
				outcome = Outcome.SOFT_BLOCKED;
				return;
			}else {
				myAgent.addLogEntry("    path to a parking room found : " + myAltRoute);
				nextMoves = myAltRoute;
				outcome = Outcome.ALTERNATIVE;
				return;
			}
		}

		// ~~~~~~~~~~~~~~~~~~
		// ~~~~~ MOVING ~~~~~
		// ~~~~~~~~~~~~~~~~~~
		myAgent.addLogEntry("current status: moving");

		// <===== FALSE ALERT =====>
		if(obstructingAgent == null) {
			myAgent.addLogEntry("no agent is blocking my move, maybe it's a Wumpus ?");
			outcome = Outcome.FALSE_ALERT;
			return;
		}

		myAgent.addLogEntry("who is prioritary ?");
		
		if(myAgent.computePriority(obstructingAgent)) {
			myAgent.addLogEntry("    I'm prioritary");
			outcome = Outcome.UNKNOWN;
		
		}else {
			myAgent.addLogEntry("    He is prioritary");
			myAgent.addLogEntry("    Planned moves are : " + obstructingAgent.plannedMoves);

			myAgent.addLogEntry("    let's see if there is an alternative route to " + myDestination +" without going through those rooms : " + occupiedRooms);
			Stack<String> myAltRoute = myAgent.getDedale().getAlternativeRoute(myPosition, myDestination, occupiedRooms);

			if(myAltRoute.isEmpty()) {
				myAgent.addLogEntry("        no alternative route found to my objectives");
				myAgent.addLogEntry("        still got to move out of his way");
				myAgent.addLogEntry("        let's look for an available room outside of his path");

				myAltRoute = myAgent.getDedale().getPathToParkingRoom(myPosition, obstructingAgent.plannedMoves, occupiedRooms);
				if(myAltRoute.isEmpty()) {
					myAgent.addLogEntry("            no parking room found, to clear a path for");
					myAgent.addLogEntry("            I can still move here : " + freeRooms);
					nextMoves.clear();
					nextMoves.push(Dedale.randomNode(freeRooms));
					outcome = Outcome.SOFT_BLOCKED;
				}else {
					myAgent.addLogEntry("            path to a parking room found, which is : " + myAltRoute);
					nextMoves = myAltRoute;
					outcome = Outcome.SOFT_BLOCKED;
				}
			}else {
				myAgent.addLogEntry("        alternative route found, which is : " + myAltRoute);
				nextMoves = myAltRoute;
				outcome = Outcome.ALTERNATIVE;
			}
		}
	}

	@Override
	public boolean done() {
		switch(this.outcome) {
			case HARD_BLOCKED:
				myAgent.setDeadlockState(DeadlockState.HARD_BLOCKED);
				solvingAttempt--; // This attempt does count since i can't do anything
				break;
			case SOFT_BLOCKED:
				myAgent.setDeadlockState(DeadlockState.SOFT_BLOCKED);
				break;
			case ALTERNATIVE:
				solvingAttempt = 0;
				myAgent.setDeadlockState(DeadlockState.ALTERNATIVE);
				break;
			case FALSE_ALERT:
				if(isIdle) {
					outcome = Outcome.NONE;
					solvingAttempt = 0;
				}
				myAgent.setDeadlockState(DeadlockState.NONE);
				break;
			case RESET:
				solvingAttempt = 0;
				myAgent.setDeadlockState(DeadlockState.NONE);
				break;
			case UNKNOWN:
				myAgent.setDeadlockState(DeadlockState.UNKNOWN);
				break;
			case NONE:
				myAgent.setDeadlockState(DeadlockState.NONE);
				break;
		default:
			break;
		}

		if (solvingAttempt >= AbstractAgent.FAILED_DEADLOCK_SOLVING_MAX_ATTEMPT) {
			solvingAttempt = 0;
			outcome = Outcome.RESET;
			myAgent.addLogEntry("failed deadlock behaviour too many times");
			if(!myAgent.getNextMove().isEmpty()) {
				myAgent.addLogEntry("adding " + myAgent.getNextMove() + " to blocked room");
				myAgent.addBlockedRoom(myAgent.getNextMove());
			}
			myAgent.forcePathPlanning();
		}

		myAgent.overrideNextMoves((nextMoves.isEmpty()) ? myAgent.getPlannedMoves() : nextMoves);

		myAgent.clearKnownNearAgents();

		myAgent.addLogEntry("outcome 	: " + outcome);
		myAgent.addLogEntry("state is 	: " + myAgent.getDeadlockState());
		myAgent.addLogEntry("next moves are " + myAgent.getPlannedMoves());

		myAgent.trace(getBehaviourName(), false);
		return true;
	}

	@Override
	public int onEnd() {
		return outcome.getValue();
	}

}
