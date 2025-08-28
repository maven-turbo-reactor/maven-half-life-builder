package com.github.seregamorph.maven.halflife.graph;

import java.util.Objects;
import org.apache.maven.project.MavenProject;

/**
 * @author Sergey Chernov
 */
public final class MavenProjectPart {

    private final MavenProject project;

    MavenProjectPart(MavenProject project) {
        this.project = project;
    }

    public MavenProject getProject() {
        return project;
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
        // same as BuilderCommon.getKey() and org.apache.maven.project.ProjectSorter.getId + part
        return project.getGroupId() + ':' + project.getArtifactId() + ':' + project.getVersion();
    }
}
