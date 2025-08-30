package com.github.seregamorph.maven.halflife;

import static com.github.seregamorph.maven.halflife.graph.ProjectPart.MAIN;
import static com.github.seregamorph.maven.halflife.graph.ProjectPart.TEST;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.junit.jupiter.api.Test;

class HalfLifeProjectExecutionListenerTest {

    @Test
    public void shouldExecuteMojoInMain() {
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

        for (var mainPhase : mainPhases) {
            assertTrue(HalfLifeProjectExecutionListener.isExecuteMojo(MAIN, mojoExecution(mainPhase)),
                "Should execute mojo phase " + mainPhase + " for part MAIN");
            assertFalse(HalfLifeProjectExecutionListener.isExecuteMojo(TEST, mojoExecution(mainPhase)),
                "Should not execute mojo phase " + mainPhase + " for part TEST");
        }
        for (var testPhase : testPhases) {
            assertTrue(HalfLifeProjectExecutionListener.isExecuteMojo(TEST, mojoExecution(testPhase)),
                "Should execute mojo phase " + testPhase + " for part TEST");
            assertFalse(HalfLifeProjectExecutionListener.isExecuteMojo(MAIN, mojoExecution(testPhase)),
                "Should not execute mojo phase " + testPhase + " for part MAIN");
        }
    }

    private static MojoExecution mojoExecution(String phase) {
        var mojoExecution = new MojoExecution(new MojoDescriptor());
        mojoExecution.setLifecyclePhase(phase);
        return mojoExecution;
    }
}
