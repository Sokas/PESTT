package view;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.graphics.Image;

import activator.Activator;
import constants.Images_ID;

public class StatusImages {
	
	private List<Image> images; // the list of images.
	private Image PASS; // the pas image.
	private Image FAIL; // the fail image
	private Image CHECK; // the check image.
	private Image UNCHECK; // the uncheck image.

	public StatusImages() {
		PASS = Activator.getImageDescriptor(Images_ID.PASS_LOCATION).createImage(); // load the pass image.
		FAIL = Activator.getImageDescriptor(Images_ID.FAIL_LOCATION).createImage(); // load the fail image.
		CHECK = Activator.getImageDescriptor(Images_ID.CHECK_LOCATION).createImage(); // load the check image.
		UNCHECK = Activator.getImageDescriptor(Images_ID.UNCHECK_LOCATION).createImage(); // load the uncheck image.
		images = new ArrayList<Image>(); // create the new image list.
		images.add(PASS); // add pass image to the list.
		images.add(FAIL); // add fail image to the list.
		images.add(CHECK); // add check image to the list.
		images.add(UNCHECK); // add uncheck image to the list.
	}
	
	public List<Image> getImage() {
		return images;
	}	
}