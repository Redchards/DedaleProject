package knowledge;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.Stack;
import java.util.TreeMap;

import org.graphstream.algorithm.Toolkit;
import org.graphstream.algorithm.BetweennessCentrality;
import org.graphstream.algorithm.Dijkstra;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.DefaultGraph;
import org.graphstream.graph.implementations.Graphs;
import org.graphstream.ui.view.Viewer;

import env.Attribute;
import env.Couple;

public class Dedale implements Serializable {
	
	// ===================
	// ===== MEMBERS =====
	// ===================
	
	private static final long serialVersionUID = 8928064847035812873L;

	// --- Graph ---
	@SuppressWarnings("unused")
	private transient Viewer 	viewer;
	private transient Graph 	graph;
	private boolean   bDisplayMap;
	
	private String	currentNode;
	private String	lastNode;
	
	// ========================
	// ===== CONSTRUCTORS =====
	// ========================

	public Dedale(boolean bDisplayMap) {
		
		this.bDisplayMap 	= bDisplayMap;

		this.currentNode	= "";
		this.lastNode		= "";

		initializeGraph();
	}
	
	public void start(String id) {
		currentNode = id;
		addNode(id);
	}

	// ==========================
	// ===== PUBLIC METHODS =====
	// ==========================
	
	public void moveTo(String newPos) {
		lastNode	= currentNode;
		currentNode = newPos;
		
		updateNode(lastNode);
		updateNode(currentNode);
	}
	
	public void scavenge(String id, int value) {
		Node node = graph.getNode(id);
		if(node == null) throw new IllegalArgumentException("Node " + id + "is not known");
		
		int treasureValue 	= node.getAttribute(NodeAttributes.TREASURE_VALUE.toString());
		int resultat		= treasureValue - value;

		if(resultat <=0) {
			node.setAttribute(NodeAttributes.TREASURE_TYPE.toString(), TreasureType.NONE.toString());
			node.setAttribute(NodeAttributes.TREASURE_VALUE.toString(), 0);
		}else {
			node.setAttribute(NodeAttributes.TREASURE_VALUE.toString(), resultat);
		}
		
		updateNode(id);
	}
	
	// Return true if the room is a treasure room
	public boolean integrateObservation(String position, List<Couple<String, List<Attribute>>> observations) {

		boolean bIsTreasureRoom = false;
		HashSet<String> neighbours = new HashSet<String>();

		for(Couple<String,List<Attribute>> obs:observations){
			String id = obs.getLeft();

			if(!position.equals(id)) {
				neighbours.add(id);
			}else {
				bIsTreasureRoom = exploreRoom(position, obs.getRight());
			}
		}
		
		for(String id:neighbours) {
			addNode(id);
			addEdge(id, position);
		}

		return bIsTreasureRoom;
	}

	public void integrateKnowledge(Dedale knowledge) {
		HashSet<String> modifiedRooms = new HashSet<String>();

		if(knowledge == null) throw new IllegalArgumentException("Knowledge is null");

		for (Node node: knowledge.getGraph().getEachNode()){
			mergeNode(node, knowledge.getNodeNeighbours(node.getId()));
			modifiedRooms.add(node.getId());
		}
		
		for(String id:modifiedRooms) 
			updateNode(id);
	}

	public boolean isExplored() { return getUnexploredRooms().isEmpty(); }

	public Set<String> getUnexploredRooms(){
		return getUnexploredRooms(graph);
	}

	public Set<String> getUnexploredRooms(Set<String> excludedRooms){
		return getUnexploredRooms(computeSubgraph(excludedRooms));
	}

	public Set<String> getUnexploredRooms(Graph graph){
		HashSet<String> unexploredRooms = new HashSet<String>();

		for(Node node:graph.getEachNode()) {
			if (!(boolean) node.getAttribute(NodeAttributes.HAS_BEEN_EXPLORED.toString()))
				unexploredRooms.add(node.getId());
		}

		return unexploredRooms;
	}

