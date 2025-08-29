package com.github.seregamorph.maven.halflife;

import java.util.List;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.lifecycle.LifecycleExecutionException;
import org.apache.maven.plugin.DefaultMojosExecutionStrategy;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.MojoExecutionRunner;
import org.apache.maven.plugin.MojosExecutionStrategy;

/**
 * @author Sergey Chernov
 */
public class HalfLifeBuilderExecutionStrategy extends DefaultMojosExecutionStrategy implements MojosExecutionStrategy {

    @Override
    public void execute(
        List<MojoExecution> mojos,
        MavenSession session,
        MojoExecutionRunner mojoExecutionRunner
    ) throws LifecycleExecutionException {
        super.execute(mojos, session, mojoExecutionRunner);
    }
}
