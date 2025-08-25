package com.github.seregamorph.maven.halflife.graph;

import java.util.List;
import org.apache.maven.project.MavenProject;

/**
 * Based on org.apache.maven.graph.ProjectDependencyGraph from maven-core.
 *
 * @author Sergey Chernov
 */
public interface ProjectDependencyGraph2 {

    List<MavenProject> getDirectDownstreamProjects(MavenProject project);

    List<MavenProject> getDirectUpstreamProjects(MavenProject project);
}
