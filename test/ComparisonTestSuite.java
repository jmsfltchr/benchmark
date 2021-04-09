/*
 * Copyright (C) 2021 Grakn Labs
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package grakn.benchmark.test;

import grakn.benchmark.config.AgentMode;
import grakn.benchmark.config.Config;
import grakn.benchmark.grakn.GraknSimulation;
import grakn.benchmark.neo4j.Neo4JSimulation;
import grakn.benchmark.simulation.common.SimulationContext;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.junit.runner.Runner;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.Suite;
import org.junit.runners.model.InitializationError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static grakn.benchmark.config.Config.Agent.ConstructAgentConfig;
import static grakn.benchmark.simulation.common.World.initialise;

public class ComparisonTestSuite extends Suite {
    private static final Logger LOG = LoggerFactory.getLogger(ComparisonTestSuite.class);
    private static final List<Runner> NO_RUNNERS = Collections.emptyList();
    private static int iteration = 1;
    private static final int NUM_ITERATIONS = 5;
    private final List<Runner> runners;
    private final Class<?> klass;

    public static Neo4JSimulation NEO4J;
    public static GraknSimulation GRAKN_CORE;

    public ComparisonTestSuite(Class<?> klass) throws Throwable {
        super(klass, NO_RUNNERS);
        this.klass = klass;
        this.runners = Collections.unmodifiableList(createRunnersForIterations());

        String[] args = System.getProperty("sun.java.command").split(" ");

        Options options = new Options();
        options.addOption(Option.builder("g")
                                  .longOpt("grakn-uri").desc("Grakn server URI").hasArg().required().argName("grakn-uri")
                                  .build());
        options.addOption(Option.builder("n")
                                  .longOpt("neo4j-uri").desc("Neo4j server URI").hasArg().required().argName("neo4j-uri")
                                  .build());

        CommandLineParser parser = new DefaultParser();
        CommandLine commandLine;

        String graknUri = null;
        String neo4jUri = null;
        try {
            commandLine = parser.parse(options, args);
            graknUri = commandLine.getOptionValue("g");
            neo4jUri = commandLine.getOptionValue("n");
        } catch (ParseException e) {
            LOG.error(e.getMessage(), e);
        }

        int scaleFactor = 5;
        int randomSeed = 1;

        Map<String, Path> files = new HashMap<>();

        List<String> dirPaths = new ArrayList<>();
        dirPaths.add("simulation/data");
        dirPaths.add("grakn/data");
        dirPaths.add("neo4j/data");

        dirPaths.forEach(dirPath -> {
            Arrays.asList(Objects.requireNonNull(Paths.get(dirPath).toFile().listFiles())).forEach(file -> {
                Path path = file.toPath();
                String filename = path.getFileName().toString();
                files.put(filename, path);
            });
        });

        ArrayList<String> agentNames = new ArrayList<>();
//        agentNames.add("marriage");
        agentNames.add("personBirth");
//        agentNames.add("ageUpdate");
//        agentNames.add("parentship");
//        agentNames.add("relocation");
        agentNames.add("company");
//        agentNames.add("employment");
        agentNames.add("product");
        agentNames.add("purchase");
//        agentNames.add("friendship");

        ArrayList<Config.Agent> agentConfigs = new ArrayList<>();
        agentNames.forEach(name -> agentConfigs.add(ConstructAgentConfig(name, AgentMode.RUN)));
        try {
            GRAKN_CORE = GraknSimulation.core(graknUri, files, randomSeed, agentConfigs,
                                              SimulationContext.create(initialise(scaleFactor, files), true));
            NEO4J = Neo4JSimulation.create(neo4jUri, files, randomSeed, agentConfigs,
                                           SimulationContext.create(initialise(scaleFactor, files), true));
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
    }

    private List<Runner> createRunnersForIterations() {
        List<Runner> runners = new ArrayList<>();
        for (int i = 1; i <= NUM_ITERATIONS; i++) {
            try {
                BlockJUnit4ClassRunner runner = new ComparisonTestRunner(klass, i);
                runners.add(runner);
            } catch (InitializationError initializationError) {
                throw new RuntimeException(initializationError);
            }
        }
        return runners;
    }

    protected void runChild(Runner runner, final RunNotifier notifier) {
        iteration++;
        NEO4J.iterate();
        GRAKN_CORE.iterate();
        super.runChild(runner, notifier);
        if (iteration == NUM_ITERATIONS + 1) {
            GRAKN_CORE.close();
            NEO4J.close();
        }
    }

    protected List<Runner> getChildren() {
        return this.runners;
    }
}