	public Map<String, Integer> getTreasureRooms(TreasureType type){
		return getTreasureRooms(graph, type);
	}

	public Map<String, Integer> getTreasureRooms(TreasureType type, Set<String> excludedRooms){
		return getTreasureRooms(computeSubgraph(excludedRooms), type);
	}
	
	public Map<String, Integer> getTreasureRooms(Graph graph, TreasureType type){
		Map<String, Integer> treasureRooms	= new HashMap<String, Integer>();

		for(Node node:graph.getEachNode()) {
			String treasureType = node.getAttribute(NodeAttributes.TREASURE_TYPE.toString());
			if (treasureType.equalsIgnoreCase(type.toString()))
				treasureRooms.put(node.getId(), (int) node.getAttribute(NodeAttributes.TREASURE_VALUE.toString()));
		}

		return treasureRooms;
	}
	
	public TreasureType getTreasureType(String id) {
		Node node = graph.getNode(id);
		if(node == null) throw new IllegalArgumentException("Node " + id + "is not known");

		return TreasureType.fromValue(node.getAttribute(NodeAttributes.TREASURE_TYPE.toString()));
	}

	public int getTreasureValue(String id) {
		Node node = graph.getNode(id);
		if(node == null) throw new IllegalArgumentException("Node " + id + "is not known");

		return node.getAttribute(NodeAttributes.TREASURE_VALUE.toString());
	}
	
	// ========================
	// ===== PATH METHODS =====
	// ========================

	public Stack<String> getRandomWalk(String from){
		return getRandomWalk(graph, from);
	}
	
	public Stack<String> getRandomWalk(String from, Set<String> excludedRooms){
		if(excludedRooms.contains(from)) excludedRooms.remove(from);
		return getRandomWalk(computeSubgraph(excludedRooms), from);
	}

	public Stack<String> getRandomWalk(Graph graph, String from){
		Stack<String> 	path 	= new Stack<String>();
			
		path = getShortestPathFromTo(graph, from, Toolkit.randomNode(graph).getId());
		if(path.isEmpty()) {
			Set<String> neighbours 	= getNodeNeighbours(graph, from);

			if(neighbours.isEmpty())
				return path;
			path.push(Dedale.randomNode(neighbours));
		}
		return path;
			
	}

	public Stack<String> getShortestPathForExploration(String from){
		return getShortestPathForExploration(graph, from);
	}
	
	public Stack<String> getShortestPathForExploration(String from, Set<String> excludedRooms){
		if(excludedRooms.contains(from)) excludedRooms.remove(from);
		return getShortestPathForExploration(computeSubgraph(excludedRooms), from);
	}

	public Stack<String> getShortestPathForExploration(Graph graph, String position){
		Stack<String> 	path 		= new Stack<String>();

		Set<String> unexploredRooms = getUnexploredRooms(graph);
		Set<String> neighbours 		= getNodeNeighbours(graph, position);

		neighbours.retainAll(unexploredRooms);

		if(!neighbours.isEmpty()) {
			path.push(randomNode(neighbours));

		}else {
			Dijkstra dijkstra = new Dijkstra(Dijkstra.Element.EDGE, null, null);
			dijkstra.init(graph);
			dijkstra.setSource(graph.getNode(position));
			dijkstra.compute();
			
			TreeMap<Integer, Node> paths = new TreeMap<Integer, Node>();
			for(String id:unexploredRooms) {
				Node node = graph.getNode(id);
				paths.put((int) dijkstra.getPathLength(node), node);
			}

			if(!paths.isEmpty()) {
				Node nearestNode = paths.get(paths.firstKey());
				for (Node node : dijkstra.getPathNodes(nearestNode)) {
					path.push(node.getId());
				}
				if(!path.isEmpty())
					path.pop(); // Enleve le noeud actuelle du plan
			}

			dijkstra.clear();
		}
		return path;
	}

