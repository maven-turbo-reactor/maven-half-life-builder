package com.github.seregamorph.maven.halflife;

import static com.github.seregamorph.maven.halflife.MavenPropertyUtils.getProperty;
import static com.github.seregamorph.maven.halflife.MavenPropertyUtils.isTrue;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;

/**
 * @author Sergey Chernov
 */
public final class SkipPartSplitUtils {

    static final String PROPERTY_SKIP_PART_SPLIT = "skipPartSplit";

    public static boolean isSkipPartSplit(MavenSession session, MavenProject project) {
        return isTrue(getProperty(session, project, PROPERTY_SKIP_PART_SPLIT));
    }

    private SkipPartSplitUtils() {
    }
}
