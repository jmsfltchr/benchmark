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

package grakn.simulation.benchmark

import grakn.simulation.common.agent.insight.*
import grakn.simulation.common.agent.write.AgeUpdateAgent
import grakn.simulation.common.agent.write.PersonBirthAgent
import grakn.simulation.grakn.action.insight.*
import grakn.simulation.grakn.action.read.GraknBirthsInCityAction
import grakn.simulation.grakn.action.write.GraknUpdateAgesOfPeopleInCityAction
import grakn.simulation.neo4j.action.insight.*
import grakn.simulation.neo4j.action.read.Neo4jBirthsInCityAction
import grakn.simulation.neo4j.action.write.Neo4jUpdateAgesOfPeopleInCityAction
import groovy.text.GStringTemplateEngine

import java.time.LocalDateTime

class Report {

    static LinkedHashMap<String, String> agentDescriptions() {
        LinkedHashMap<String, String> desc = [:]

        desc."MarriageAgent" = """Retrieves a set of men and a set of women from a given city, who are not in a marriage 
already. Creates random pairings between them, and creates a marriage relation between the pair. This marriage is 
related to the city in which it took place by another relation."""

        desc."PersonBirthAgent" = """Simulates people being born into the world simulation. This adds each person as an 
entity, with attributes Email, Forename, Surname, Gender, Date of Birth and creates a relation from that person to their 
place of birth."""
        
        desc."AgeUpdateAgent" = """Increments the Age attribute of all people in the simulation by 1 year."""
        
        desc."ParentshipAgent" = """Finds children born in a city and married couples who don't have children. 
Distributes the children across the marriages, relating them together."""

        desc."CompanyAgent" = """Inserts new companies with a name and number, relating them to the country in which 
they are incorporated, also recording the date of incorporation."""

        desc."ProductAgent" = """Inserts new products, each with a name, barcode and description. Relates each to its 
continent of origin."""

        desc."TransactionAgent" = """For a given country, finds finds companies incorporated there and products that are 
produced in the country's continent. Creates transactions between random pairs of those companies, with one as the buyer, 
one as the seller, and a product as the merchandise."""

        desc."ArbitraryOneHopAgent" = """Matches for a given person and anything that is one relation away."""
        
        desc."TwoHopAgent" = """Find all of the parents of children who were born in London."""
        
        desc."FindSpecificMarriageAgent" = """Retrieve a specific marriage (a relation) by it's identifier (a key)."""
        
        desc."FindSpecificPersonAgent" = """Retrieve a specific person (an entity) by their email (a key)."""

//        desc."Employment" = """Finds existing people and makes them employees of companies.""" // Not enabled yet

        return desc
    }

    static String outputFile = "/Users/jamesfletcher/programming/simulation/benchmark/tmp/report.tex"
    def engine

    static void main(String[] args) {
        def report = new Report()
        def output = report.render()
        def writer = new FileWriter(new File(outputFile))
        writer.write(output)
        writer.close()
    }

    Report() {
        engine = new GStringTemplateEngine()
    }

    abstract class Section {
        abstract String title()

        abstract String render()
    }

    class DbAgentSection extends Section {
        private List<String> queries
        private String name
        private String description

        DbAgentSection(String name, List<String> queries) {
            this.queries = queries
            this.name = name
            this.description = agentDescriptions().get(name)
        }

        @Override
        String title() {
            return name
        }

        @Override
        String render() {
            return engine.createTemplate(new File("benchmark/templates/agent_queries.tex")).make([databaseName: title(), queries: queries])
        }
    }

    class AgentSection extends Section {
        private final Class<?> agentClass
        private List<DbAgentSection> dbAgents = new ArrayList<>()
        private String title
        private String agentName

        AgentSection(Class<?> agentClass, List<String> graknQueries, List<String> neo4jQueries) {
            this.agentClass = agentClass
            this.agentName = agentClass.getSimpleName()
            this.title = this.agentName

            String suffix = "Agent"
            if (this.title.endsWith(suffix)) {
                this.title = this.title.substring(0, this.title.length() - suffix.length())
            }
            dbAgents.add(new DbAgentSection("Grakn", graknQueries))
            dbAgents.add(new DbAgentSection("Neo4j", neo4jQueries))
        }

