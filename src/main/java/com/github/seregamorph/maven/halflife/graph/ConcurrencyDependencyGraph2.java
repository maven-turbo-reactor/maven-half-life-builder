package com.github.seregamorph.maven.halflife.graph;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.maven.execution.ProjectDependencyGraph;
import org.apache.maven.lifecycle.internal.ProjectBuildList;
import org.apache.maven.lifecycle.internal.ProjectSegment;
import org.apache.maven.project.DuplicateProjectException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.dag.CycleDetectedException;

/**
 * Based on org.apache.maven.lifecycle.internal.builder.multithreaded.ConcurrencyDependencyGraph from maven-core.
 *
 * @author Sergey Chernov
 */
public class ConcurrencyDependencyGraph2 {

    private final Set<ProjectPartKey> finishedProjects = new HashSet<>();

    private final ProjectBuildList projectBuilds;
    private final ProjectDependencyGraph2 projectDependencyGraph;

    public ConcurrencyDependencyGraph2(
        ProjectBuildList projectBuilds,
        ProjectDependencyGraph projectDependencyGraph
    ) throws CycleDetectedException, DuplicateProjectException {
        this.projectDependencyGraph = getProjectDependencyGraph2(projectDependencyGraph);
        this.projectBuilds = projectBuilds;
    }

    private static FilteredProjectDependencyGraph2 getProjectDependencyGraph2(
        ProjectDependencyGraph projectDependencyGraph
    ) throws CycleDetectedException, DuplicateProjectException {
        List<MavenProject> allProjects = projectDependencyGraph.getAllProjects();
        DefaultProjectDependencyGraph2 defaultProjectDependencyGraph2 = new DefaultProjectDependencyGraph2(allProjects);
        List<MavenProject> sortedProjects = projectDependencyGraph.getSortedProjects();
        return new FilteredProjectDependencyGraph2(defaultProjectDependencyGraph2, sortedProjects);
    }

    public int getNumberOfBuilds() {
        return projectBuilds.size();
    }

    public List<MavenProject> getRootSchedulableBuilds() {
        Set<MavenProject> result = new LinkedHashSet<>();
        for (ProjectSegment projectBuild : projectBuilds) {
            List<MavenProject> upstreamProjects =
                projectDependencyGraph.getUpstreamProjects(projectBuild.getProject(), false);
            if (upstreamProjects.isEmpty()) {
                result.add(projectBuild.getProject());
            }
        }
        if (result.isEmpty() && !projectBuilds.isEmpty()) {
            result.add(projectBuilds.get(0).getProject());
        }
        return new ArrayList<>(result);
    }

    public List<MavenProject> markAsFinished(MavenProject mavenProject) {
        finishedProjects.add(new ProjectPartKey(mavenProject));
        return getSchedulableNewProcesses(mavenProject);
    }

    private List<MavenProject> getSchedulableNewProcesses(MavenProject finishedProject) {
        List<MavenProject> result = new ArrayList<>();
        for (MavenProject dependentProject : projectDependencyGraph.getDownstreamProjects(finishedProject, false)) {
            // todo List<ProjectKey>
            List<MavenProject> upstreamProjects = projectDependencyGraph.getUpstreamProjects(dependentProject, false);
            if (finishedProjects.containsAll(upstreamProjects.stream().map(ProjectPartKey::new).collect(Collectors.toList()))) {
                result.add(dependentProject);
            }
        }
        return result;
    }
}
