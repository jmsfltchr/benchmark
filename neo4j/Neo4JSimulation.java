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

package grakn.benchmark.neo4j;

import grakn.benchmark.config.Config;
import grakn.benchmark.neo4j.action.Neo4jActionFactory;
import grakn.benchmark.neo4j.driver.Neo4jClient;
import grakn.benchmark.neo4j.driver.Neo4jSession;
import grakn.benchmark.neo4j.driver.Neo4jTransaction;
import grakn.benchmark.neo4j.loader.Neo4jYAMLLoader;
import grakn.benchmark.simulation.Simulation;
import grakn.benchmark.simulation.action.ActionFactory;
import grakn.benchmark.simulation.common.SimulationContext;
import grakn.benchmark.simulation.loader.YAMLException;
import grakn.benchmark.simulation.loader.YAMLLoader;
import org.neo4j.driver.Query;
import org.neo4j.driver.Session;
import org.neo4j.driver.Transaction;

import java.io.File;
import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Neo4JSimulation extends Simulation<Neo4jClient, Neo4jSession, Neo4jTransaction> {

    private static final File CYPHER_TEMPLATES = Paths.get("neo4j/data/cypher_templates.yml").toFile();

    private Neo4JSimulation(Neo4jClient driver, Map<String, Path> dataFiles, int randomSeed, List<Config.Agent> agentConfigs, SimulationContext context) throws Exception {
        super(driver, dataFiles, randomSeed, agentConfigs, context);
    }

    public static Neo4JSimulation create(String hostUri, Map<String, Path> files, int randomSeed, List<Config.Agent> agentConfigs, SimulationContext context) throws Exception {
        return new Neo4JSimulation(new Neo4jClient(hostUri), files, randomSeed, agentConfigs, context);
    }

    @Override
    protected ActionFactory<Neo4jTransaction, ?> actionFactory() {
        return new Neo4jActionFactory();
    }

    @Override
    protected void initialise(Map<String, Path> dataFiles) {
        try (org.neo4j.driver.Session session = client().unpack().session()) {
            addKeyConstraints(session);
            cleanDatabase(session);
            YAMLLoader loader = new Neo4jYAMLLoader(session, dataFiles);
            try {
                loader.loadFile(CYPHER_TEMPLATES);
            } catch (YAMLException | FileNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void cleanDatabase(Session session) {
        Transaction tx = session.beginTransaction();
        tx.run(new Query("MATCH (n) DETACH DELETE n"));
        tx.commit();
    }

    /**
     * Neo4j Community can create only uniqueness constraints, and only on nodes, not relationships. This means that it
     * does not enforce the existence of a property on those nodes. `exists()` is only available in Neo4j Enterprise.
     * https://neo4j.com/developer/kb/how-to-implement-a-primary-key-property-for-a-label/
     *
     * @param session
     */
    private void addKeyConstraints(Session session) {
        List<String> queries = new ArrayList<>() {{
            add("CREATE CONSTRAINT unique_person_email ON (person:Person) ASSERT person.email IS UNIQUE");
            add("CREATE CONSTRAINT unique_location_locationName ON (location:Location) ASSERT location.locationName IS UNIQUE");
            add("CREATE CONSTRAINT unique_company_companyName ON (company:Company) ASSERT company.companyName IS UNIQUE");
            add("CREATE CONSTRAINT unique_company_companyNumber ON (company:Company) ASSERT company.companyNumber IS UNIQUE");
            add("CREATE CONSTRAINT unique_product_productBarcode ON (product:Product) ASSERT product.productBarcode IS UNIQUE");
        }};
        Transaction tx = session.beginTransaction();
        for (String query : queries) {
            tx.run(new Query(query));
        }
        tx.commit();
    }
}
