package com.github.seregamorph.maven.halflife;

import com.github.seregamorph.maven.halflife.graph.ProjectPart;
import java.util.Arrays;
import javax.inject.Named;
import javax.inject.Singleton;
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
            event.getExecutionPlan().removeIf(mojoExecution -> {
                return !isExecuteMojo(execution.part, mojoExecution);
            });
        });
    }

    static boolean isExecuteMojo(ProjectPart part, MojoExecution mojoExecution) {
        String phase = getLifecyclePhase(mojoExecution);
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
            "package" // todo support test-jar
        ).contains(phase);

        if (part == ProjectPart.MAIN) {
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
