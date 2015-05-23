/*
 * #%L
 * Maven Integration for Eclipse CDT
 * %%
 * Copyright (C) 2014 Stephen Edwards
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
 */
package com.github.sdedwards.m2e_nar.internal;

import java.io.File;
import java.util.Set;

import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.project.MavenProject;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.project.configurator.MojoExecutionBuildParticipant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NarBuildParticipant extends MojoExecutionBuildParticipant {

	private static final Logger logger = LoggerFactory.getLogger(CProjectConfigurator.class);

	public NarBuildParticipant(MojoExecution execution, boolean runOnIncremental, boolean runOnConfiguration) {
		super(execution, runOnIncremental, runOnConfiguration);
	}

	@Override
	public Set<IProject> build(int kind, IProgressMonitor monitor) throws Exception {
		logger.info("Build kind=" + kind + " for execution " + getMojoExecution().getExecutionId());
		Set<IProject> retVal = null;
		if (appliesToBuildKind(kind)) {
			retVal = super.build(kind, monitor);
			MavenProject project = getMavenProjectFacade().getMavenProject(monitor);
			File generated = MavenPlugin.getMaven().getMojoParameterValue(project, getMojoExecution(), "outputDirectory", File.class, monitor);
			if (generated != null) {
				getBuildContext().refresh(generated);
			}
		}
		return retVal;
	}

}
