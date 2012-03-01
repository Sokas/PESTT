package coverage;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.jacoco.core.analysis.Analyzer;
import org.jacoco.core.analysis.CoverageBuilder;
import org.jacoco.core.analysis.IClassCoverage;
import org.jacoco.core.data.ExecutionDataStore;
import org.jacoco.core.instr.Instrumenter;
import org.jacoco.core.runtime.IRuntime;
import org.jacoco.core.runtime.LoggerRuntime;

import editor.ActiveEditor;

public class CodeCoverage {

	private List<List<ICoverageData>> data;
	private ActiveEditor editor;


	public CodeCoverage(ActiveEditor editor) {
		data = new LinkedList<List<ICoverageData>>();
		this.editor = editor;
	}

	public List<List<ICoverageData>> getCodeCoverageStatus() {

		String targetName = editor.getLocation();
		try {				
			ActiveEditor testEditor = new ActiveEditor();
			Collection<String> methodsToRun = new ArrayList<String>();
			if(testEditor.isInMethod())
				methodsToRun.add(testEditor.getSelectedMethod());
			else
				methodsToRun = testEditor.getMethodNames();

			for(String method : methodsToRun) {
				// for instrumentation and runtime we need a IRuntime instance to collect execution data.
				final IRuntime runtime = new LoggerRuntime();

				// the Instrumenter creates a modified version of our target class that contains additional probes for execution data recording.
				final Instrumenter instr = new Instrumenter(runtime);
				final byte[] instrumented = instr.instrument(getTargetClass(editor));
				final byte[] instrumentedJUnitTest = instr.instrument(getTargetClass(testEditor));
				
				// class loader to directly load the instrumented class definition from a byte[] instances.
				final MemoryClassLoader memoryClassLoader = new MemoryClassLoader();
				memoryClassLoader.addDefinition(targetName, instrumented);
				memoryClassLoader.addDefinition(testEditor.getLocation(), instrumentedJUnitTest);
				final Class<?> classJUnit = memoryClassLoader.loadClass(testEditor.getLocation());

				// start the runtime to run the instrumented class.
				runtime.startup();
				
//				JUnitCore runner = new JUnitCore();
//				Request request = Request.method(classJUnit, method);
//				runner.run(request);
				
				Runner runner = new Runner();
				runner.setMethodToRun(classJUnit, method);
				runner.run();

				// collect execution data and shutdown the runtime.
				ExecutionDataStore executionData = new ExecutionDataStore();
				runtime.collect(executionData, null, false);
				runtime.shutdown();

				// together with the original class definition we can calculate coverage information.
				final CoverageBuilder coverageBuilder = new CoverageBuilder();
				final Analyzer analyzer = new Analyzer(executionData, coverageBuilder);
				analyzer.analyzeClass(getTargetClass(editor));

				List<ICoverageData> dataRun = new LinkedList<ICoverageData>();
				// line coverage information:
				for(final IClassCoverage classCoverage : coverageBuilder.getClasses()) {
					dataRun.add(new CoverageData(classCoverage));
				}
					
				data.add(dataRun);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}		
		return data;
	}

	private InputStream getTargetClass(ActiveEditor editor) throws FileNotFoundException {
		return new FileInputStream(editor.getClassFilePath());
	}
}