	public Stack<String> getShortestPathForTreasureHunt(String from, TreasureType treasureType, int freeSpace, int capacity){
		return getShortestPathForTreasureHunt(from, treasureType, freeSpace, capacity, new HashSet<String>());
	}

	public Stack<String> getShortestPathForTreasureHunt(String from, TreasureType treasureType, int freeSpace, int capacity, Set<String> excludedRooms){
		if(excludedRooms.contains(from)) excludedRooms.remove(from);
		return getShortestPathForTreasureHunt(computeSubgraph(excludedRooms), from, treasureType, freeSpace, capacity);
	}

	public Stack<String> getShortestPathForTreasureHunt(Graph graph, String from, TreasureType treasureType, int freeSpace, int capacity){
		Stack<String> 	path 				= new Stack<String>();

		Map<String, Integer> treasureRooms = getTreasureRooms(graph, treasureType);
		treasureRooms.remove(from);

		TreeMap<Integer, String> optimalRooms = new TreeMap<Integer, String>();
		for(Map.Entry<String, Integer> treasureRoom:treasureRooms.entrySet()) {
			String 	id 		= treasureRoom.getKey();
			int		value	= treasureRoom.getValue();

			if(value <= freeSpace || freeSpace == capacity)
				optimalRooms.put(Math.abs(value-freeSpace),id);	// La salle dont la valeur se rapproche le plus de la capacité restante
		}
		
		// Triage des salles optimales voisines
		TreeMap<Integer, String> neighbourOptimalRooms = new TreeMap<Integer, String>();
		for(Map.Entry<Integer, String> optimalRoom:optimalRooms.entrySet()) {
			if(getNodeNeighbours(graph, from).contains(optimalRoom.getValue()))
				neighbourOptimalRooms.put(optimalRoom.getKey(), optimalRoom.getValue());
		}

		// Si des salles optimales sont des salles voisines, se diriger vers la meilleure
		String pos;
		if(!neighbourOptimalRooms.isEmpty()) {
			pos = neighbourOptimalRooms.get(neighbourOptimalRooms.firstKey());
			path.push(pos);
		}
		else{	// Sinon se diriger vers la meilleure salle (où qu'elle soit)
			pos = optimalRooms.get(optimalRooms.firstKey());
			path = getShortestPathFromTo(graph, from, pos);
		}
		return path;
	}

	public Stack<String> getShortestPathForRendezVous(String from, int offset){
		return getShortestPathForRendezVous(graph, from, offset);
	}

	public Stack<String> getShortestPathForRendezVous(String from, int offset, Set<String> excludedRooms){
		if(excludedRooms.contains(from)) excludedRooms.remove(from);
		return getShortestPathForRendezVous(computeSubgraph(excludedRooms), from, offset);
	}
	
	public Stack<String> getShortestPathForRendezVous(Graph graph, String from, int offset){
		BetweennessCentrality bcb = new BetweennessCentrality();
		bcb.setUnweighted();
		bcb.init(graph);
		bcb.compute();
		
		ArrayList<Node> degreeMap = Toolkit.degreeMap(graph);
		
		Node rdvNode = degreeMap.get(0);
		for(Node node:degreeMap) {
			double cb 	= rdvNode.getAttribute("Cb");
			double _cb 	= node.getAttribute("Cb");
			int degree	= rdvNode.getDegree();
			int _degree	= node.getDegree();

			if( _degree > degree ) { rdvNode = node; }
			else if( _degree == degree ) {
				if( _cb > cb ) { rdvNode = node; }
				else if( _cb == cb ) {
					if (node.getId().compareTo(rdvNode.getId()) > 0)
						rdvNode = node;
				}
			}
		}
		
		String to = "";
		if( offset != 0) {
			to = Dedale.randomNode(getNodeNeighbours(graph, rdvNode.getId()));
		}else {
			to = rdvNode.getId();
		}

		return getShortestPathFromTo(graph, from, to);
	}

