/*
 * (c) Copyright 2017 Palantir Technologies Inc. All rights reserved.
 */

package com.palantir.conjure.gen.python;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableSet;
import com.palantir.conjure.ConjureSubfolderRunner;
import com.palantir.conjure.defs.Conjure;
import com.palantir.conjure.defs.ConjureDefinition;
import com.palantir.conjure.gen.python.client.ClientGenerator;
import com.palantir.conjure.gen.python.types.DefaultBeanGenerator;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.runner.RunWith;

@ConjureSubfolderRunner.ParentFolder("src/test/resources")
@RunWith(ConjureSubfolderRunner.class)
public final class ConjurePythonGeneratorTest {

    private final ConjurePythonGenerator generator = new ConjurePythonGenerator(
            new DefaultBeanGenerator(ImmutableSet.of()), new ClientGenerator());
    private final InMemoryPythonFileWriter pythonFileWriter = new InMemoryPythonFileWriter();

    @ConjureSubfolderRunner.Test
    public void assertThatFilesRenderAsExpected(Path folder) throws IOException {
        Path expected = folder.resolve("expected");
        List<ConjureDefinition> definitions = getInputDefinitions(folder);
        maybeResetExpectedDirectory(expected, definitions);

        definitions.forEach(definition ->
                generator.write(definition, pythonFileWriter));
        assertFoldersEqual(expected);
    }

    private void assertFoldersEqual(Path expected) throws IOException {
        long count = 0;
        for (Path path : java.nio.file.Files.walk(expected).collect(Collectors.toList())) {
            if (!path.toFile().isFile()) {
                continue;
            }
            assertThat(path).hasContent(pythonFileWriter.getPythonFiles().get(expected.relativize(path)));
            count += 1;
        }
        System.out.println(count + " files checked");
    }

    private void maybeResetExpectedDirectory(Path expected, List<ConjureDefinition> definitions) throws IOException {
        if (Boolean.valueOf(System.getProperty("recreate", "false"))
                || !expected.toFile().isDirectory()) {
            Files.createDirectories(expected);
            Files.walk(expected).filter(path -> path.toFile().isFile()).forEach(path -> path.toFile().delete());
            Files.walk(expected).forEach(path -> path.toFile().delete());
            Files.createDirectories(expected);
            definitions.forEach(definition ->
                    generator.write(definition, new DefaultPythonFileWriter(expected)));
        }
    }

    private List<ConjureDefinition> getInputDefinitions(Path folder) throws IOException {
        Files.createDirectories(folder);
        List<ConjureDefinition> definitions = java.nio.file.Files.walk(folder)
                .map(Path::toFile)
                .filter(file -> file.toString().endsWith(".yml"))
                .map(Conjure::parse)
                .collect(Collectors.toList());

        if (definitions.isEmpty()) {
            throw new RuntimeException(
                    folder + " contains no conjure.yml files, please write one to set up a new test");
        }
        return definitions;
    }
}
