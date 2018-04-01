/*
 * #%L
 * Native ARchive plugin for Maven
 * %%
 * Copyright (C) 2002 - 2014 NAR Maven Plugin developers.
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 * 
 * 2014/09/18 Modified by Stephen Edwards:
 *  Make a public API for extracting NAR config
 */
package com.github.maven_nar;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.StringUtils;

/**
 * Abstract Compiler class
 * 
 * @author Mark Donszelmann
 */
public abstract class Compiler implements ICompiler {

  /**
   * The name of the compiler. Some choices are: "msvc", "g++", "gcc", "CC",
   * "cc", "icc", "icpc", ... Default is
   * Architecture-OS-Linker specific: FIXME: table missing
   */
  @Parameter
  private String name;

  /**
   * The prefix for the compiler.
   */
  @Parameter
  private String prefix;

  /**
   * Path location of the compile tool
   */
  @Parameter
  private String toolPath;

  /**
   * Source directory for native files
   */
  @Parameter(defaultValue = "${basedir}/src/main", required = true)
  private File sourceDirectory;

  /**
   * Source directory for native test files
   */
  @Parameter(defaultValue = "${basedir}/src/test", required = true)
  private File testSourceDirectory;

  /**
   * To use full path for the filenames.
   * false to have "relative" path
   * true to have "absolute" path
   * absolute: will give path from filesystem root "/"
   * relative: will give relative path from "workdir" which is usually after "${basedir}/src/main"
   */
  @Parameter(required = true)
  private boolean gccFileAbsolutePath = false;

  /**
   * Include patterns for sources
   */
  @Parameter(required = true)
  private Set<String> includes = new HashSet<String>();

  /**
   * Exclude patterns for sources
   */
  @Parameter(required = true)
  private Set<String> excludes = new HashSet<String>();

  /**
   * Include patterns for test sources
   */
  @Parameter(required = true)
  private Set<String> testIncludes = new HashSet<String>();

  /**
   * Exclude patterns for test sources
   */
  @Parameter(required = true)
  private Set<String> testExcludes = new HashSet<String>();

  @Parameter(defaultValue = "false", required = false)
  private boolean ccache = false;

  /**
   * Compile with debug information.
   */
  @Parameter(required = true)
  private boolean debug = false;

  /**
   * Enables generation of exception handling code.
   */
  @Parameter(defaultValue = "true", required = true)
  private boolean exceptions = true;

  /**
   * Enables run-time type information.
   */
  @Parameter(defaultValue = "true", required = true)
  private boolean rtti = true;

  /**
   * Sets optimization. Possible choices are: "none", "size", "minimal",
   * "speed", "full", "aggressive", "extreme",
   * "unsafe".
   */
  @Parameter(defaultValue = "none", required = true)
  private String optimize = "none";

  /**
   * Enables or disables generation of multi-threaded code. Default value:
   * false, except on Windows.
   */
  @Parameter(required = true)
  private boolean multiThreaded = false;

  /**
   * Defines
   */
  @Parameter
  private List<String> defines;

  /**
   * Defines for the compiler as a comma separated list of name[=value] pairs,
   * where the value is optional. Will work
   * in combination with &lt;defines&gt;.
   */
  @Parameter
  private String defineSet;

  /**
   * Clears default defines
   */
  @Parameter(required = true)
  private boolean clearDefaultDefines;

  /**
   * Undefines
   */
  @Parameter
  private List<String> undefines;

  /**
   * Undefines for the compiler as a comma separated list of name[=value] pairs
   * where the value is optional. Will work
   * in combination with &lt;undefines&gt;.
   */
  @Parameter
  private String undefineSet;

  /**
   * Clears default undefines
   */
  @Parameter
  private boolean clearDefaultUndefines;

  /**
   * Include Paths. Defaults to "${sourceDirectory}/include"
   */
  @Parameter
  private List<IncludePath> includePaths;

  /**
   * Test Include Paths. Defaults to "${testSourceDirectory}/include"
   */
  @Parameter
  private List<IncludePath> testIncludePaths;

  /**
   * System Include Paths, which are added at the end of all include paths
   */
  @Parameter
  private List<String> systemIncludePaths;

  /**
   * Additional options for the C++ compiler Defaults to Architecture-OS-Linker
   * specific values. FIXME table missing
   */
  @Parameter
  private List<String> options;

  /**
   * Additional options for the compiler when running in the nar-testCompile
   * phase.
   */
  @Parameter
  private List<String> testOptions;

  /**
   * Options for the compiler as a whitespace separated list. Will work in
   * combination with &lt;options&gt;.
   */
  @Parameter
  private String optionSet;

