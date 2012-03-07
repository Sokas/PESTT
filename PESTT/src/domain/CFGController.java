package domain;

import java.util.Observable;

import domain.constants.Layer;

public class CFGController extends Observable{
	
	private Layer layer;
	private boolean state;
	
	public void settLinkState(boolean state) {
		this.state = state;
		setChanged();
		notifyObservers(new LinkChangeEvent(state));
	}
	
	public boolean getLinkState() {
		return state;
	}
		
	public void selectLayer(String selected) {
		switch(Integer.parseInt(selected)) {
			case 1:
				layer = Layer.GUARDS;
				break;
			case 2:
				layer = Layer.INSTRUCTIONS;
				break;
			default:
				layer = Layer.EMPTY;;
		}
		setChanged();
		notifyObservers(new LayerChangeEvent(layer));
	}

	public Layer getLayer() {
		return layer;
	}
}
