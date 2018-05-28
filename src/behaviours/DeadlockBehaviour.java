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
		UNKNOWN(-1),
		ALTERNATIVE(0),
		SOFT_BLOCKED(1),	// Peut bouger, mais ne vas pas permettre de dégager le passage
		HARD_BLOCKED(2),	// Ne peut pas bouger 
		FALSE_ALERT(3),		// Un agent ne le bloque apparament pas, mais peut-être le wumpus
		RESET(4);			// Essayer de résoudre le problème avec ces données ne mènent à rien => RESET, c'est à dire que l'on revient à une éxécution normale (On retourne à l'état "OBSERVE") 

		private int value;
		private Outcome(int value) { this.value = value; }
		public int getValue() { return this.value; }
	}

	private Outcome 		outcome;
	private Stack<String> 	nextMoves;	// Stockage juste d'une salle, le but étant de réagir localement à un blocage, et voir si le planificateur global s'en sort
	private int				solvingAttempt;

	public DeadlockBehaviour(Agent a) {
		super(a);
		outcome 		= Outcome.UNKNOWN;
		nextMoves 		= new Stack<String>();
		solvingAttempt 	= 0;
	}

	@Override
	public void action() {
		
		nextMoves = myAgent.getPlannedMoves();
		solvingAttempt++;
		
		// ~~~ THIS AGENT ~~~
		String myPosition 		= myAgent.getCurrentRoom();
		String myNextMove 		= myAgent.getNextMove();
		String myDestination 	= myAgent.getDestination();
		
		// ~~~ OTHER AGENT ~~~
		State 		obstructingAgent	= null;	// L'agent qui me bloque
		Set<State> 	obstructedAgent		= new HashSet<State>();	// Les agents que je bloque
		Set<String> excludedRooms 		= new HashSet<String>();
		Set<String> occupiedRooms 		= new HashSet<String>(); // Salle occupée par des agents, qui ne vont probablement pas bouger entre temps (Typiquement parce qu'ils veulent aller à ma position)

		
		myAgent.addLogEntry("~~~ STEP I ~~~");
		myAgent.addLogEntry("finding near agent");
		
		for(State nearAgent:myAgent.getKnownNearAgents().values()) {

			String agentName 		= nearAgent.agentName;
			String agentPos 		= nearAgent.currentPosition;
			String agentNextMove 	= nearAgent.nextMove;

			myAgent.addLogEntry("    " + agentName + " is near me, at : " + agentPos);

			if(myAgent.isNextToMe(agentPos)) {
				occupiedRooms.add(agentPos);
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
		
		// Si l'agent ne bouge pas et qu'il bloque le passage, il doit s'écarter.
		if(myNextMove.isEmpty()) {

			// OUTCOME = False Alert
			if(obstructedAgent.isEmpty()) {
				myAgent.addLogEntry("no agent is blocked by me");
				outcome = Outcome.FALSE_ALERT;
				return;
			}

			myAgent.addLogEntry("~~~ STEP II ~~~");
			myAgent.addLogEntry("how stuck am I ?");

			Set<String> paths = new HashSet<String>();

			for(State blockedAgent:obstructedAgent) {
				excludedRooms.add(blockedAgent.currentPosition);	// Parce que l'agent va devoir passer par ces casses
				paths.addAll(blockedAgent.plannedMoves);
			}
			excludedRooms.addAll(occupiedRooms);

			Set<String> freeRooms = myAgent.getDedale().getParkingRoom(myPosition, excludedRooms);	// Est-ce que je peux bouger localement ?

			// OUTCOME = Hard Blocked
			if(freeRooms.isEmpty()) {
				myAgent.addLogEntry("    I can't move anywhere");
				outcome = Outcome.HARD_BLOCKED;
				return;
			}

			myAgent.addLogEntry("    I can move to these rooms : " + freeRooms);

			myAgent.addLogEntry("~~~ STEP III ~~~");
			myAgent.addLogEntry("I'm idling, so let's look for an available room outside of his route/plannedMoves");

			Stack<String> pathsToClear = new Stack<String>();
			pathsToClear.addAll(paths);

			Stack<String> myAltRoute = myAgent.getDedale().getPathToParkingRoom(myPosition, pathsToClear, excludedRooms);
			if(myAltRoute.isEmpty()) {
				myAgent.addLogEntry("    no parking room found, to clear a path for");
				myAgent.addLogEntry("    I can still move here : " + freeRooms);
				nextMoves.add(Dedale.randomNode(freeRooms));
				outcome 	= Outcome.SOFT_BLOCKED;
				return;
			}else {
				myAgent.addLogEntry("    path to a parking room found, which is : " + myAltRoute);
				nextMoves 	= myAltRoute;
				outcome 	= Outcome.ALTERNATIVE;
				return;
			}
		}

		// Cas où l'agent se déplace

		// OUTCOME = False Alert
		if(obstructingAgent == null) {
			myAgent.addLogEntry("no agent is blocking my move, maybe it's a Wumpus ?");
			myAgent.addLogEntry("deadlock solving remaining attempt : " + (AbstractAgent.FAILED_DEADLOCK_SOLVING_MAX_ATTEMPT - this.solvingAttempt));
			outcome = Outcome.FALSE_ALERT;
			return;
		}

		myAgent.addLogEntry("~~~ STEP II ~~~");
		myAgent.addLogEntry("how stuck am I ?");

		excludedRooms.add(obstructingAgent.currentPosition); 	// La position de l'agent qui me bloque mon mouvement
		excludedRooms.addAll(occupiedRooms);					// Parce que ces salles sont occupées par des agents qui veulent venir dans ma salle

		Set<String> freeRooms = myAgent.getDedale().getParkingRoom(myPosition, excludedRooms);	// Est-ce que je peux bouger localement ?

		// OUTCOME = Hard Blocked
		if(freeRooms.isEmpty()) {
			myAgent.addLogEntry("    I can't move anywhere");
			outcome = Outcome.HARD_BLOCKED;
			return;
		}

		myAgent.addLogEntry("    I can move to these rooms : " + freeRooms);

		myAgent.addLogEntry("~~~ STEP III ~~~");
		myAgent.addLogEntry("who is prioritary ?");
		
		if(myAgent.computePriority(obstructingAgent)) {
			myAgent.addLogEntry("    I'm prioritary");
		
		}else {
			myAgent.addLogEntry("    He is prioritary");
			myAgent.addLogEntry("    Planned moves are : " + obstructingAgent.plannedMoves);

			myAgent.addLogEntry("    let's see if there is an alternative route to " + myDestination +" without going through those rooms : " + excludedRooms);
			Stack<String> myAltRoute = myAgent.getDedale().getAlternativeRoute(myPosition, myDestination, excludedRooms);

			if(myAltRoute.isEmpty()) {

				myAgent.addLogEntry("        no alternative route found to my objectives");
				myAgent.addLogEntry("        still got to move out of his way");
				myAgent.addLogEntry("        let's look for an available room outside of his route/plannedMoves");

				// Mise à jour des salles qui ne conviennent pas pour se déplacer 
				excludedRooms.addAll(obstructingAgent.plannedMoves);	// Parce que l'agent va devoir passer par ces casses

				myAltRoute = myAgent.getDedale().getPathToParkingRoom(myPosition, obstructingAgent.plannedMoves, excludedRooms);
				if(myAltRoute.isEmpty()) {
					myAgent.addLogEntry("            no parking room found, to clear a path for");
					myAgent.addLogEntry("            I can still move here : " + freeRooms);
					nextMoves.add(Dedale.randomNode(freeRooms));
					outcome 	= Outcome.SOFT_BLOCKED;
				}else {
					myAgent.addLogEntry("            path to a parking room found, which is : " + myAltRoute);
					nextMoves 	= myAltRoute;
					outcome 	= Outcome.ALTERNATIVE;
				}
			}else {
				myAgent.addLogEntry("        alternative route found, which is : " + myAltRoute);
				nextMoves 	= myAltRoute;
				outcome 	= Outcome.ALTERNATIVE;
			}
		}
	}

	@Override
	public boolean done() {
		if (solvingAttempt >= AbstractAgent.FAILED_DEADLOCK_SOLVING_MAX_ATTEMPT & outcome != Outcome.HARD_BLOCKED) {
			solvingAttempt = 0;
			outcome = Outcome.RESET;
			myAgent.addLogEntry("failed deadlock behaviour too many times");
			myAgent.addLogEntry("adding " + myAgent.getNextMove() + " to blocked room");
			myAgent.addBlockedRoom(myAgent.getNextMove());
			myAgent.forcePathPlanning();
		}

		switch(this.outcome) {
			case HARD_BLOCKED:
				myAgent.setDeadlockState(DeadlockState.HARD_BLOCKED);
				break;
			case SOFT_BLOCKED:
				myAgent.setDeadlockState(DeadlockState.SOFT_BLOCKED);
				break;
			case ALTERNATIVE:
				solvingAttempt = 0;
				myAgent.setDeadlockState(DeadlockState.ALTERNATIVE);
				break;
			default:
				myAgent.setDeadlockState(DeadlockState.UNKNOWN);
				break;
		}

		myAgent.overrideNextMoves(nextMoves);
		myAgent.addLogEntry("after analysing the situation, my deadlock state is " + myAgent.getDeadlockState());
		myAgent.addLogEntry("next moves are " + nextMoves);
		myAgent.trace(getBehaviourName(), false);
		return true;
	}

	@Override
	public int onEnd() {
		return outcome.getValue();
	}

}
