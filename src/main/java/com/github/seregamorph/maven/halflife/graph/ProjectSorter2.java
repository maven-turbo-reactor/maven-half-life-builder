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
        // groupId:artifactId:version -> project
        Map<String, MavenProject> projectMap = new HashMap<>(projects.size() * 2);

        // groupId:artifactId -> (version -> vertex)
        Map<String, Map<String, Vertex>> vertexMap = new HashMap<>(projects.size() * 2);

        for (MavenProject project : projects) {
            String projectId = getId(project);
            projectMap.put(projectId, project);

            // we can skip it as it was already executed in the original projectSorter
//            if (conflictingProject != null) {
//                throw new DuplicateProjectException(
//                    projectId,
//                    conflictingProject.getFile(),
//                    project.getFile(),
//                    "Project '" + projectId + "' is duplicated in the reactor");
//            }

            String projectKey = ArtifactUtils.versionlessKey(project.getGroupId(), project.getArtifactId());

            Map<String, Vertex> vertices = vertexMap.computeIfAbsent(projectKey, k -> new HashMap<>(2, 1));
            vertices.put(project.getVersion(), dag.addVertex(projectId));
        }

        for (Vertex projectVertex : dag.getVertices()) {
            // group:artifact:version
            String projectId = projectVertex.getLabel();

            MavenProject project = projectMap.get(projectId);

            for (Dependency dependency : project.getDependencies()) {
                // modules and libraries
                addEdge(
                    vertexMap,
                    projectVertex,
                    dependency.getGroupId(),
                    dependency.getArtifactId(),
                    dependency.getVersion(),
                    false,
                    false);
            }

            Parent parent = project.getModel().getParent();

            if (parent != null) {
                // Parent is added as an edge, but must not cause a cycle - so we remove any other edges it has
                // in conflict
                addEdge(
                    vertexMap,
                    projectVertex,
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
                        projectVertex,
                        plugin.getGroupId(),
                        plugin.getArtifactId(),
                        plugin.getVersion(),
                        false,
                        true);

                    for (Dependency dependency : plugin.getDependencies()) {
                        addEdge(
                            vertexMap,
                            projectVertex,
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
                        projectVertex,
                        extension.getGroupId(),
                        extension.getArtifactId(),
                        extension.getVersion(),
                        false,
                        true);
                }
            }
        }

        List<MavenProjectPart> sortedProjects = new ArrayList<>(projects.size());
        List<String> sortedProjectLabels = TopologicalSorter.sort(dag);
        for (String id : sortedProjectLabels) {
            MavenProject mavenProject = projectMap.get(id);
            sortedProjects.add(new MavenProjectPart(mavenProject));
        }
        this.sortedProjectParts = Collections.unmodifiableList(sortedProjects);
    }

    private void addEdge(
        Map<String, Map<String, Vertex>> vertexMap,
        Vertex projectVertex,
        String groupId,
        String artifactId,
        String version,
        boolean force,
        boolean safe
    ) throws CycleDetectedException {
        String projectKey = ArtifactUtils.versionlessKey(groupId, artifactId);
        Map<String, Vertex> vertices = vertexMap.get(projectKey);

        if (vertices != null) {
            if (isSpecificVersion(version)) {
                Vertex vertex = vertices.get(version);
                if (vertex != null) {
                    addEdge(projectVertex, vertex, force, safe);
                }
            } else {
                for (Vertex vertex : vertices.values()) {
                    addEdge(projectVertex, vertex, force, safe);
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

    private static String getId(MavenProject project) {
        // todo MavenProjectPart.toString
        return ArtifactUtils.key(project.getGroupId(), project.getArtifactId(), project.getVersion());
    }
}
