package domain.graph.visitors;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

import main.activator.Activator;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;

import ui.editor.Line;
import adt.graph.Edge;
import adt.graph.Node;
import domain.constants.Layer;
import domain.explorer.DefUsesStatementVisitor;

public class DefUsesVisitor<V extends Comparable<V>> extends DepthFirstGraphVisitor<Integer> {
	
	private Set<Node<Integer>> visitedNodes; // nodes must be visited just one time.
	private DefUsesStatementVisitor visitor;
	
	public DefUsesVisitor() {
		visitedNodes = new HashSet<Node<Integer>>();
	}

	@SuppressWarnings("unchecked")
	@Override
	public boolean visit(Node<Integer> node) {
		if(!visitedNodes.contains(node)) {
			graph.selectMetadataLayer(Layer.INSTRUCTIONS.getLayer());
			visitedNodes.add(node);
			HashMap<ASTNode, Line> nodeInstructions = (HashMap<ASTNode, Line>) graph.getMetadata(node);
			Set<String> defs = new TreeSet<String>();
			Set<String> uses = new TreeSet<String>();
			if(graph.isInitialNode(node))
				addToDefs(defs);
			visitor = new DefUsesStatementVisitor(defs, uses);
			if(nodeInstructions != null) {
				List<ASTNode> astNodes = getASTNodes(nodeInstructions);
				if(!isProgramStatement(astNodes))
					for(ASTNode ast : astNodes)
						ast.accept(visitor);
			}
			if(!defs.isEmpty() || !uses.isEmpty()) {
				List<String> defuses = getDefUses(defs, uses);
				Activator.getDefault().getDefUsesController().put(node, defuses);
			}
			return true;
		}
		return false;
	}	

	@SuppressWarnings("unchecked")
	@Override
	public boolean visit(Edge<Integer> edge) {
		HashMap<ASTNode, Line> nodeInstructions = (HashMap<ASTNode, Line>) graph.getMetadata(edge.getBeginNode());
		Set<String> defs = new TreeSet<String>();
		Set<String> uses = new TreeSet<String>();
		visitor = new DefUsesStatementVisitor(defs, uses);
		if(nodeInstructions != null) {
			List<ASTNode> astNodes = getASTNodes(nodeInstructions);
			if(isProgramStatement(astNodes))
				for(ASTNode ast : astNodes)
					ast.accept(visitor);
		}
		if(!defs.isEmpty() || !uses.isEmpty()) {
			List<String> defuses = getDefUses(defs, uses);
			Activator.getDefault().getDefUsesController().put(edge, defuses);
		}
		return false;
	}
	
	public List<String> getDefUses(Set<String> defsSet, Set<String> usesSet) {
		List<String> defuses = new LinkedList<String>();
		String defs = "";
		String uses = "";
		for(String str : defsSet)
			defs += " " + str + ",";
		if(defs.length() > 2)
			defs = defs.substring(0, defs.length() - 1);
		for(String str : usesSet)
			uses += " " + str + ",";
		if(uses.length() > 2)
			uses = uses.substring(0, uses.length() - 1);
		defuses.add(defs);
		defuses.add(uses);
		return defuses;
	}
	
	private List<ASTNode> getASTNodes(HashMap<ASTNode, Line> map) {
		List<ASTNode> nodes = new LinkedList<ASTNode>();
		for(Entry<ASTNode, Line> entry : map.entrySet()) 
	         nodes.add(entry.getKey());
		return nodes;
	}
	
	private boolean isProgramStatement(List<ASTNode> ast) {
		switch(ast.get(0).getNodeType()) {
			case ASTNode.IF_STATEMENT:
			case ASTNode.DO_STATEMENT:
			case ASTNode.FOR_STATEMENT:
			case ASTNode.ENHANCED_FOR_STATEMENT:
			case ASTNode.SWITCH_STATEMENT:
			case ASTNode.WHILE_STATEMENT:
				return true;
			default:
				return false;
		}
	}
	
	private void addToDefs(Set<String> defs) {
		List<SingleVariableDeclaration> params = Activator.getDefault().getSourceGraphController().getMethodParameters();
		for(SingleVariableDeclaration param : params) 
			defs.add(param.getName().toString());		
	}
}