  /**
   * Clears default options
   */
  @Parameter(required = true)
  private boolean clearDefaultOptions;

  /**
   * Comma separated list of filenames to compile in order
   */
  @Parameter
  private String compileOrder;

	private AbstractCompileMojo mojo;

	public static final String MAIN = "main";
	public static final String TEST = "test";

	protected Compiler() {
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.github.maven_nar.ICompiler#getName()
	 */
	public String getName() throws MojoFailureException, MojoExecutionException {
		// adjust default values
		if (name == null) {
			name = mojo.getNarProperties().getProperty(getPrefix() + "compiler");
		}
		return name;
	}

	public final void setAbstractCompileMojo(AbstractCompileMojo mojo) {
		this.mojo = mojo;
	}

	public final List/* <File> */getSourceDirectories() {
		return getSourceDirectories("dummy");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.github.maven_nar.ICompiler#getSourceDirectories(java.lang.String)
	 */
	public List<File> getSourceDirectories(String type) {
		List sourceDirectories = new ArrayList();
		File baseDir = mojo.getMavenProject().getBasedir();

		if (type.equals(TEST)) {
			if (testSourceDirectory == null) {
				testSourceDirectory = new File(baseDir, "/src/test");
			}
			if (testSourceDirectory.exists()) {
				sourceDirectories.add(testSourceDirectory);
			}

			for (Iterator i = mojo.getMavenProject().getTestCompileSourceRoots().iterator(); i.hasNext();) {
				File extraTestSourceDirectory = new File((String) i.next());
				if (extraTestSourceDirectory.exists()) {
					sourceDirectories.add(extraTestSourceDirectory);
				}
			}
		} else {
			if (sourceDirectory == null) {
				sourceDirectory = new File(baseDir, "src/main");
			}
			if (sourceDirectory.exists()) {
				sourceDirectories.add(sourceDirectory);
			}

			for (Iterator i = mojo.getMavenProject().getCompileSourceRoots().iterator(); i.hasNext();) {
				File extraSourceDirectory = new File((String) i.next());
				if (extraSourceDirectory.exists()) {
					sourceDirectories.add(extraSourceDirectory);
				}
			}
		}

		if (mojo.getLog().isDebugEnabled()) {
			for (Iterator i = sourceDirectories.iterator(); i.hasNext();) {
				mojo.getLog().debug("Added to sourceDirectory: " + ((File) i.next()).getPath());
			}
		}
		return sourceDirectories;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.github.maven_nar.ICompiler#getIncludePaths(java.lang.String)
	 */
	public final List<IncludePath> getIncludePaths(String type) {
		List<IncludePath> includePathList = createIncludePaths(type, type.equals(TEST) ? testIncludePaths : includePaths);
		if (type.equals(TEST)) {
			// Add main includes paths too
			includePathList.addAll(createIncludePaths(MAIN, includePaths));
		}
		return includePathList;
	}

	private List<IncludePath> createIncludePaths(String type, List paths) {
		List includeList = paths;
		if (includeList == null || (paths.size() == 0)) {
			includeList = new ArrayList();
			for (Iterator i = getSourceDirectories(type).iterator(); i.hasNext();) {
				// VR 20100318 only add include directories that exist - we now
				// fail the build fast if an include directory does not exist
				File file = new File((File) i.next(), "include");
				if (file.isDirectory()) {
	                IncludePath includePath = new IncludePath();
	                includePath.setPath( file.getPath() );
					includeList.add(includePath);
				}
			}
		}
		return includeList;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.github.maven_nar.ICompiler#getSystemIncludePaths()
	 */
	public final List/* <String> */getSystemIncludePaths() {
		return systemIncludePaths;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.github.maven_nar.ICompiler#getIncludes()
	 */
	public final Set getIncludes() throws MojoFailureException, MojoExecutionException {
		return getIncludes("main");
	}

	public final Set getIncludes(String type) throws MojoFailureException, MojoExecutionException {
		Set result = new HashSet();
		if (!type.equals(TEST) && !includes.isEmpty()) {
			result.addAll(includes);
		} else if (type.equals(TEST) && !testIncludes.isEmpty()) {
			result.addAll(testIncludes);
		} else {
			String defaultIncludes = mojo.getNarProperties().getProperty(getPrefix() + "includes");
			if (defaultIncludes != null) {
				String[] include = defaultIncludes.split(" ");
				for (int i = 0; i < include.length; i++) {
					result.add(include[i].trim());
				}
			}
		}
		return result;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.github.maven_nar.ICompiler#getExcludes()
	 */
	public final Set getExcludes() throws MojoFailureException, MojoExecutionException {
		return getExcludes("main", null);
	}

	public final Set getExcludes(String type, ITest currentTest) throws MojoFailureException, MojoExecutionException {
		Set result = new HashSet();
		if (type.equals(TEST) && !testExcludes.isEmpty()) {
			result.addAll(testExcludes);
		} else if (!excludes.isEmpty()) {
			result.addAll(excludes);
		} else {
			String defaultExcludes = mojo.getNarProperties().getProperty(getPrefix() + "excludes");
			if (defaultExcludes != null) {
				String[] exclude = defaultExcludes.split(" ");
				for (int i = 0; i < exclude.length; i++) {
					result.add(exclude[i].trim());
				}
			}
		}
		// now add all but the current test to the excludes
		String testName = null;
		if (currentTest != null) {
			testName = currentTest.getName();
		}
		for (Iterator i = mojo.getTests().iterator(); i.hasNext();) {
			Test test = (Test) i.next();
			if (!test.getName().equals(testName)) {
				result.add("**/" + test.getName() + ".*");
			}
		}

		return result;
	}

	protected final String getPrefix() throws MojoFailureException, MojoExecutionException {
		return mojo.getAOL().getKey() + "." + getLanguage() + ".";
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.github.maven_nar.ICompiler#isDebug()
	 */
	public boolean isDebug() {
		return debug;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.github.maven_nar.ICompiler#isRtti()
	 */
	public boolean isRtti() {
		return rtti;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.github.maven_nar.ICompiler#getOptimize()
	 */
	public String getOptimize() {
		return optimize;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.github.maven_nar.ICompiler#isMultiThreaded()
	 */
	public boolean isMultiThreaded() {
		return mojo.getOS().equals("Windows") ? true : multiThreaded;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.github.maven_nar.ICompiler#getDefines()
	 */
	public List getDefines() throws MojoFailureException, MojoExecutionException {
		List defineList = new ArrayList();
		if (defines != null) {
			defineList.addAll(defines);
		}

		if (defineSet != null) {

			String[] defList = defineSet.split(",");

			for (int i = 0; i < defList.length; i++) {
				defineList.add(defList[i].trim());
			}
		}

		if (!clearDefaultDefines) {
			String defaultDefines = mojo.getNarProperties().getProperty(getPrefix() + "defines");
			if (defaultDefines != null) {
				String[] define = new NarUtil.StringArrayBuilder(defaultDefines).getValue();
				for (int i = 0; i < define.length; i++) {
					defineList.add(define[i]);
				}
			}
		}
		return defineList;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.github.maven_nar.ICompiler#getUndefines()
	 */
	public List getUndefines() throws MojoFailureException, MojoExecutionException {
		List undefineList = new ArrayList();
		if (undefines != null) {
			undefineList.addAll(undefines);
		}

		if (undefineSet != null) {

			String[] undefList = undefineSet.split(",");

			for (int i = 0; i < undefList.length; i++) {
				undefineList.add(undefList[i].trim());
			}
		}

		if (!clearDefaultUndefines) {
			String defaultUndefines = mojo.getNarProperties().getProperty(getPrefix() + "undefines");
			if (defaultUndefines != null) {
				String[] undefine = new NarUtil.StringArrayBuilder(defaultUndefines).getValue();
				for (int i = 0; i < undefine.length; i++) {
					undefineList.add(undefine[i]);
				}
			}
		}
		return undefineList;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.github.maven_nar.ICompiler#getOptions()
	 */
	public List getOptions() throws MojoFailureException, MojoExecutionException {
		List optionList = new ArrayList();
		if (options != null) {
			optionList.addAll(options);
		}

		if (optionSet != null) {

			String[] opts = optionSet.split("\\s");

			for (int i = 0; i < opts.length; i++) {
				optionList.add(opts[i]);
			}
		}

		if (!clearDefaultOptions) {
			String optionsProperty = mojo.getNarProperties().getProperty(getPrefix() + "options");
			if (optionsProperty != null) {
				String[] option = optionsProperty.split(" ");
				for (int i = 0; i < option.length; i++) {
					optionList.add(option[i]);
				}
			}
		}
		return optionList;
	}
	
	public List getTestOptions() {
		return testOptions;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.github.maven_nar.ICompiler#isExceptions()
	 */
	public boolean isExceptions() {
		return exceptions;
	}

	protected abstract String getLanguage();

	public boolean isClearDefaultOptions() {
		return clearDefaultOptions;
	}

}
