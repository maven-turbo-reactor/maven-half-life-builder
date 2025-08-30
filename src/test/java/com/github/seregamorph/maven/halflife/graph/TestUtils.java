package com.github.seregamorph.maven.halflife.graph;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Parent;
import org.apache.maven.project.MavenProject;

final class TestUtils {

    static final String GROUP_ID = "groupId";
    static final String VERSION = "1.0-SNAPSHOT";

    static MavenProject project(String artifactId) {
        var project = new MavenProject();
        project.setGroupId(GROUP_ID);
        project.setArtifactId(artifactId);
        project.setVersion(VERSION);
        return project;
    }

    static Parent parentModel(MavenProject parentProject) {
        var parent = new Parent();
        parent.setGroupId(parentProject.getGroupId());
        parent.setArtifactId(parentProject.getArtifactId());
        parent.setVersion(parentProject.getVersion());
        return parent;
    }

    static Dependency moduleDependency(String artifactId, Scope scope) {
        var dependency = new Dependency();
        dependency.setGroupId(GROUP_ID);
        dependency.setArtifactId(artifactId);
        dependency.setVersion(VERSION);
        dependency.setScope(scope.toString());
        return dependency;
    }

    static Dependency libraryDependency(String groupId, String artifactId, String version, Scope scope) {
        var dependency = new Dependency();
        dependency.setGroupId(groupId);
        dependency.setArtifactId(artifactId);
        dependency.setVersion(version);
        dependency.setScope(scope.toString());
        return dependency;
    }

    static Dependency jacksonCoreCompileDependency() {
        return libraryDependency("com.fasterxml.jackson.core", "jackson-core", "2.19.2", Scope.COMPILE);
    }

    static Dependency junitJupiterTestDependency() {
        return libraryDependency("org.junit.jupiter", "junit-jupiter", "5.13.4", Scope.TEST);
    }

    private TestUtils() {
    }
}
