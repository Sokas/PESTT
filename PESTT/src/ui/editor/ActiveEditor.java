package ui.editor;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageDeclaration;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Javadoc;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.TagElement;
import org.eclipse.jdt.core.dom.TextElement;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.ITextEditor;

import ui.constants.Description;
import domain.constants.JavadocTagAnnotations;

public class ActiveEditor {

	private IEditorPart part;
	private ITextSelection textSelect; // text selected in editor.
	private IFile file; // the current open file.
	private Markers marker; // marker to add.
	private ICompilationUnit compilationUnit;
	private IJavaProject javaProject;

	public ActiveEditor() {
		part = PlatformUI.getWorkbench().getActiveWorkbenchWindow()
				.getActivePage().getActiveEditor();
		ITextEditor editor = (ITextEditor) part; // obtain the text editor.
		ISelection select = editor.getSelectionProvider().getSelection(); // the
																			// selected
																			// text.
		textSelect = (ITextSelection) select; // get the text selected.
		file = (IFile) part.getEditorInput().getAdapter(IFile.class); // get the
																		// file
		marker = new Markers(file);
		IProject project = file.getProject();
		try {
			if (project.isNatureEnabled("org.eclipse.jdt.core.javanature")) {
				javaProject = JavaCore.create(project);
				compilationUnit = JavaCore.createCompilationUnitFrom(file);
			}
		} catch (CoreException e) {
			e.printStackTrace();
		}
	}

	public void createMarker(String markerType, int offset, int length) {
		PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage()
				.bringToTop(part);
		marker.createMarks(markerType, offset, length);
	}

	public void removeALLMarkers() {
		marker.deleteAllMarkers();
	}

	public String getProjectName() {
		return javaProject.getElementName();
	}

	public String getPackageName() {
		IPackageDeclaration[] pd;
		try {
			pd = compilationUnit.getPackageDeclarations();
			if (pd.length != 0)
				return pd[0].getElementName();
		} catch (JavaModelException e) {
			e.printStackTrace();
		}
		return "";
	}

	public List<String> getMethodNames() {
		List<String> methodNames = new LinkedList<String>();
		try {
			for (IType type : compilationUnit.getAllTypes())
				for (IMethod method : type.getMethods())
					methodNames.add(method.getElementName());
		} catch (JavaModelException e) {
			e.printStackTrace();
		}
		return methodNames;
	}

	public String getSelectedMethod() {
		try {
			for(IType type : compilationUnit.getAllTypes())
				for(IMethod method : type.getMethods()) {
					int cursorPosition = textSelect.getOffset();
					int methodStart = method.getSourceRange().getOffset();
					int methodEnd = method.getSourceRange().getOffset()
							+ method.getSourceRange().getLength();
					if (methodStart <= cursorPosition
							&& cursorPosition <= methodEnd)
						return method.getElementName();
				}
		} catch (JavaModelException e) {
			e.printStackTrace();
		}
		return null;
	}

	public boolean isInMethod() {
		if (getSelectedMethod() != null)
			return true;
		return false;
	}

	public String getClassName() {
		return compilationUnit.getElementName().substring(0,
				compilationUnit.getElementName().length() - 5);
	}

	public String getLocation() {
		if (!getPackageName().equals(Description.EMPTY))
			return getPackageName() + "." + getClassName();
		else
			return getClassName();
	}

	public String getClassFilePath() {
		try {
			String outputFolder = javaProject
					.getOutputLocation()
					.toOSString()
					.substring(
							getProjectName().length() + 2,
							javaProject.getOutputLocation().toOSString()
									.length());
			return javaProject.getResource().getLocation().toOSString()
					+ IPath.SEPARATOR + outputFolder + IPath.SEPARATOR
					+ getPackageName() + IPath.SEPARATOR + getClassName()
					+ ".class";
		} catch (JavaModelException e) {
			e.printStackTrace();
			return "";
		}
	}

	public ICompilationUnit getCompilationUnit() {
		return compilationUnit;
	}

	
	public void addJavadocTagAnnotation(CompilationUnit unit, MethodDeclaration method, JavadocTagAnnotations tagAnnotation, String input) {	
		if(tagAnnotation.getTag().equals(JavadocTagAnnotations.COVERAGE_CRITERIA.getTag()))
			removeJavadocTagAnnotation(unit, method, tagAnnotation, input); 
		Javadoc javadoc = method.getJavadoc();
		if(javadoc == null) {
			javadoc = method.getAST().newJavadoc();
			method.setJavadoc(javadoc);
		}
		createTag(method, tagAnnotation, input, javadoc);
		sortJavadocTagAnnotation(method, javadoc);
		applychanges(unit);
	}

