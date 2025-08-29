package com.github.seregamorph.maven.halflife.graph;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.apache.maven.execution.ProjectDependencyGraph;
import org.apache.maven.lifecycle.internal.ProjectBuildList;
import org.apache.maven.project.DuplicateProjectException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.dag.CycleDetectedException;

/**
 * Based on {@link org.apache.maven.lifecycle.internal.builder.multithreaded.ConcurrencyDependencyGraph} from
 * maven-core.
 *
 * @author Sergey Chernov
 */
public class ConcurrencyDependencyGraph2 {

    private final Set<MavenProjectPart> finishedProjects = new HashSet<>();

    private final List<MavenProject> projectsToBuild;
    private final ProjectDependencyGraph2 projectDependencyGraph;

    public ConcurrencyDependencyGraph2(
        ProjectBuildList projectBuilds,
        ProjectDependencyGraph projectDependencyGraph
    ) throws CycleDetectedException, DuplicateProjectException {
        this(projects(projectBuilds), getProjectDependencyGraph2(projectDependencyGraph));
    }

    ConcurrencyDependencyGraph2(
        List<MavenProject> projectsToBuild,
        ProjectDependencyGraph2 projectDependencyGraph
    ) {
        this.projectsToBuild = projectsToBuild;
        this.projectDependencyGraph = projectDependencyGraph;
    }

    private static List<MavenProject> projects(ProjectBuildList projectBuilds) {
        List<MavenProject> projects = new ArrayList<>();
        projectBuilds.forEach(build -> projects.add(build.getProject()));
        return projects;
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
        return projectsToBuild.size();
    }

    public List<MavenProjectPart> getRootSchedulableBuilds() {
        Set<MavenProjectPart> result = new LinkedHashSet<>();
        for (MavenProject project : projectsToBuild) {
            // todo ProjectPart.MAIN
            MavenProjectPart mainProjectPart = new MavenProjectPart(project);
            List<MavenProjectPart> upstreamProjects = projectDependencyGraph.getDirectUpstreamProjects(mainProjectPart);
            if (upstreamProjects.isEmpty()) {
                result.add(mainProjectPart);
            }
        }
        if (result.isEmpty() && !projectsToBuild.isEmpty()) {
            // todo ProjectPart.MAIN
            result.add(new MavenProjectPart(projectsToBuild.get(0)));
        }
        return new ArrayList<>(result);
    }

    public List<MavenProjectPart> markAsFinished(MavenProjectPart mavenProjectPart) {
        finishedProjects.add(mavenProjectPart);
        return getSchedulableNewProcesses(mavenProjectPart);
    }

    private List<MavenProjectPart> getSchedulableNewProcesses(MavenProjectPart finishedProjectPart) {
        List<MavenProjectPart> result = new ArrayList<>();
        for (MavenProjectPart dependentProjectPart :
                projectDependencyGraph.getDirectDownstreamProjects(finishedProjectPart)) {
            List<MavenProjectPart> upstreamProjects =
                    projectDependencyGraph.getDirectUpstreamProjects(dependentProjectPart);
            if (finishedProjects.containsAll(upstreamProjects)) {
                result.add(dependentProjectPart);
            }
        }
        return result;
    }
}
