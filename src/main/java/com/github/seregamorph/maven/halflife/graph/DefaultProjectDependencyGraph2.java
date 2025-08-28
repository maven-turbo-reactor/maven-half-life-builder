package com.github.seregamorph.maven.halflife.graph;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.apache.maven.project.DuplicateProjectException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectSorter;
import org.codehaus.plexus.util.dag.CycleDetectedException;

/**
 * Based on org.apache.maven.graph.DefaultProjectDependencyGraph from maven-core.
 *
 * @author Sergey Chernov
 */
public class DefaultProjectDependencyGraph2 implements ProjectDependencyGraph2 {

    private final ProjectSorter2 sorter;
    private final Map<MavenProject, Integer> order;
    private final Map<String, MavenProject> projects;

    public DefaultProjectDependencyGraph2(Collection<MavenProject> projects) throws CycleDetectedException, DuplicateProjectException {
        this.sorter = new ProjectSorter2(projects);
        List<MavenProject> sorted = this.sorter.getSortedProjects();
        this.order = new HashMap<>(sorted.size());
        this.projects = new HashMap<>(sorted.size());
        int index = 0;
        for (MavenProject project : sorted) {
            String id = ProjectSorter.getId(project);
            this.projects.put(id, project);
            this.order.put(project, index++);
        }
    }

    @Override
    public List<MavenProject> getDirectDownstreamProjects(MavenProject project) {
        Objects.requireNonNull(project, "project cannot be null");
        Set<String> projectIds = new HashSet<>(sorter.getDependents(ProjectSorter.getId(project)));
        return getSortedProjects(projectIds);
    }

    @Override
    public List<MavenProject> getDirectUpstreamProjects(MavenProject project) {
        Objects.requireNonNull(project, "project cannot be null");
        Set<String> projectIds = new HashSet<>(sorter.getDependencies(ProjectSorter.getId(project)));
        return getSortedProjects(projectIds);
    }

    private List<MavenProject> getSortedProjects(Set<String> projectIds) {
        List<MavenProject> result = new ArrayList<>(projectIds.size());
        for (String projectId : projectIds) {
            result.add(projects.get(projectId));
        }
        result.sort(new MavenProjectComparator());
        return result;
    }

    @Override
    public String toString() {
        return sorter.getSortedProjects().toString();
    }

    private class MavenProjectComparator implements Comparator<MavenProject> {
        @Override
        public int compare(MavenProject o1, MavenProject o2) {
            return order.get(o1) - order.get(o2);
        }
    }
}