	@SuppressWarnings("unchecked")
	private void createTag(MethodDeclaration method, JavadocTagAnnotations tagAnnotation, String input, Javadoc javadoc) {
		TagElement newTag = method.getAST().newTagElement();
		newTag.setTagName(tagAnnotation.getTag());
		TextElement newText = method.getAST().newTextElement();
		newText.setText(input);
		newTag.fragments().add(newText);
		javadoc.tags().add(newTag);
	}

	@SuppressWarnings("unchecked")
	public void removeJavadocTagAnnotation(CompilationUnit unit, MethodDeclaration method, JavadocTagAnnotations tagAnnotation, String input) {	
		Javadoc javadoc = method.getJavadoc();
		if(javadoc != null) {
			List<TagElement> tags = (List<TagElement>) javadoc.tags();
			int index = -1;
			for(TagElement tag : tags) 
				if(tag.getTagName().equals(tagAnnotation.getTag()))
					if(tag.fragments().get(0).toString().equals(" " + input))
						index = tags.indexOf(tag);
			if(index != -1)
				tags.remove(index);
			applychanges(unit);
		}
	}
	
	public void cleanJavadocTagAnnotation(CompilationUnit unit, MethodDeclaration method) {
		if(method.getJavadoc() != null) {
			method.getJavadoc().delete();
			applychanges(unit);
		}
	}
	
	@SuppressWarnings("unchecked")
	private void sortJavadocTagAnnotation(MethodDeclaration method, Javadoc javadoc) {
		String criteria = Description.EMPTY;
		Set<String> testPath = new TreeSet<String>(); 
		Set<String> testRequirements = new TreeSet<String>();
		Set<String> infeasibles = new TreeSet<String>();
		List<TagElement> tags = (List<TagElement>) javadoc.tags();
		for(TagElement tag : tags) 
			if(JavadocTagAnnotations.COVERAGE_CRITERIA.getTag().equals(tag.getTagName())) {
				 criteria = tag.fragments().get(0).toString();
				 criteria = criteria.substring(0, criteria.length());
			} else if(JavadocTagAnnotations.INFEASIBLE_PATH.getTag().equals(tag.getTagName())) {
				String str = tag.fragments().get(0).toString();
				str = str.substring(1, str.length());
				infeasibles.add(str);
			} else if(JavadocTagAnnotations.ADDITIONAL_TEST_REQUIREMENT_PATH.getTag().equals(tag.getTagName())) {
				String str = tag.fragments().get(0).toString();
				str = str.substring(1, str.length());
				testRequirements.add(str);
			} else if(JavadocTagAnnotations.ADDITIONAL_TEST_PATH.getTag().equals(tag.getTagName())) {
				String str = tag.fragments().get(0).toString();
				str = str.substring(1, str.length());
				testPath.add(str);
			}
		javadoc.delete();
		javadoc = method.getAST().newJavadoc();
		method.setJavadoc(javadoc);
		createTag(method, JavadocTagAnnotations.COVERAGE_CRITERIA, criteria, javadoc);
		for(String str : infeasibles)
			createTag(method, JavadocTagAnnotations.INFEASIBLE_PATH, str, javadoc);
		for(String str : testRequirements)
			createTag(method, JavadocTagAnnotations.ADDITIONAL_TEST_REQUIREMENT_PATH, str, javadoc);
		for(String str : testPath)
			createTag(method, JavadocTagAnnotations.ADDITIONAL_TEST_PATH, str, javadoc);
	}
	
	private void applychanges(CompilationUnit unit) {
		ITextEditor editor = (ITextEditor) part;
		IDocument document = editor.getDocumentProvider().getDocument(editor.getEditorInput());
		TextEdit edit = unit.rewrite(document, javaProject.getOptions(true));
		try {
			edit.apply(document);
		} catch (MalformedTreeException e) {
			e.printStackTrace(); 
		} catch (BadLocationException e) {
			e.printStackTrace(); 
		}
	}
}