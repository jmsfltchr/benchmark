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

import grakn.benchmark.common.Config;
import grakn.benchmark.grakn.GraknSimulation;
import grakn.benchmark.neo4j.Neo4JSimulation;
import grakn.benchmark.simulation.common.SimulationContext;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.Suite;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static grakn.benchmark.common.Util.parseCommandLine;

public class ComparisonTestSuite extends Suite {

    private static final Logger LOG = LoggerFactory.getLogger(ComparisonTestSuite.class);
    private static final Config CONFIG = Config.loadYML(Paths.get("test/config.yml").toFile());
    private static final TestOptions OPTIONS = parseCommandLine(args(), new TestOptions()).get();

    public static GraknSimulation GRAKN_CORE;

    public static Neo4JSimulation NEO4J;
    private int iteration = 1;

    public ComparisonTestSuite(Class<?> testClass) throws Throwable {
        super(testClass, createRunners(testClass));
        GRAKN_CORE = GraknSimulation.core(OPTIONS.graknAddress(), CONFIG.randomSeed(), CONFIG.agents(),
                                          SimulationContext.create(CONFIG, false, true));
        NEO4J = Neo4JSimulation.create(OPTIONS.neo4jAddress(), CONFIG.randomSeed(), CONFIG.agents(),
                                       SimulationContext.create(CONFIG, false, true));
    }

    private static String[] args() {
        String[] input = System.getProperty("sun.java.command").split(" ");
        return Arrays.copyOfRange(input, 1, input.length);
    }

    private static List<org.junit.runner.Runner> createRunners(Class<?> testClass) throws InitializationError {
        List<org.junit.runner.Runner> runners = new ArrayList<>();
        for (int i = 1; i <= CONFIG.iterations(); i++) {
            BlockJUnit4ClassRunner runner = new Runner(testClass, i);
            runners.add(runner);
        }
        return runners;
    }

    @Override
    protected void runChild(org.junit.runner.Runner runner, final RunNotifier notifier) {
        iteration++;
        NEO4J.iterate();
        GRAKN_CORE.iterate();
        super.runChild(runner, notifier);
        if (iteration == CONFIG.iterations() + 1) {
            GRAKN_CORE.close();
            NEO4J.close();
        }
    }

    private static class Runner extends BlockJUnit4ClassRunner {
        private final int iteration;

        public Runner(Class<?> aClass, int iteration) throws InitializationError {
            super(aClass);
            this.iteration = iteration;
        }

        @Override
        protected String testName(FrameworkMethod method) {
            return method.getName() + "-iter-" + iteration;
        }

        @Override
        protected String getName() {
            return super.getName() + "-iter-" + iteration;
        }
    }

    @CommandLine.Command(name = "benchmark-test", mixinStandardHelpOptions = true)
    private static class TestOptions {

        @CommandLine.Option(names = {"--grakn"}, required = true, description = "Database address URI")
        private String graknAddress;

        @CommandLine.Option(names = {"--neo4j"}, required = true, description = "Database address URI")
        private String neo4jAddress;

        public String graknAddress() {
            return graknAddress;
        }

        public String neo4jAddress() {
            return neo4jAddress;
        }
    }
}
