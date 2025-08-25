package com.github.seregamorph.maven.halflife.graph;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
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

    private final ProjectSorter sorter;
    private final List<MavenProject> allProjects;
    private final Map<MavenProject, Integer> order;
    private final Map<String, MavenProject> projects;

    public DefaultProjectDependencyGraph2(Collection<MavenProject> projects) throws CycleDetectedException, DuplicateProjectException {
        this.allProjects = Collections.unmodifiableList(new ArrayList<>(projects));
        this.sorter = new ProjectSorter(projects);
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
    public List<MavenProject> getAllProjects() {
        return this.allProjects;
    }

    @Override
    public List<MavenProject> getSortedProjects() {
        return new ArrayList<>(sorter.getSortedProjects());
    }

    @Override
    public List<MavenProject> getDownstreamProjects(MavenProject project, boolean transitive) {
        Objects.requireNonNull(project, "project cannot be null");
        Set<String> projectIds = new HashSet<>();
        getDownstreamProjects(ProjectSorter.getId(project), projectIds, transitive);
        return getSortedProjects(projectIds);
    }

    private void getDownstreamProjects(String projectId, Set<String> projectIds, boolean transitive) {
        for (String id : sorter.getDependents(projectId)) {
            if (projectIds.add(id) && transitive) {
                getDownstreamProjects(id, projectIds, transitive);
            }
        }
    }

    @Override
    public List<MavenProject> getUpstreamProjects(MavenProject project, boolean transitive) {
        Objects.requireNonNull(project, "project cannot be null");

        Set<String> projectIds = new HashSet<>();

        getUpstreamProjects(ProjectSorter.getId(project), projectIds, transitive);

        return getSortedProjects(projectIds);
    }

    private void getUpstreamProjects(String projectId, Collection<String> projectIds, boolean transitive) {
        for (String id : sorter.getDependencies(projectId)) {
            if (projectIds.add(id) && transitive) {
                getUpstreamProjects(id, projectIds, transitive);
            }
        }
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
