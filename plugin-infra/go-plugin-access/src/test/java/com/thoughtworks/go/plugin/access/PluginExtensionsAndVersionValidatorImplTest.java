/*
 * Copyright 2019 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.thoughtworks.go.plugin.access;

import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginDescriptor;
import com.thoughtworks.go.plugin.infra.plugininfo.PluginRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.util.Arrays;
import java.util.Collections;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.thoughtworks.go.plugin.domain.common.PluginConstants.AUTHORIZATION_EXTENSION;
import static com.thoughtworks.go.plugin.domain.common.PluginConstants.ELASTIC_AGENT_EXTENSION;
import static com.thoughtworks.go.plugin.infra.PluginExtensionsAndVersionValidator.ValidationResult;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

class PluginExtensionsAndVersionValidatorImplTest {
    private static final String PLUGIN_ID = "Some-Plugin-Id";
    @Mock
    private ExtensionsRegistry extensionsRegistry;
    @Mock
    private GoPluginDescriptor descriptor;
    @Mock
    private PluginRegistry pluginRegistry;
    private PluginExtensionsAndVersionValidatorImpl pluginExtensionsAndVersionValidator;

    @BeforeEach
    void setUp() {
        initMocks(this);
        when(descriptor.id()).thenReturn(PLUGIN_ID);
        when(extensionsRegistry.allRegisteredExtensions())
                .thenReturn(Stream.of(ELASTIC_AGENT_EXTENSION, AUTHORIZATION_EXTENSION).collect(Collectors.toSet()));
        when(extensionsRegistry.gocdSupportedExtensionVersions(ELASTIC_AGENT_EXTENSION))
                .thenReturn(Arrays.asList("1.0", "2.0"));
        when(extensionsRegistry.gocdSupportedExtensionVersions(AUTHORIZATION_EXTENSION))
                .thenReturn(singletonList("2.0"));

        pluginExtensionsAndVersionValidator = new PluginExtensionsAndVersionValidatorImpl(extensionsRegistry, pluginRegistry);
    }

    @Test
    void shouldNotAddErrorOnSuccessfulValidation() {
        when(pluginRegistry.getExtensionsInfo(PLUGIN_ID)).thenReturn(Collections.singletonMap(ELASTIC_AGENT_EXTENSION, singletonList("2.0")));

        final ValidationResult validationResult = pluginExtensionsAndVersionValidator.validate(descriptor);

        assertThat(validationResult.hasError()).isFalse();
    }

    @Test
    void shouldAddErrorAndReturnValidationResultWhenPluginRequiredExtensionIsNotSupportedByGoCD() {
        when(pluginRegistry.getExtensionsInfo(PLUGIN_ID)).thenReturn(Collections.singletonMap("some-invalid-extension", singletonList("2.0")));

        final ValidationResult validationResult = pluginExtensionsAndVersionValidator.validate(descriptor);

        assertThat(validationResult.hasError()).isTrue();
        assertThat(validationResult.toErrorMessage()).isEqualTo("Extension incompatibility detected between plugin(Some-Plugin-Id) and GoCD:\n" +
                "  Extension(s) [some-invalid-extension] used by the plugin is not supported. GoCD Supported extensions are [authorization, elastic-agent].");
    }

    @Test
    void shouldAddErrorAndReturnValidationResultWhenPluginRequiredExtensionVersionIsNotSupportedByGoCD() {
        when(pluginRegistry.getExtensionsInfo(PLUGIN_ID)).thenReturn(Collections.singletonMap(ELASTIC_AGENT_EXTENSION, singletonList("3.0")));

        final ValidationResult validationResult = pluginExtensionsAndVersionValidator.validate(descriptor);

        assertThat(validationResult.hasError()).isTrue();
        assertThat(validationResult.toErrorMessage())
                .isEqualTo("Extension incompatibility detected between plugin(Some-Plugin-Id) and GoCD:\n" +
                        "  Expected elastic-agent extension version(s) [3.0] by plugin is unsupported. GoCD Supported versions are [1.0, 2.0].");
    }

    @Test
    void shouldConsiderPluginValidWhenOneOfTheExtensionVersionUsedByThePluginIsSupportedByGoCD() {
        when(pluginRegistry.getExtensionsInfo(PLUGIN_ID)).thenReturn(Collections.singletonMap(ELASTIC_AGENT_EXTENSION, Arrays.asList("a.b", "2.0")));

        final ValidationResult validationResult = pluginExtensionsAndVersionValidator.validate(descriptor);

        assertThat(validationResult.hasError()).isFalse();
    }
}