package coveragealgorithms;

import graphvisitors.BreadthFirstGraphVisitor;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import sourcegraph.Edge;
import sourcegraph.Graph;
import sourcegraph.InfinitePath;
import sourcegraph.Node;
import sourcegraph.Path;

public class CompletePathCoverage<V> extends BreadthFirstGraphVisitor<V> implements ICoverageAlgorithms<V> {

	private List<Path<V>> paths;
	private List<Path<V>> temp;
	private List<Path<V>> completePaths;
	
	private Graph<V> graph;

	public CompletePathCoverage() {
		paths = new LinkedList<Path<V>>();
		temp = new LinkedList<Path<V>>();
		completePaths = new LinkedList<Path<V>>();
	}

	@Override
	public boolean visit(Graph<V> graph) {
		this.graph = graph;
		for(Node<V> node : graph.getNodes())
			if(graph.isInitialNode(node))
				paths.add(new Path<V>(node));

		while(!paths.isEmpty()) {
			for(Path<V> path : paths)
				addNodes(path);

			paths.clear();
			for(Path<V> path : temp)
				paths.add(path);
				temp.clear();
		}
		insertSpecialPaths();
		return true;
	}
	
	@SuppressWarnings("unchecked")
	private void insertSpecialPaths() {
		Map<Integer, InfinitePath<V>> replace = new LinkedHashMap<Integer, InfinitePath<V>>();
		for(Path<V> path : completePaths) {
			List<Integer> index = new ArrayList<Integer>();
			for(Node<V> node : path) {
				if(getNumberOfOccurrences(path, node) == 2) {
					int i = getIndexSpecialNode(path, node);
					if(!index.contains(i))
						index.add(i);
				}
			}
			if(!index.isEmpty()) {
				InfinitePath<V> infinite = null;
				for(Node<V> node : path)
					if(infinite == null)
						infinite = new InfinitePath<V>(node);
					else
						infinite.addNode(node);
				Node<Integer> n = new Node<Integer>(-1);
				for(int x : index)
					infinite.addNode(x, (Node<V>) n);
						
				replace.put(completePaths.indexOf(path), infinite);
			}			
		}
		Set<Entry<Integer, InfinitePath<V>>> set = replace.entrySet(); // the node properties.
		Iterator<Entry<Integer, InfinitePath<V>>> iterator = set.iterator(); 
		while(iterator.hasNext()) {
			Entry<Integer, InfinitePath<V>> entry = iterator.next();
			int i = entry.getKey();
			completePaths.remove(i);
			completePaths.add(entry.getKey(), entry.getValue());
		}
	}

	private int getIndexSpecialNode(Path<V> path, Node<V> node) {
		int index = -1;
		for(int i = 0; i < path.getPathNodes().size(); i++) {
			Node<V> n = path.getPathNodes().get(i);
			if(n == node)
				index = i;
		}
		return index;
	}

	private void addNodes(Path<V> path) {
		Node<V> finalNode = path.getPathNodes().get(path.getPathNodes().size() - 1);
		for(Edge<V> edge : graph.getNodeEdges(finalNode)) {
			Path<V> aux = path.clone();
			aux.addNode(edge.getEndNode());
			if(graph.isFinalNode(edge.getEndNode())) 
				completePaths.add(aux);
			else if(!isLop(aux))
				temp.add(aux);
		}
	}
	
	public boolean isLop(Path<V> path) {
		for(Node<V> node : path) 
			if(getNumberOfOccurrences(path, node) > 2)
				return true;
		return false;
		
	}

	private int getNumberOfOccurrences(Path<V> path, Node<V> node) {
		int occurrences = 0;
		for(Node<V> n : path)
			if(n == node)
				occurrences++;
		return occurrences;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public List<Path<V>> getTestRequirements() {
		completePaths = new SortPaths().sort(completePaths); // sort the paths.
		return completePaths;
	}

}