package com.github.seregamorph.maven.halflife.graph;

import java.util.Objects;
import org.apache.maven.project.MavenProject;

/**
 * @author Sergey Chernov
 */
public final class MavenProjectPart {

    private final MavenProject project;
    private final ProjectPart part;

    MavenProjectPart(MavenProject project, ProjectPart part) {
        this.project = Objects.requireNonNull(project, "project");
        this.part = part;
    }

    public MavenProject getProject() {
        return project;
    }

    public ProjectPart getPart() {
        return part;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        MavenProjectPart that = (MavenProjectPart) o;
        return project == that.project
            && part == that.part;
    }

    @Override
    public int hashCode() {
        return Objects.hash(project, part);
    }

    @Override
    public String toString() {
        // same as BuilderCommon.getKey() and org.apache.maven.project.ProjectSorter.getId + part
        return project.getGroupId() + ':' + project.getArtifactId() + ':' + project.getVersion()
            + "(" + part + ")";
    }
}