	public Stack<String> getShortestPathFromTo(String from, String to){
		return getShortestPathFromTo(this.graph, from, to);
	}

	public Stack<String> getShortestPathFromTo(String from, String to, Set<String> excludedRooms){
		if(excludedRooms.contains(from)) return idle();
		return getShortestPathFromTo(computeSubgraph(excludedRooms), from, to);
	}

	public Stack<String> getShortestPathFromTo(Graph graph, String from, String to){
		if(graph.getNode(from) == null) throw new IllegalArgumentException("Node 'from' " + from + " is not known");
		if(graph.getNode(to) == null) 	throw new IllegalArgumentException("Node 'to' " + to + " is not known");

		Stack<String> path = new Stack<String>();
		
		if(from.equals(to)) return path;

		// Si la salle est voisine pas la peine de faire un plus court chemin
		if((getNodeNeighbours(graph, from)).contains(to)) { path.push(to); return path; }

		Dijkstra dijkstra = new Dijkstra(Dijkstra.Element.EDGE, null, null);

		dijkstra.init(graph);
		dijkstra.setSource(graph.getNode(from));
		dijkstra.compute();

		for (Node node : dijkstra.getPathNodes(graph.getNode(to)))
			path.push(node.getId());

		if(!path.isEmpty())  path.pop();

		dijkstra.clear();

		return path;
	}
	
	public Stack<String> idle(){
		return new Stack<String>();
	}
	
	public Stack<String> getAlternativeRoute(String from, String to, Set<String> excludedRooms){
		if(graph.getNode(from) == null) 	throw new IllegalArgumentException("Node 'from' " + from + "is not known");
		if(graph.getNode(to) == null) 		throw new IllegalArgumentException("Node 'to' " + to + "is not known");

		if(excludedRooms.contains(from))	return new Stack<String>();
		if(excludedRooms.contains(to)) 		return new Stack<String>();
	
		return this.getShortestPathFromTo(computeSubgraph(excludedRooms), from, to);
	}

	/**
	 * @param from 		Salle de de départ
	 * @param excludedRooms	Salle où l'on ne peut pas stationner
	 * @return 				Salle d'arrivée si il y en a une, vide sinon
	 */
	public Set<String> getParkingRoom(String from, Set<String> excludedRooms){
		if(graph.getNode(from) != null)
			return getParkingRoom(computeSubgraph(excludedRooms), from);
		return new HashSet<String>();
	}

	public Set<String> getParkingRoom(Graph graph, String from){
		if(graph.getNode(from) == null)
			return new HashSet<String>();
		return getNodeNeighbours(graph, from);
	}

	/**
	 * @param from				Salle de départ
	 * @param plan				Chemin dont on doit libérer le passage 
	 * @param excludedRooms		Salle où l'on ne peut pas stationner
	 * @return					Le chemin pour accéder à une salle de stationnement (la plus proche, aléatoire parmi si équivalente, probablement libre), vide sinon
	 */
	@SuppressWarnings("unchecked")
	public Stack<String> getPathToParkingRoom(String from, Stack<String> plan, Set<String> excludedRooms){
		
		Stack<String> 	_plan 			= (Stack<String>) plan.clone();		  			// Le chemin que l'on doit libérer
		Set<String> 	_excludedRooms 	= new HashSet<String>(excludedRooms); 	// Les salles où l'on ne peut pas stationner
		_plan.addAll(plan);

		Stack<String> 	path 		= new Stack<String>();			// Le chemin pour accéder à la salle de stationnement
		Set<String> 	candidates	= new HashSet<String>();		// Les salles où l'agent pourrait potentiellement stationné

		while(candidates.isEmpty() && !_plan.isEmpty()) {			// Tant qu'on a pas une place de stationnement à chaque point du chemin
			String id = _plan.pop();
			candidates = getParkingRoom(id, _excludedRooms);
			_excludedRooms.add(id);									// La salle précédente du chemin ne doit pas être vue comme une salle potentielle, puisque l'agent doit libérer le chemin
		}

		if(!candidates.isEmpty())
			path = getShortestPathFromTo(from, randomNode(candidates), excludedRooms);

		return path; 
	}

