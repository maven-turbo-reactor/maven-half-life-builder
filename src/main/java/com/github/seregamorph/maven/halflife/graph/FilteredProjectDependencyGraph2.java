package com.github.seregamorph.maven.halflife.graph;

import java.util.ArrayList;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import org.apache.maven.project.MavenProject;

/**
 * Based on original org.apache.maven.graph.FilteredProjectDependencyGraph from maven-core.
 *
 * @author Sergey Chernov
 */
class FilteredProjectDependencyGraph2 implements ProjectDependencyGraph2 {

    private final ProjectDependencyGraph2 projectDependencyGraph;
    private final Map<MavenProject, ?> whiteList;

    FilteredProjectDependencyGraph2(
        ProjectDependencyGraph2 projectDependencyGraph,
        Collection<? extends MavenProject> whiteList
    ) {
        this.projectDependencyGraph = projectDependencyGraph;

        this.whiteList = new IdentityHashMap<>();

        for (MavenProject project : whiteList) {
            this.whiteList.put(project, null);
        }
    }

    @Override
    public List<MavenProject> getDownstreamProjects(MavenProject project, boolean transitive) {
        return applyFilter(projectDependencyGraph.getDownstreamProjects(project, transitive));
    }

    @Override
    public List<MavenProject> getUpstreamProjects(MavenProject project, boolean transitive) {
        return applyFilter(projectDependencyGraph.getUpstreamProjects(project, transitive));
    }

    private List<MavenProject> applyFilter(Collection<? extends MavenProject> projects) {
        List<MavenProject> filtered = new ArrayList<>(projects.size());

        for (MavenProject project : projects) {
            if (whiteList.containsKey(project)) {
                filtered.add(project);
            }
        }

        return filtered;
    }
}
