package domain;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import java.util.Set;
import java.util.TreeSet;

import main.activator.Activator;
import adt.graph.AbstractPath;
import adt.graph.Edge;
import adt.graph.Node;
import domain.constants.DefUsesView;
import domain.events.DefUsesChangedEvent;
import domain.events.TestRequirementChangedEvent;

public class DefUsesSet extends Observable implements Observer {
	
	private Map<Object, List<List<String>>> nodeedgeDefUses;
	private Map<String, List<List<Object>>> variableDefUses;
	private Map<Object, Set<AbstractPath<Integer>>> nodeedgeTestRequirements;
	private Map<String, Set<AbstractPath<Integer>>> variableTestRequirements;
	
	public DefUsesSet() {
		nodeedgeDefUses = new LinkedHashMap<Object, List<List<String>>>();
		variableDefUses = new LinkedHashMap<String, List<List<Object>>>();
		nodeedgeTestRequirements = new LinkedHashMap<Object,  Set<AbstractPath<Integer>>>();
		variableTestRequirements = new LinkedHashMap<String,  Set<AbstractPath<Integer>>>();
	}
	
	public void addObserver() {
		Activator.getDefault().getTestRequirementController().addObserverTestRequirement(this);
	}
	
	public void deleteObserver() {
		Activator.getDefault().getTestRequirementController().deleteObserverTestRequirement(this);
	}
	
	@Override
	public void update(Observable obs, Object data) {
		if(data instanceof TestRequirementChangedEvent) 
			switch(Activator.getDefault().getTestRequirementController().getSelectedCoverageCriteria()) {
				case ALL_DU_PATHS:
				case ALL_DEFS:
				case ALL_USES:
					getTestRequirements(((TestRequirementChangedEvent) data).testRequirementSet);
					break;
				default:
					break;
			}		
	}

	/***
	 * Insert a new element in the list.
	 * Update the variables list.
	 * 
	 * @param obj - The Object associated to the def-uses (Node or edge).
	 * @param defuses - The list of Def-Uses to the Object.
	 */
	public void put(Object obj, List<List<String>> defuses) {
		getDefUsesByNodeEdge(obj, defuses);
		getDefUsesByVariables();
		setChanged();
		notifyObservers(new DefUsesChangedEvent(nodeedgeDefUses, variableDefUses));
	}
	
	/***
	 * Clear all structures and notify the view.
	 */
	public void clear() {
		nodeedgeDefUses.clear();
		variableDefUses.clear();
		nodeedgeTestRequirements.clear();
		variableTestRequirements.clear();
		setChanged();
		notifyObservers(new DefUsesChangedEvent(nodeedgeDefUses, variableDefUses));
	}
	
	/***
	 * Varify if the set is empty;
	 * @return boolean - true if yes.
	 * 				     false otherwise.
	 */
	public boolean isEmpty() {
		return nodeedgeDefUses.isEmpty();
	}
	
	public void notifyChanges() {
		setChanged();
		notifyObservers(new DefUsesChangedEvent(nodeedgeDefUses, variableDefUses));
	}
	
	private void getDefUsesByNodeEdge(Object obj, List<List<String>> defuses) {
		if(nodeedgeDefUses.isEmpty())
			nodeedgeDefUses.put(obj, defuses);
		else {
			Map<Object, List<List<String>>> aux = new LinkedHashMap<Object, List<List<String>>>();
			int pos = -1;
			int res;	
			int i = 0;
			for(Object key : nodeedgeDefUses.keySet()) {
				res = compare(key, obj);
				if(res == 1) {
					pos = i;
					break;
				}
				i++;
			}
			if(pos == -1)
				nodeedgeDefUses.put(obj, defuses);
			else {
				i = 0;
				for(Object key : nodeedgeDefUses.keySet()) {
					List<List<String>> vars = nodeedgeDefUses.get(key);
					if(i == pos) 
						aux.put(obj, defuses);
					aux.put(key, vars);
					i++;
				}
				nodeedgeDefUses.clear();
				nodeedgeDefUses = aux;
			}
		}
	}
	
	/***
	 * Get the def-uses for all variables.
	 */
	private void getDefUsesByVariables() {
		variableDefUses.clear();
		Set<String> vars = getVariable();
		for(String var : vars) 
			variableDefUses.put(var, getNodesEdgesDefUses(var));
	}
	
	/***
	 * Get the nodes and edges for a variable.
	 * 
	 * @param var - The variable to get the nodes and edges.
	 * @return List<List<Object>> - A List with two list.
	 * 								One to the nodes and edges of variable definitions.
	 * 								Other to the node and edges of variable uses.
	 */
	private List<List<Object>> getNodesEdgesDefUses(String var) {
		List<List<Object>> nodeedges = new LinkedList<List<Object>>();
		List<Object> defs = new LinkedList<Object>();
		List<Object> uses = new LinkedList<Object>();
		for(Object key : nodeedgeDefUses.keySet()) {
			List<List<String>> vars = nodeedgeDefUses.get(key);
			if(vars.get(0).contains(var)) {
				if(defs.isEmpty())
					defs.add(key);
				else 
					addToList(defs, key);
			}
			if(vars.get(1).contains(var)) {
				if(uses.isEmpty())
					uses.add(key);
				else 
					addToList(uses, key);
			}
		}
		nodeedges.add(defs);
		nodeedges.add(uses);
		return nodeedges;
	}
	
