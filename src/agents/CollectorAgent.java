package agents;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import behaviours.AbstractReceiveMessageBehaviour;
import behaviours.EmptyBackpackBehaviour;
import behaviours.ObservationBehaviour;
import behaviours.ScavengeBehaviour;
import knowledge.Dedale;

public class CollectorAgent extends AbstractAgent {

	private static final long serialVersionUID = -5033177871865633542L;

	protected void setup() {
		super.setup();

		// ~~~~~ Define FSM ~~~~~

		// ~~~ FSM STATES
		fsm.registerState(new ScavengeBehaviour(this)		, STATE_SCAVENGE);
		fsm.registerState(new EmptyBackpackBehaviour(this)	, STATE_EMPTY_BACKPACK);

		// ~~~ FSM TRANSITIONS
		fsm.registerTransition(STATE_OBSERVE, STATE_SCAVENGE, ObservationBehaviour.Outcome.TREASURE_ROOM.getValue());

		fsm.registerDefaultTransition(STATE_SCAVENGE, STATE_RECEIVE_HELLO);

		fsm.registerTransition(STATE_RECEIVE_DEDALE		, STATE_EMPTY_BACKPACK 			, AbstractReceiveMessageBehaviour.Outcome.NO_MORE_MESSAGE.getValue());
		fsm.registerTransition(STATE_RECEIVE_DEDALE		, STATE_EMPTY_BACKPACK 			, AbstractReceiveMessageBehaviour.Outcome.NO_MESSAGE.getValue());

		fsm.registerDefaultTransition(STATE_EMPTY_BACKPACK, STATE_PLANNING);

		addBehaviour(fsm);
	}
	
	@Override
	public Strategy computeStrategy() {
		Map<String, Integer> treasureRooms = new HashMap<String, Integer>();
		treasureRooms = dedale.getTreasureRooms(getTreasureType());
		treasureRooms.remove(getCurrentRoom());
		treasureRooms.keySet().removeAll(blockedRooms.keySet());

		int remainingFreeSpace 	= getBackPackFreeSpace();
		int backpackCapacity   	= getBackpackCapacity();
		
		// I/ - Reste t-il des salles de trésors ?
		if(treasureRooms.isEmpty()) {
			addLogEntry("no fitting treasure room");

			if(backpackCapacity > remainingFreeSpace) { // a) Rejoindre le tanker si : Le sac est remplie, sa position est connue
				addLogEntry("my backpack is not empty");
				lookForTanker();
			}else {										// b) Sinon explorer pour trouver des salles potentiels
				addLogEntry("my backpack is empty");
				if(dedale.isExplored())
					strategy = Strategy.RANDOM_WALK;
				else
					strategy = Strategy.EXPLORATION;
			}
		// II/ - Si oui, est-il intéressant d'aller ou mieux vaut chercher le tanker ? 
		}else {
			addLogEntry("fitting treasure room available : " + treasureRooms);

			// a) Si la capacité restante du robot permet de récupérer en entier un trésor (ou que son sac est vide), on l'ajoute aux cibles potentiels
			TreeMap<Integer, String> optimalRooms = new TreeMap<Integer, String>();
			for(Map.Entry<String, Integer> treasureRoom:treasureRooms.entrySet()) {
				String 	id 		= treasureRoom.getKey();
				int		value	= treasureRoom.getValue();
				if(value <= remainingFreeSpace || remainingFreeSpace == backpackCapacity) {
					optimalRooms.put(Math.abs(value-remainingFreeSpace),id);	// La salle dont la valeur se rapproche le plus de la capacité restante
				}
			}

			if(optimalRooms.isEmpty()) {
				addLogEntry("fitting treasure rooms are available, but my remaining capacity is too low");
				lookForTanker();
			}else {
				strategy = Strategy.TREASURE_HUNT;
			}
		}
		addLogEntry("Strategy : " + strategy);
		trace("STRATEGY", false);
		return strategy;
	}

	@Override
	public boolean isGoalReached() {
		if( dedale.isExplored() & dedale.getTreasureRooms(getTreasureType()).isEmpty() & isBackpackEmpty()){
			if(bIsGoalReached == false) goalReached();
			return true;
		}
		return false;
	}

	private void lookForTanker() {
		Map<String, String> tankers = getKnownNearTankers();
		if(tankers.isEmpty()) {
			addLogEntry("no tanker nearby");
			if(dedale.isExplored()) { strategy = Strategy.RENDEZ_VOUS; }
			else 					{ strategy = Strategy.EXPLORATION; }
		}else {
			addLogEntry("tanker nearby");
			String pos = tankers.values().iterator().next();

			if(dedale.getGraph().getNode(pos) == null) {
				strategy = Strategy.EXPLORATION;
			}else {
				Set<String> rooms = dedale.getNodeNeighbours(pos);
				rooms.removeAll(blockedRooms.keySet());
				objective = Dedale.randomNode(rooms);
				strategy = Strategy.GOTO;
			}
		}
	}

}
