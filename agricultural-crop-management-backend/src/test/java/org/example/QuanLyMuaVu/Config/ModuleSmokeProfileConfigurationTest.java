package org.example.QuanLyMuaVu.Config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.stream.Stream;
import org.example.QuanLyMuaVu.QuanLyMuaVuApplication;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;

class ModuleSmokeProfileConfigurationTest {

    @ParameterizedTest(name = "profile {0} should start with target {1}")
    @MethodSource("moduleSmokeProfiles")
    void moduleSmokeProfiles_shouldBootSuccessfully(String profile, String targetProperty) {
        try (ConfigurableApplicationContext context = new SpringApplicationBuilder(QuanLyMuaVuApplication.class)
                .web(WebApplicationType.NONE)
                .profiles("local", "module-smoke", profile)
                .properties(
                        "spring.main.banner-mode=off",
                        "logging.level.root=ERROR",
                        "logging.level.org.springframework=ERROR")
                .run()) {

            Environment env = context.getEnvironment();
            assertThat(env.getProperty("module.smoke.enabled", Boolean.class)).isTrue();
            assertThat(env.getProperty("module.smoke.target")).isEqualTo(targetProperty);
            assertThat(env.getProperty("module.smoke.modules." + targetProperty, Boolean.class)).isTrue();
        }
    }

    private static Stream<Arguments> moduleSmokeProfiles() {
        return Stream.of(
                Arguments.of("module-smoke-ai", "ai"),
                Arguments.of("module-smoke-cropcatalog", "cropcatalog"),
                Arguments.of("module-smoke-inventory", "inventory"));
    }
}
