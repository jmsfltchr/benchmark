/*
 * Copyright (C) 2020 Grakn Labs
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

package grakn.benchmark.simulation.agent.write;

import grakn.benchmark.simulation.action.Action;
import grakn.benchmark.simulation.action.ActionFactory;
import grakn.benchmark.simulation.agent.Agent;
import grakn.benchmark.simulation.agent.base.RandomValueGenerator;
import grakn.benchmark.simulation.agent.base.SimulationContext;
import grakn.benchmark.simulation.driver.Session;
import grakn.benchmark.simulation.driver.Transaction;
import grakn.benchmark.simulation.driver.Client;
import grakn.benchmark.simulation.world.World;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static java.util.stream.Collectors.toList;

public class ProductAgent<TX extends Transaction> extends Agent<World.Continent, TX> {

    public ProductAgent(Client<? extends Session<TX>, TX> client, ActionFactory<TX, ?> actionFactory, SimulationContext context) {
        super(client, actionFactory, context);
    }

    @Override
    protected List<World.Continent> getRegions(World world) {
        return world.getContinents().collect(toList());
    }

    @Override
    protected List<Action<?, ?>.Report> run(Session<TX> session, World.Continent region, Random random) {
        List<Action<?, ?>.Report> reports = new ArrayList<>();
        int numProducts = context.world().getScaleFactor();
        try (TX dbOperation = session.newTransaction(region.tracker(), context.iteration(), isTracing())) {
            for (int i = 0; i < numProducts; i++) {
                String productName = RandomValueGenerator.of(random).boundRandomLengthRandomString(5, 20);
                String productDescription = RandomValueGenerator.of(random).boundRandomLengthRandomString(75, 100);
                long barcode = uniqueId(context, region.tracker(), i).hashCode();
                runAction(actionFactory().insertProductAction(dbOperation, region, barcode, productName, productDescription), reports);
            }
            dbOperation.commit();
        }
        return reports;
    }
}
