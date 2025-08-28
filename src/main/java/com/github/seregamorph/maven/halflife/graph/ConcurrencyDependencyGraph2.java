package com.github.seregamorph.maven.halflife.graph;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
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

    private final Set<MavenProjectPart> finishedProjects = new HashSet<>();

    private final ProjectBuildList projectBuilds;
    private final ProjectDependencyGraph2 projectDependencyGraph;

    public ConcurrencyDependencyGraph2(
        ProjectBuildList projectBuilds,
        ProjectDependencyGraph projectDependencyGraph
    ) throws CycleDetectedException, DuplicateProjectException {
        this.projectBuilds = projectBuilds;
        this.projectDependencyGraph = getProjectDependencyGraph2(projectDependencyGraph);
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

    public List<MavenProjectPart> getRootSchedulableBuilds() {
        Set<MavenProjectPart> result = new LinkedHashSet<>();
        for (ProjectSegment projectBuild : projectBuilds) {
            MavenProjectPart mainProjectPart = new MavenProjectPart(projectBuild.getProject());
            List<MavenProjectPart> upstreamProjects = projectDependencyGraph.getDirectUpstreamProjects(mainProjectPart);
            if (upstreamProjects.isEmpty()) {
                result.add(mainProjectPart);
            }
        }
        if (result.isEmpty() && !projectBuilds.isEmpty()) {
            // todo ProjectPart.MAIN
            result.add(new MavenProjectPart(projectBuilds.get(0).getProject()));
        }
        return new ArrayList<>(result);
    }

    public List<MavenProjectPart> markAsFinished(MavenProjectPart mavenProjectPart) {
        finishedProjects.add(mavenProjectPart);
        return getSchedulableNewProcesses(mavenProjectPart);
    }

    private List<MavenProjectPart> getSchedulableNewProcesses(MavenProjectPart finishedProjectPart) {
        List<MavenProjectPart> result = new ArrayList<>();
        for (MavenProjectPart dependentProjectPart : projectDependencyGraph.getDirectDownstreamProjects(finishedProjectPart)) {
            List<MavenProjectPart> upstreamProjects = projectDependencyGraph.getDirectUpstreamProjects(dependentProjectPart);
            if (finishedProjects.containsAll(upstreamProjects)) {
                result.add(dependentProjectPart);
            }
        }
        return result;
    }
}