	/***
	 * Add the object to the right place in the list.
	 * 
	 * @param list - The list to add the Object.
	 * @param obj - The Object to be added.
	 * @param compare - The result of compare the last Object in the list and the Object to be added.
	 */
	private void addToList(List<Object> list, Object obj) {
		int pos = -1;
		int res;
		for(Object o : list) {
			res = compare(o, obj);
			if(res == 1) {
				pos = list.indexOf(o);
				break;
			}
		}
		if(pos == -1)
			list.add(obj);
		else
			list.add(pos, obj);
	}

	/***
	 * Compare two object to see which one is bigger.
	 * 
	 * @param inList - The Object in the list.
	 * @param toAdd - The Object to add to the list.
	 * @return int - 0 if they are equal;
	 * 				 1 if the Object in the list is bigger then the Object to add.
	 * 				-1 if the Object in the list is lesser than the Object to add. 
	 */
	@SuppressWarnings("unchecked")
	private int compare(Object inList, Object toAdd) {
		Node<Integer> nodeInList;
		Node<Integer> nodeToAdd;
		Edge<Integer> edgeInList;
		Edge<Integer> edgeToAdd;
		if(inList instanceof Node<?>) {
			nodeInList = (Node<Integer>) inList;
			if(toAdd instanceof Node<?>) 
				nodeToAdd = (Node<Integer>) toAdd;
			else 
				nodeToAdd = ((Edge<Integer>) toAdd).getBeginNode();
			return nodeInList.compareTo(nodeToAdd);
		} else {
			if(toAdd instanceof Node<?>) {
				nodeInList = ((Edge<Integer>) inList).getBeginNode();
				nodeToAdd = (Node<Integer>) toAdd;
				return nodeInList.compareTo(nodeToAdd);
			} else {
				edgeInList = (Edge<Integer>) inList;
				edgeToAdd = (Edge<Integer>) toAdd;
				return edgeInList.compareTo(edgeToAdd);
			}
		}
	}

	/***
	 * Get all variables.
	 * 
	 * @return Set<String> - The set of variables.
	 */
	private Set<String> getVariable() {
		Set<String> vars = new TreeSet<String>();
		for(Object key : nodeedgeDefUses.keySet()) {
			List<List<String>> varList = nodeedgeDefUses.get(key);
			vars.addAll(varList.get(0));
			vars.addAll(varList.get(1));
		}
		return vars;
	}
	
	public Map<Object, List<List<String>>> getDefUsesByNodeEdge() {
		return nodeedgeDefUses;
	}
	
	public Map<String, List<List<Object>>> getDefUsesByVariable() {
		return variableDefUses;
	}

	private void getTestRequirements(Iterable<AbstractPath<Integer>> testRequirements) {
		nodeedgeTestRequirements.clear();
		variableTestRequirements.clear();
		for(Object key : nodeedgeDefUses.keySet()) {
			Set<AbstractPath<Integer>> pathsForNodeEdge = new LinkedHashSet<AbstractPath<Integer>>();
			for(AbstractPath<Integer> path : testRequirements)
				setDefUsesStatus(pathsForNodeEdge, path, key, DefUsesView.NODE_EDGE);
			nodeedgeTestRequirements.put(key, pathsForNodeEdge);
		}
		for(String key : variableDefUses.keySet()) {
			Set<AbstractPath<Integer>> pathsForVariables = new LinkedHashSet<AbstractPath<Integer>>();
			for(AbstractPath<Integer> path : testRequirements)
				setDefUsesStatus(pathsForVariables, path, key, DefUsesView.VARIABLE);
			variableTestRequirements.put(key, pathsForVariables);
		}
	}
	
	@SuppressWarnings("unchecked")
	private void setDefUsesStatus(Set<AbstractPath<Integer>> paths, AbstractPath<Integer> path, Object key, DefUsesView view) {
		switch(view) {
			case NODE_EDGE:
				if(key instanceof Edge<?> && path.isEdgeOfPath((Edge<Integer>) key))
					paths.add(path);
				else if(key instanceof Node<?> && path.containsNode((Node<Integer>) key))
					paths.add(path);
				break;
			case VARIABLE:
				List<List<Object>> values = variableDefUses.get(key);
				List<Object> defs = values.get(0);
				List<Object> uses = values.get(1);
				for(Object def : defs) {
					Node<Integer> begin = null;
					if(def instanceof Edge<?>)
						begin = ((Edge<Integer>) def).getBeginNode();
					else if(def instanceof Node<?>)
						begin = ((Node<Integer>) def);
					if(begin == path.from()) 
						for(Object use : uses) {
							Node<Integer> end = null;
							if(use instanceof Node<?>) {
								end = ((Node<Integer>) use);
								if(end == path.to()) 
									paths.add(path);
							} else if(use instanceof Edge<?>) {
								end = ((Edge<Integer>) use).getEndNode();
								if(path.isEdgeOfPath((Edge<Integer>) use) && end == path.to() )
									paths.add(path);
							}
						}
				}
				break;
		}
	}

	public Set<AbstractPath<Integer>> getTestRequirementsToNode(Object obj) {
		return nodeedgeTestRequirements.get(obj);
	}
	
	public Set<AbstractPath<Integer>> getTestRequirementsToVariable(String str) {
		return variableTestRequirements.get(str);
	}
}