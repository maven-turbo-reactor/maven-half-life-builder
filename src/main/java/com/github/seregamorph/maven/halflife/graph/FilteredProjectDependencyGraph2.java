package com.github.seregamorph.maven.halflife.graph;

import java.util.ArrayList;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import org.apache.maven.project.MavenProject;

/**
 * Based on {@link org.apache.maven.graph.FilteredProjectDependencyGraph} from maven-core.
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
    public List<MavenProjectPart> getDirectDownstreamProjects(MavenProjectPart projectPart) {
        return applyFilter(projectDependencyGraph.getDirectDownstreamProjects(projectPart));
    }

    @Override
    public List<MavenProjectPart> getDirectUpstreamProjects(MavenProjectPart projectPart) {
        return applyFilter(projectDependencyGraph.getDirectUpstreamProjects(projectPart));
    }

    private List<MavenProjectPart> applyFilter(Collection<? extends MavenProjectPart> projects) {
        List<MavenProjectPart> filtered = new ArrayList<>(projects.size());

        for (MavenProjectPart projectPart : projects) {
            if (whiteList.containsKey(projectPart.getProject())) {
                filtered.add(projectPart);
            }
        }

        return filtered;
    }
}
