package com.github.seregamorph.maven.halflife.graph;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.maven.artifact.ArtifactUtils;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Extension;
import org.apache.maven.model.Parent;
import org.apache.maven.model.Plugin;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.dag.CycleDetectedException;
import org.codehaus.plexus.util.dag.DAG;
import org.codehaus.plexus.util.dag.TopologicalSorter;
import org.codehaus.plexus.util.dag.Vertex;

/**
 * @author Sergey Chernov
 * <p>
 * Based on {@link org.apache.maven.project.ProjectSorter} from maven-core.
 */
public class ProjectSorter2 {

    private final DAG dag = new DAG();

    private final List<MavenProjectPart> sortedProjectParts;

    public ProjectSorter2(Collection<MavenProject> projects) throws CycleDetectedException {
        // groupId:artifactId:version(part) -> project
        Map<String, MavenProjectPart> projectPartMap = new HashMap<>(projects.size() * 2);

        // groupId:artifactId -> (version -> (part -> vertex))
        Map<String, Map<String, Map<ProjectPart, Vertex>>> vertexMap = new HashMap<>(projects.size() * 2);

        for (MavenProject project : projects) {
            MavenProjectPart mainProjectPart = new MavenProjectPart(project, ProjectPart.MAIN);
            MavenProjectPart testProjectPart = new MavenProjectPart(project, ProjectPart.TEST);
            projectPartMap.put(mainProjectPart.toString(), mainProjectPart);
            projectPartMap.put(testProjectPart.toString(), testProjectPart);

            // we can skip it as it was already executed in the original projectSorter
//            if (conflictingProject != null) {
//                throw new DuplicateProjectException(
//                    projectId,
//                    conflictingProject.getFile(),
//                    project.getFile(),
//                    "Project '" + projectId + "' is duplicated in the reactor");
//            }

            Vertex mainProjectVertex = dag.addVertex(mainProjectPart.toString());
            Vertex testProjectVertex = dag.addVertex(testProjectPart.toString());
            dag.addEdge(testProjectVertex, mainProjectVertex);

            String projectKey = ArtifactUtils.versionlessKey(project.getGroupId(), project.getArtifactId());
            Map<ProjectPart, Vertex> vertices = vertexMap.computeIfAbsent(projectKey, k -> new HashMap<>(2, 1))
                .computeIfAbsent(project.getVersion(), k -> new HashMap<>(2, 1));
            vertices.put(ProjectPart.MAIN, mainProjectVertex);
            vertices.put(ProjectPart.TEST, testProjectVertex);
        }

        for (Vertex projectPartVertex : dag.getVertices()) {
            // group:artifact:version(part)
            String projectPartId = projectPartVertex.getLabel();

            MavenProjectPart projectPart = projectPartMap.get(projectPartId);
            MavenProject project = projectPart.getProject();

            for (Dependency dependency : project.getDependencies()) {
                // modules and libraries
                if (isAddDependency(projectPart.getPart(), dependency.getScope())) {
                    addEdge(
                        vertexMap,
                        projectPartVertex,
                        dependency.getGroupId(),
                        dependency.getArtifactId(),
                        dependency.getVersion(),
                        false,
                        false);
                }
            }

            Parent parent = project.getModel().getParent();

            if (parent != null) {
                // Parent is added as an edge, but must not cause a cycle - so we remove any other edges it has
                // in conflict
                addEdge(
                    vertexMap,
                    projectPartVertex,
                    parent.getGroupId(),
                    parent.getArtifactId(),
                    parent.getVersion(),
                    true,
                    false);
            }

            List<Plugin> buildPlugins = project.getBuildPlugins();
            if (buildPlugins != null) {
                for (Plugin plugin : buildPlugins) {
                    addEdge(
                        vertexMap,
                        projectPartVertex,
                        plugin.getGroupId(),
                        plugin.getArtifactId(),
                        plugin.getVersion(),
                        false,
                        true);

                    for (Dependency dependency : plugin.getDependencies()) {
                        addEdge(
                            vertexMap,
                            projectPartVertex,
                            dependency.getGroupId(),
                            dependency.getArtifactId(),
                            dependency.getVersion(),
                            false,
                            true);
                    }
                }
            }

            List<Extension> buildExtensions = project.getBuildExtensions();
            if (buildExtensions != null) {
                for (Extension extension : buildExtensions) {
                    addEdge(
                        vertexMap,
                        projectPartVertex,
                        extension.getGroupId(),
                        extension.getArtifactId(),
                        extension.getVersion(),
                        false,
                        true);
                }
            }
        }

        List<MavenProjectPart> sortedProjects = new ArrayList<>(projects.size());
        List<String> sortedProjectPartLabels = TopologicalSorter.sort(dag);
        for (String id : sortedProjectPartLabels) {
            MavenProjectPart mavenProjectPart = projectPartMap.get(id);
            sortedProjects.add(mavenProjectPart);
        }
        this.sortedProjectParts = Collections.unmodifiableList(sortedProjects);
    }

    private static boolean isAddDependency(ProjectPart part, String scope) {
        if (part == ProjectPart.MAIN) {
            return !"test".equals(scope);
        } else {
            return "test".equals(scope);
        }
    }

    private void addEdge(
        Map<String, Map<String, Map<ProjectPart, Vertex>>> vertexMap,
        Vertex projectPartVertex,
        String groupId,
        String artifactId,
        String version,
        boolean force,
        boolean safe
    ) throws CycleDetectedException {
        String projectKey = ArtifactUtils.versionlessKey(groupId, artifactId);
        // version -> (part -> vertex)
        Map<String, Map<ProjectPart, Vertex>> vertices = vertexMap.get(projectKey);

        if (vertices != null) {
            if (isSpecificVersion(version)) {
                Map<ProjectPart, Vertex> partVertices = vertices.get(version);
                // todo support ProjectPart.TEST for test-jar
                Vertex vertex = partVertices == null ? null : partVertices.get(ProjectPart.MAIN);
                if (vertex != null) {
                    addEdge(projectPartVertex, vertex, force, safe);
                }
            } else {
                for (Map<ProjectPart, Vertex> partVertices : vertices.values()) {
                    Vertex vertex = partVertices.get(ProjectPart.MAIN);
                    if (vertex != null) {
                        addEdge(projectPartVertex, vertex, force, safe);
                    }
                }
            }
        }
    }

    private void addEdge(
        Vertex fromVertex,
        Vertex toVertex,
        boolean force,
        boolean safe
    ) throws CycleDetectedException {
        if (fromVertex.equals(toVertex)) {
            return;
        }

        // we can skip it as it was already executed in the original projectSorter
//        if (fromProject != null) {
//            MavenProject toProject = projectMap.get(toVertex.getLabel());
//            fromProject.addProjectReference(toProject);
//        }

        if (force && toVertex.getChildren().contains(fromVertex)) {
            dag.removeEdge(toVertex, fromVertex);
        }

        try {
            dag.addEdge(fromVertex, toVertex);
        } catch (CycleDetectedException e) {
            if (!safe) {
                throw e;
            }
        }
    }

    private static boolean isSpecificVersion(String version) {
        return !(StringUtils.isEmpty(version) || version.startsWith("[") || version.startsWith("("));
    }

    List<MavenProjectPart> getSortedProjectParts() {
        return sortedProjectParts;
    }

    List<String> getDependents(String id) {
        return dag.getParentLabels(id);
    }

    List<String> getDependencies(String id) {
        return dag.getChildLabels(id);
    }
}
