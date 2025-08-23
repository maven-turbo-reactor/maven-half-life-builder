package com.github.seregamorph.maven.halflife.graph;

import java.util.List;
import org.apache.maven.project.MavenProject;

/**
 * Based on org.apache.maven.graph.ProjectDependencyGraph from maven-core.
 *
 * @author Sergey Chernov
 */
public interface ProjectDependencyGraph2 {

    List<MavenProject> getAllProjects();

    List<MavenProject> getSortedProjects();

    List<MavenProject> getDownstreamProjects(MavenProject project, boolean transitive);

    List<MavenProject> getUpstreamProjects(MavenProject project, boolean transitive);
}
