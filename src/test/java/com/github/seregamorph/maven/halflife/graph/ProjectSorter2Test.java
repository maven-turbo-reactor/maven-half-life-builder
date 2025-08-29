package com.github.seregamorph.maven.halflife.graph;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Locale;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Parent;
import org.apache.maven.project.DuplicateProjectException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.dag.CycleDetectedException;
import org.junit.jupiter.api.Test;

class ProjectSorter2Test {

    enum Scope {
        COMPILE,
        TEST;

        @Override
        public String toString() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    private static final String GROUP_ID = "groupId";
    private static final String VERSION = "1.0-SNAPSHOT";

    @Test
    public void shouldSort() throws CycleDetectedException, DuplicateProjectException {
        var parent = project("parent");
        var app = project("app");
        var core = project("core");
        var testUtils = project("test-utils");

        app.getModel().setParent(parentModel(parent));
        core.getModel().setParent(parentModel(parent));
        testUtils.getModel().setParent(parentModel(parent));

        app.getDependencies().addAll(List.of(
            moduleDependency("core", Scope.COMPILE),
            junitJupiterTestDependency()
        ));
        core.getDependencies().addAll(List.of(
            jacksonCoreCompileDependency(),
            moduleDependency("test-utils", Scope.TEST),
            junitJupiterTestDependency()
        ));

        var projectSorter = new ProjectSorter2(List.of(app, core, parent, testUtils));
        assertEquals(List.of(
            new MavenProjectPart(parent),
            new MavenProjectPart(testUtils),
            new MavenProjectPart(core),
            new MavenProjectPart(app)
        ), projectSorter.getSortedProjectParts());

        assertEquals(List.of(), projectSorter.getDependencies(id(parent)));
        assertEquals(List.of(
            "groupId:app:1.0-SNAPSHOT",
            "groupId:core:1.0-SNAPSHOT",
            "groupId:test-utils:1.0-SNAPSHOT"
        ), projectSorter.getDependents(id(parent)));

        assertEquals(List.of(
            "groupId:parent:1.0-SNAPSHOT"
        ), projectSorter.getDependencies(id(testUtils)));
        assertEquals(List.of(
            "groupId:core:1.0-SNAPSHOT"
        ), projectSorter.getDependents(id(testUtils)));

        assertEquals(List.of(
            "groupId:test-utils:1.0-SNAPSHOT",
            "groupId:parent:1.0-SNAPSHOT"
        ), projectSorter.getDependencies(id(core)));
        assertEquals(List.of(
            "groupId:app:1.0-SNAPSHOT"
        ), projectSorter.getDependents(id(core)));

        assertEquals(List.of(
            "groupId:core:1.0-SNAPSHOT",
            "groupId:parent:1.0-SNAPSHOT"
        ), projectSorter.getDependencies(id(app)));
        assertEquals(List.of(), projectSorter.getDependents(id(app)));
    }

    private static String id(MavenProject parent) {
        return new MavenProjectPart(parent).toString();
    }

    private Parent parentModel(MavenProject parentProject) {
        var parent = new Parent();
        parent.setGroupId(parentProject.getGroupId());
        parent.setArtifactId(parentProject.getArtifactId());
        parent.setVersion(parentProject.getVersion());
        return parent;
    }

    private static MavenProject project(String artifactId) {
        var project = new MavenProject();
        project.setGroupId(GROUP_ID);
        project.setArtifactId(artifactId);
        project.setVersion(VERSION);
        return project;
    }

    private static Dependency moduleDependency(String artifactId, Scope scope) {
        var dependency = new Dependency();
        dependency.setGroupId(GROUP_ID);
        dependency.setArtifactId(artifactId);
        dependency.setVersion(VERSION);
        dependency.setScope(scope.toString());
        return dependency;
    }

    private static Dependency libraryDependency(String groupId, String artifactId, String version, Scope scope) {
        var dependency = new Dependency();
        dependency.setGroupId(groupId);
        dependency.setArtifactId(artifactId);
        dependency.setVersion(version);
        dependency.setScope(scope.toString());
        return dependency;
    }

    private static Dependency jacksonCoreCompileDependency() {
        return libraryDependency("com.fasterxml.jackson.core", "jackson-core", "2.19.2", Scope.COMPILE);
    }

    private static Dependency junitJupiterTestDependency() {
        return libraryDependency("org.junit.jupiter", "junit-jupiter", "5.13.4", Scope.TEST);
    }
}