	// ===========================
	// ===== PRIVATE METHODS =====
	// ===========================

	// Return true if the room is a treasure room
	private boolean exploreRoom(String id, List<Attribute> info) {
		Node node = graph.getNode(id);

		node.setAttribute(NodeAttributes.LAST_EXPLORED.toString(), LocalTime.now().toNanoOfDay());
		node.setAttribute(NodeAttributes.HAS_BEEN_EXPLORED.toString(), true);
		

		for(Attribute attr:info) {
	        String attributeName  = attr.getName();
	        Object attributeValue = attr.getValue();
			
			switch(attributeName) {
				case "Diamonds":
					node.setAttribute(NodeAttributes.TREASURE_TYPE.toString(), TreasureType.DIAMONDS.toString());
					node.setAttribute(NodeAttributes.TREASURE_VALUE.toString(), (int) attributeValue);
					return true;

				case "Treasure":
					node.setAttribute(NodeAttributes.TREASURE_TYPE.toString(), TreasureType.TREASURE.toString());
					node.setAttribute(NodeAttributes.TREASURE_VALUE.toString(), (int) attributeValue);
					return true;

				default:
					node.setAttribute(NodeAttributes.TREASURE_TYPE.toString(), TreasureType.NONE.toString());
					node.setAttribute(NodeAttributes.TREASURE_VALUE.toString(), 0);
					return false;
			}
	    }
		return false;
	}

	private Graph computeSubgraph(Set<String> excludedRooms) {
		Graph subgraph = Graphs.clone(graph);
		for(String excludedRoom:excludedRooms) {
			if(!excludedRoom.isEmpty() && graph.getNode(excludedRoom) != null) subgraph.removeNode(excludedRoom);
		}
		return subgraph;
	}
	// ===========================
	// ===== UTILITY METHODS =====
	// ===========================
	
	public static String randomNode(Set<String> set) {
		Random rnd = new Random();
		int i = rnd.nextInt(set.size());
		return (String) set.toArray()[i];
	}

	// =======================
	// ===== NESTED ENUM =====
	// =======================

	// Enum describing the type of the treasure room
	// 		NONE 		- Initialization value 
	// 		TREASURE 	-
	// 		DIAMONDS 	- 
	public enum TreasureType {
		NONE("NONE"),
		TREASURE("TREASURE"),
		DIAMONDS("DIAMONDS");
		
		private final String value;
		
		TreasureType(String value){ this.value = value; }

		public String getValue() { return value; }
		public String toString() { return value; }
		public static TreasureType getDefault() { return NONE; }
		
		public static TreasureType fromValue(String value) {
			if(value != null) {
				for(TreasureType treasureType : values())
						if (treasureType.value.equalsIgnoreCase(value)) { return treasureType; }

			}else { throw new IllegalArgumentException("value is null"); }

			return getDefault();
		}
	}

	// Enum describing the attributes of a node
	//	HAS_BEEN_EXPLORED 	- boolean
	//	LAST_EXPLORED		- long
	//	TREASURE_TYPE		- string
	//	TREASURE_VALUE		- int
	private enum NodeAttributes {
		HAS_BEEN_EXPLORED("HAS_BEEN_EXPLORED"),
		LAST_EXPLORED("LAST_EXPLORED"),
		TREASURE_TYPE("TREASURE_TYPE"),
		TREASURE_VALUE("TREASURE_VALUE");
		
		private final String value;
		
		NodeAttributes(String value){ this.value = value; }

		public String toString() { return value; }
	}

	// =================
	// ===== GRAPH =====
	// =================
	
