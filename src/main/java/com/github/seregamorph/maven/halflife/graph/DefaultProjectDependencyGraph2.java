package com.github.seregamorph.maven.halflife.graph;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.maven.project.DuplicateProjectException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.dag.CycleDetectedException;

/**
 * Based on org.apache.maven.graph.DefaultProjectDependencyGraph from maven-core.
 *
 * @author Sergey Chernov
 */
public class DefaultProjectDependencyGraph2 implements ProjectDependencyGraph2 {

    private final ProjectSorter2 sorter;
    private final Map<MavenProjectPart, Integer> order;
    private final Map<String, MavenProjectPart> projects;

    public DefaultProjectDependencyGraph2(Collection<MavenProject> projects) throws CycleDetectedException, DuplicateProjectException {
        this.sorter = new ProjectSorter2(projects);
        List<MavenProject> sorted = this.sorter.getSortedProjects();
        this.order = new HashMap<>(sorted.size());
        this.projects = new HashMap<>(sorted.size());
        int index = 0;
        for (MavenProject project : sorted) {
            MavenProjectPart projectPart = new MavenProjectPart(project);
            String id = projectPart.toString();
            this.projects.put(id, projectPart);
            this.order.put(projectPart, index++);
        }
    }

    @Override
    public List<MavenProjectPart> getDirectDownstreamProjects(MavenProjectPart projectPart) {
        Set<String> projectIds = new HashSet<>(sorter.getDependents(projectPart.toString()));
        return getSortedProjects(projectIds);
    }

    @Override
    public List<MavenProjectPart> getDirectUpstreamProjects(MavenProjectPart projectPart) {
        Set<String> projectIds = new HashSet<>(sorter.getDependencies(projectPart.toString()));
        return getSortedProjects(projectIds);
    }

    private List<MavenProjectPart> getSortedProjects(Set<String> projectIds) {
        List<MavenProjectPart> result = new ArrayList<>(projectIds.size());
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

    private class MavenProjectComparator implements Comparator<MavenProjectPart> {
        @Override
        public int compare(MavenProjectPart o1, MavenProjectPart o2) {
            return order.get(o1) - order.get(o2);
        }
    }
}
