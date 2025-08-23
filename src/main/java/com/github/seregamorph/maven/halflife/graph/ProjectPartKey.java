package com.github.seregamorph.maven.halflife.graph;

import java.util.Objects;
import org.apache.maven.project.MavenProject;

/**
 * @author Sergey Chernov
 */
final class ProjectPartKey {

    private final String gav;

    ProjectPartKey(MavenProject project) {
        this.gav = getGav(project);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ProjectPartKey that = (ProjectPartKey) o;
        return Objects.equals(gav, that.gav);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(gav);
    }

    @Override
    public String toString() {
        return gav;
    }

    private static String getGav(MavenProject project) {
        return project.getGroupId() + ':' + project.getArtifactId() + ':' + project.getVersion();
    }
}
