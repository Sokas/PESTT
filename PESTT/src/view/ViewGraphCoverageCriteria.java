package view;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.part.ViewPart;

public class ViewGraphCoverageCriteria extends ViewPart  {

	private Composite parent;
	
	@Override
	public void createPartControl(Composite parent) {
		this.parent = parent;
	}
	
	@Override
	public void setFocus() {
	}

	public void create() {
		for(Control control : parent.getChildren()) // dispose all active controls.
			control.dispose();
		GraphsCreator.INSTANCE.createCoverageCriteriaGraph(parent); // create the new viewGraph.
	}
}