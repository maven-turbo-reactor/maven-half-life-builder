package com.github.seregamorph.maven.halflife.graph;

import java.util.Objects;
import org.apache.maven.project.MavenProject;

/**
 * @author Sergey Chernov
 */
final class MavenProjectPart {

    private final MavenProject project;

    MavenProjectPart(MavenProject project) {
        this.project = project;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        MavenProjectPart that = (MavenProjectPart) o;
        return project == that.project;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(project);
    }

    @Override
    public String toString() {
        return getGav(project);
    }

    private static String getGav(MavenProject project) {
        // same as BuilderCommon.getKey()
        return project.getGroupId() + ':' + project.getArtifactId() + ':' + project.getVersion();
    }
}