	private void initializeGraph() {
		graph = new DefaultGraph("Dedale");

		graph.setStrict(false);
		graph.setAutoCreate(false);

// Uncomment if you favor quality over speed
//		graph.addAttribute("ui.quality");
//		graph.addAttribute("ui.antialias");

		graph.addAttribute("ui.stylesheet", "url('file:config/stylesheet.css')");

		if(bDisplayMap) viewer = graph.display();
	}
	
	public boolean areNodeAdjacent(String a, String b) {
		return getNodeNeighbours(a).contains(b);
	}
	
	private void updateNode(String id) {
		Node node = graph.getNode(id);
		if(node == null) throw new IllegalArgumentException("Node " + id + "is not known");

		node.setAttribute("ui.class", getNodeStyle(id));
	}
	
	private void addNode(String id) {
		Node node = graph.getNode(id);
		if(node == null) {
			node = graph.addNode(id); 
			node.addAttribute(NodeAttributes.HAS_BEEN_EXPLORED.toString(), false);
			node.addAttribute(NodeAttributes.LAST_EXPLORED.toString(), (long) 0);
			node.addAttribute(NodeAttributes.TREASURE_TYPE.toString(), TreasureType.NONE.toString());
			node.addAttribute(NodeAttributes.TREASURE_VALUE.toString(), 0);

			node.setAttribute("ui.label", id);
		}
		node.setAttribute("ui.class", getNodeStyle(id));
	}
	
	private void addNode(Node _node, Set<String> neighbours) {
		String id = _node.getId();

		Node node = graph.addNode(id);
		node.addAttribute(NodeAttributes.HAS_BEEN_EXPLORED.toString(), (boolean) _node.getAttribute(NodeAttributes.HAS_BEEN_EXPLORED.toString()));
		node.addAttribute(NodeAttributes.LAST_EXPLORED.toString(), (long) _node.getAttribute(NodeAttributes.LAST_EXPLORED.toString()));
		node.addAttribute(NodeAttributes.TREASURE_TYPE.toString(), (String) _node.getAttribute(NodeAttributes.TREASURE_TYPE.toString()));
		node.addAttribute(NodeAttributes.TREASURE_VALUE.toString(), (int) _node.getAttribute(NodeAttributes.TREASURE_VALUE.toString()));
		node.setAttribute("ui.label", id);
		node.setAttribute("ui.class", getNodeStyle(id));

		for(String _id:neighbours) {
			if(graph.getNode(_id) == null) addNode(_id);
			addEdge(_id, id);
		}
	}
	
	private void addNode(Room room) {
		Node node = graph.getNode(room.id);
		if(node == null) node = graph.addNode(room.id);

		node.addAttribute(NodeAttributes.HAS_BEEN_EXPLORED.toString(), (boolean) room.hasBeenExplored);
		node.addAttribute(NodeAttributes.LAST_EXPLORED.toString(), (long) room.lastExplored);
		node.addAttribute(NodeAttributes.TREASURE_TYPE.toString(), (String) room.treasureType);
		node.addAttribute(NodeAttributes.TREASURE_VALUE.toString(), (int) room.treasureValue);
		node.setAttribute("ui.label", room.id);
		node.setAttribute("ui.class", getNodeStyle(room.id));
		
		for(String id:room.neighbours) {
			if(graph.getNode(id) == null) addNode(id);
			addEdge(id, room.id);
		}
	}

	private void mergeNode(Node _node, Set<String> neighbours) {
		String id = _node.getId();

		Node node = graph.getNode(id);
		if(node == null) {
			addNode(_node, neighbours);
		}else {
			long _nodeLastExplore 	= _node.getAttribute(NodeAttributes.LAST_EXPLORED.toString());
			long nodeLastExplore 	= node.getAttribute(NodeAttributes.LAST_EXPLORED.toString());
			
			if(nodeLastExplore - _nodeLastExplore < 0)
				addNode(_node, neighbours);
		}
	}