        AgentSection(Class<?> agentClass, String graknQueries, String neo4jQueries) {
            this(agentClass, Arrays.asList(graknQueries), Arrays.asList(neo4jQueries))
        }

        @Override
        String title() {
            return this.title
        }

        @Override
        String render() {
            List<String> renderedDbAgents = []
            for (dbAgent in dbAgents) {
                renderedDbAgents.add(dbAgent.render())
            }
            return engine.createTemplate(new File("benchmark/templates/agent_section.tex")).make([agentSectionTitle: title(), agentName: this.agentName, renderedDbAgents: renderedDbAgents])
        }
    }

    String renderAgentSection() {
        String output = ""
        for (agentSection in agentSections()) {
            output = output.concat(agentSection.render())
        }
        return output
    }

    static String renderIntroduction() {
        return new File("benchmark/templates/introduction.tex").readLines().join("\n")
    }

    static String renderAgentSectionIntroduction() {
        return new File("benchmark/templates/agent_intro.tex").readLines().join("\n")
    }

    static String renderEnd() {
        return '\\end{document}'
    }

    String render() {
        String output = renderIntroduction() + "\n"
        output = output.concat(renderAgentSectionIntroduction())
        output = output.concat(renderAgentSection())
        output = output.concat(renderEnd())
        return output
    }

    List<AgentSection> agentSections() {
        String cityName = "{}"
        String email = "{}"
        long age = 5
        LocalDateTime dummyDate = LocalDateTime.of(0, 1, 1, 0, 0)

        List<AgentSection> agentSections = Arrays.asList(
                new AgentSection(
                        ArbitraryOneHopAgent.class,
                        GraknArbitraryOneHopAction.query().toString(),
                        Neo4jArbitraryOneHopAction.query()
                ),
                new AgentSection(
                        FindCurrentResidentsAgent.class,
                        GraknFindCurrentResidentsAction.query().toString(),
                        Neo4jFindCurrentResidentsAction.query()
                ),
                new AgentSection(
                        FindLivedInAgent.class,
                        GraknFindLivedInAction.query().toString(),
                        Neo4jFindLivedInAction.query()
                ),
                new AgentSection(
                        FindSpecificMarriageAgent.class,
                        GraknFindSpecificMarriageAction.query().toString(),
                        Neo4jFindSpecificMarriageAction.query()
                ),
                new AgentSection(
                        FindSpecificPersonAgent.class,
                        GraknFindSpecificPersonAction.query().toString(),
                        Neo4jFindSpecificPersonAction.query()
                ),
//                Disabled due to long runtimes
//                new AgentSection(
//                        FindTransactionCurrencyAgent.class,
//                        GraknFindTransactionCurrencyAction.query().toString(),
//                        Neo4jFindTransactionCurrencyAction.query()
//                ),
                new AgentSection(
                        FourHopAgent.class,
                        GraknFourHopAction.query().toString(),
                        Neo4jFourHopAction.query()
                ),
                new AgentSection(
                        MeanWageAgent.class,
                        GraknMeanWageOfPeopleInWorldAction.query().toString(),
                        Neo4jMeanWageOfPeopleInWorldAction.query()
                ),
                new AgentSection(
                        ThreeHopAgent.class,
                        GraknThreeHopAction.query().toString(),
                        Neo4jThreeHopAction.query()
                ),
                new AgentSection(
                        TwoHopAgent.class,
                        GraknTwoHopAction.query().toString(),
                        Neo4jTwoHopAction.query()
                ),
                new AgentSection(
                        PersonBirthAgent.class,
                        GraknBirthsInCityAction.query(cityName, dummyDate).toString(),
                        Neo4jBirthsInCityAction.query()
                ),
                new AgentSection(
                        AgeUpdateAgent.class,
                        Arrays.asList(
                                GraknUpdateAgesOfPeopleInCityAction.getPeopleBornInCityQuery(cityName).toString(),
                                GraknUpdateAgesOfPeopleInCityAction.deleteHasQuery(email).toString(),
                                GraknUpdateAgesOfPeopleInCityAction.insertNewAgeQuery(email, age).toString()
                        ),
                        Arrays.asList(
                                Neo4jUpdateAgesOfPeopleInCityAction.query()
                        )
                ),
        )
        return agentSections
    }
}
