package com.github.seregamorph.maven.halflife;

import com.github.seregamorph.maven.halflife.graph.MavenProjectPart;
import com.github.seregamorph.maven.halflife.graph.ProjectPart;
import java.util.Arrays;
import javax.inject.Named;
import javax.inject.Singleton;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.execution.ProjectExecutionEvent;
import org.apache.maven.execution.ProjectExecutionListener;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.descriptor.MojoDescriptor;

/**
 * @author Sergey Chernov
 */
@SuppressWarnings("CodeBlock2Expr")
@Named
@Singleton
public class HalfLifeProjectExecutionListener implements ProjectExecutionListener {

    @Override
    public void beforeProjectExecution(ProjectExecutionEvent event) {
    }

    @Override
    public void beforeProjectLifecycleExecution(ProjectExecutionEvent event) {
        CurrentProjectExecution.ifPresent(execution -> {
            boolean hasTestJar = TestJarSupport.hasTestJar(event.getExecutionPlan());
            MavenProjectPart projectPart = new MavenProjectPart(event.getProject(), execution.part);
            event.getExecutionPlan().removeIf(mojoExecution -> {
                return !isExecuteMojo(hasTestJar, event.getSession(), projectPart, mojoExecution);
            });
        });
    }

    static boolean isExecuteMojo(boolean hasTestJar, MavenSession session,
                                 MavenProjectPart projectPart, MojoExecution mojoExecution) {
        boolean skipPartSplit = SkipPartSplitUtils.isSkipPartSplit(session, projectPart.getProject());
        if (skipPartSplit) {
            return projectPart.getPart() == ProjectPart.MAIN;
        }

        String phase = getLifecyclePhase(mojoExecution);
        // TODO #5 support Maven 4
        boolean isMainPhaseMojo = Arrays.asList(
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
            "package"
        ).contains(phase);
        if (hasTestJar) {
            // if project has the "test-jar" goal, it should compile (but not run)
            // the test sources in the MAIN part as well
            isMainPhaseMojo = isMainPhaseMojo || Arrays.asList(
                "generate-test-sources",
                "process-test-sources",
                "generate-test-resources",
                "process-test-resources",
                "test-compile",
                "process-test-classes"
            ).contains(phase);
        }

        if (projectPart.getPart() == ProjectPart.MAIN) {
            return isMainPhaseMojo;
        } else {
            return !isMainPhaseMojo;
        }
    }

    private static String getLifecyclePhase(MojoExecution mojoExecution) {
        String phase = mojoExecution.getLifecyclePhase();
        if (phase == null) {
            MojoDescriptor mojoDescriptor = mojoExecution.getMojoDescriptor();
            if (mojoDescriptor != null) {
                phase = mojoDescriptor.getPhase();
            }
        }
        return phase;
    }

    @Override
    public void afterProjectExecutionSuccess(ProjectExecutionEvent event) {
    }

    @Override
    public void afterProjectExecutionFailure(ProjectExecutionEvent event) {
    }
}
