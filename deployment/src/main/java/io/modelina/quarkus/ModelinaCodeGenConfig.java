package io.modelina.quarkus;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

// This configuration is read in codegen phase (before build time), the annotation is for
// documentation
// purposes and to avoid quarkus warnings
@ConfigRoot(name = "modelina", phase = ConfigPhase.BUILD_TIME)
public class ModelinaCodeGenConfig {
    static final String MODELINA_CONFIG_PREFIX = "quarkus.modelina";

}
