package com.github.seregamorph.maven.halflife;

import static com.github.seregamorph.maven.halflife.graph.ProjectPart.MAIN;
import static com.github.seregamorph.maven.halflife.graph.ProjectPart.TEST;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.github.seregamorph.maven.halflife.graph.MavenProjectPart;
import java.util.List;
import java.util.Properties;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.Test;

class HalfLifeProjectExecutionListenerTest {

    @Test
    public void shouldExecuteMojoInMainNoTestJar() {
        var mainPhases = List.of(
            // "clean" lifecycle
            "pre-clean",
            "clean",
            "post-clean",
            // "site" lifecycle
            "pre-site",
            "site",
            "post-site",
            "site-deploy",
            // "default" lifecycle
            "validate",
            "initialize",
            "generate-sources",
            "process-sources",
            "generate-resources",
            "process-resources",
            "compile",
            "process-classes",
            "prepare-package",
            "package" // todo support test-jar
        );
        var testPhases = List.of("generate-test-sources",
            "process-test-sources",
            "generate-test-resources",
            "process-test-resources",
            "test-compile",
            "process-test-classes",
            "test",
            "pre-integration-test",
            "integration-test",
            "post-integration-test",
            "verify",
            "install",
            "deploy");

        var session = mock(MavenSession.class);
        when(session.getSystemProperties()).thenReturn(new Properties());
        when(session.getUserProperties()).thenReturn(new Properties());
        var project = mock(MavenProject.class);
        when(project.getProperties()).thenReturn(new Properties());
        var mainProjectPart = new MavenProjectPart(project, MAIN);
        var testProjectPart = new MavenProjectPart(project, TEST);
        for (var mainPhase : mainPhases) {
            assertTrue(HalfLifeProjectExecutionListener.isExecuteMojo(false, session, mainProjectPart,
                    mojoExecution(mainPhase)),
                "Should execute mojo phase " + mainPhase + " for part MAIN");
            assertFalse(HalfLifeProjectExecutionListener.isExecuteMojo(false, session, testProjectPart,
                    mojoExecution(mainPhase)),
                "Should not execute mojo phase " + mainPhase + " for part TEST");
        }
        for (var testPhase : testPhases) {
            assertTrue(HalfLifeProjectExecutionListener.isExecuteMojo(false, session, testProjectPart,
                    mojoExecution(testPhase)),
                "Should execute mojo phase " + testPhase + " for part TEST");
            assertFalse(HalfLifeProjectExecutionListener.isExecuteMojo(false, session, mainProjectPart,
                    mojoExecution(testPhase)),
                "Should not execute mojo phase " + testPhase + " for part MAIN");
        }
    }

    @Test
    public void shouldExecuteMojoInMainWithTestJar() {
        var mainPhases = List.of(
            // "clean" lifecycle
            "pre-clean",
            "clean",
            "post-clean",
            // "site" lifecycle
            "pre-site",
            "site",
            "post-site",
            "site-deploy",
            // "default" lifecycle
            "validate",
            "initialize",
            "generate-sources",
            "process-sources",
            "generate-resources",
            "process-resources",
            "compile",
            "process-classes",
            "generate-test-sources",
            "process-test-sources",
            "generate-test-resources",
            "process-test-resources",
            "test-compile",
            "process-test-classes",
            "prepare-package",
            "package"
        );
        var testPhases = List.of(
            "test",
            "pre-integration-test",
            "integration-test",
            "post-integration-test",
            "verify",
            "install",
            "deploy");

        var session = mock(MavenSession.class);
        when(session.getSystemProperties()).thenReturn(new Properties());
        when(session.getUserProperties()).thenReturn(new Properties());
        var project = mock(MavenProject.class);
        when(project.getProperties()).thenReturn(new Properties());
        var mainProjectPart = new MavenProjectPart(project, MAIN);
        var testProjectPart = new MavenProjectPart(project, TEST);
        for (var mainPhase : mainPhases) {
            assertTrue(HalfLifeProjectExecutionListener.isExecuteMojo(true, session, mainProjectPart,
                    mojoExecution(mainPhase)),
                "Should execute mojo phase " + mainPhase + " for part MAIN");
            assertFalse(HalfLifeProjectExecutionListener.isExecuteMojo(true, session, testProjectPart,
                    mojoExecution(mainPhase)),
                "Should not execute mojo phase " + mainPhase + " for part TEST");
        }
        for (var testPhase : testPhases) {
            assertTrue(HalfLifeProjectExecutionListener.isExecuteMojo(true, session, testProjectPart,
                    mojoExecution(testPhase)),
                "Should execute mojo phase " + testPhase + " for part TEST");
            assertFalse(HalfLifeProjectExecutionListener.isExecuteMojo(true, session, mainProjectPart,
                    mojoExecution(testPhase)),
                "Should not execute mojo phase " + testPhase + " for part MAIN");
        }
    }

    private static MojoExecution mojoExecution(String phase) {
        var mojoExecution = new MojoExecution(new MojoDescriptor());
        mojoExecution.setLifecyclePhase(phase);
        return mojoExecution;
    }
}
