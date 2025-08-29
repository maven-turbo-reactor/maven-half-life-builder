package com.github.seregamorph.maven.halflife.graph;

import java.util.List;

/**
 * Based on {@link org.apache.maven.execution.ProjectDependencyGraph} from maven-core.
 *
 * @author Sergey Chernov
 */
public interface ProjectDependencyGraph2 {

    List<MavenProjectPart> getDirectDownstreamProjects(MavenProjectPart projectPart);

    List<MavenProjectPart> getDirectUpstreamProjects(MavenProjectPart projectPart);
}