	private void addEdge(String a, String b) {
		graph.addEdge(a+"_"+b, a, b);
	}
	
	private String getNodeStyle(String id) {
		Node node = graph.getNode(id);
		if(node == null) throw new IllegalArgumentException("Node " + id + "is not known");

		String style = "";
		
		TreasureType treasureType = TreasureType.fromValue(node.getAttribute(NodeAttributes.TREASURE_TYPE.toString()));

		switch(treasureType) {
			case TREASURE:
				style += ", treasure";
				break;
			case DIAMONDS:
				style += ", treasure_diamonds";
				break;
			default:
				break;
		}

		if(id.equals(currentNode)) style += ", agent";

		if((boolean) node.getAttribute(NodeAttributes.HAS_BEEN_EXPLORED.toString())) style += ", visited";

		return style;
	}

	public Set<String> getNodeNeighbours(String id){
		Node node = graph.getNode(id);
		if(node == null) throw new IllegalArgumentException("Node " + id + " is not known");

		Iterator<Node> it 	= node.getNeighborNodeIterator();
		HashSet<String> neighbours = new HashSet<String>();

		while(it.hasNext()) neighbours.add(it.next().getId());
		
		return neighbours;
	}

	public Set<String> getNodeNeighbours(Graph graph, String id){
		Node node = graph.getNode(id);
		if(node == null) throw new IllegalArgumentException("Node " + id + " is not known");

		Iterator<Node> it 	= node.getNeighborNodeIterator();
		HashSet<String> neighbours = new HashSet<String>();

		while(it.hasNext()) neighbours.add(it.next().getId());
		
		return neighbours;
	}
	// =============================
	// ===== GETTERS & SETTERS =====
	// =============================

	public String getCurrentNode() { return currentNode; }

	public String getLastNode() { return lastNode; }

	public Graph getGraph() { return graph; }

	public void setCurrentNode(String currentNode) { this.currentNode = currentNode; }

	public void setLastNode(String lastNode) { this.lastNode = lastNode; }
	
	// =========================
	// ===== SERIALIZATION =====
	// =========================

	// Class used for graph serialization
	private class Room implements Serializable{

		private static final long serialVersionUID = 9174808852329879086L;

		public String 		id;
		public boolean 		hasBeenExplored;
		public long 		lastExplored;
		public String		treasureType;
		public int			treasureValue;
		public Set<String>	neighbours;
		
		public Room(String id, boolean hasBeenExplored, long lastExplored, String treasureType, int treasureValue, Set<String> neighbours) {
			this.id 				= id;
			this.hasBeenExplored 	= hasBeenExplored;
			this.lastExplored 		= lastExplored;
			this.treasureType 		= treasureType;
			this.treasureValue 		= treasureValue;
			this.neighbours 		= neighbours;
		}
	}
	
	private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
		this.bDisplayMap 	= false;

		this.currentNode	= "";
		this.lastNode		= "";
		
		initializeGraph();

		int nodeCount = ois.readInt();
		
		for(int i=0; i<nodeCount; i++) {
			Room room = (Room) ois.readObject();
			addNode(room);
		}
    }

    private void writeObject(ObjectOutputStream oos) throws IOException {
    	oos.writeInt(graph.getNodeCount());

    	for(Node node:graph.getEachNode()) {
    		Room room = new Room(
    				node.getId(),
    				(boolean) node.getAttribute(NodeAttributes.HAS_BEEN_EXPLORED.toString()),
    				(long) node.getAttribute(NodeAttributes.LAST_EXPLORED.toString()),
    				(String) node.getAttribute(NodeAttributes.TREASURE_TYPE.toString()),
    				(int) node.getAttribute(NodeAttributes.TREASURE_VALUE.toString()),
    				getNodeNeighbours(node.getId())
    				);
    		oos.writeObject(room);
    	}
    }
}
