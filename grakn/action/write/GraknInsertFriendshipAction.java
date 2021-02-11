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

package grakn.benchmark.grakn.action.write;

import grakn.benchmark.common.action.Action;
import grakn.benchmark.common.action.write.InsertFriendshipAction;
import grakn.benchmark.grakn.driver.GraknOperation;
import grakn.client.concept.answer.ConceptMap;
import graql.lang.pattern.variable.UnboundVariable;
import graql.lang.query.GraqlInsert;

import java.time.LocalDateTime;
import java.util.HashMap;

import static grakn.benchmark.grakn.action.Model.EMAIL;
import static grakn.benchmark.grakn.action.Model.FRIENDSHIP;
import static grakn.benchmark.grakn.action.Model.FRIENDSHIP_FRIEND;
import static grakn.benchmark.grakn.action.Model.FRIENDSHIP_START_DATE;
import static grakn.benchmark.grakn.action.Model.PERSON;
import static grakn.benchmark.grakn.action.Model.RESIDENCY_START_DATE;
import static graql.lang.Graql.match;
import static graql.lang.Graql.not;
import static graql.lang.Graql.var;

public class GraknInsertFriendshipAction extends InsertFriendshipAction<GraknOperation, ConceptMap> {

    public GraknInsertFriendshipAction(GraknOperation dbOperation, LocalDateTime today, String friend1Email, String friend2Email) {
        super(dbOperation, today, friend1Email, friend2Email);
    }

    @Override
    public ConceptMap run() {
        return Action.optionalSingleResult(dbOperation.execute(query(today, friend1Email, friend2Email)));
    }

    public static GraqlInsert query(LocalDateTime today, String friend1Email, String friend2Email) {
        UnboundVariable person1 = var("p1");
        UnboundVariable person2 = var("p2");
        UnboundVariable friendship = var();

        return match(
                person1
                        .isa(PERSON).has(EMAIL, friend1Email),
                person2
                        .isa(PERSON).has(EMAIL, friend2Email),
                not(
                        friendship
                                .rel(FRIENDSHIP_FRIEND, person1)
                                .rel(FRIENDSHIP_FRIEND, person2)
                                .isa(FRIENDSHIP)
                )
        ).insert(
                var(FRIENDSHIP)
                        .rel(FRIENDSHIP_FRIEND, person1)
                        .rel(FRIENDSHIP_FRIEND, person2)
                        .isa(FRIENDSHIP)
                        .has(FRIENDSHIP_START_DATE, today)
        );
    }

    @Override
    protected HashMap<ComparableField, Object> outputForReport(ConceptMap answer) {
        return new HashMap<ComparableField, Object>() {
            {
                put(InsertFriendshipActionField.FRIEND1_EMAIL, dbOperation.getOnlyAttributeOfThing(answer, "p1", EMAIL));
                put(InsertFriendshipActionField.FRIEND2_EMAIL, dbOperation.getOnlyAttributeOfThing(answer, "p2", EMAIL));
                put(InsertFriendshipActionField.START_DATE, dbOperation.getOnlyAttributeOfThing(answer, FRIENDSHIP, RESIDENCY_START_DATE));
            }
        };

    }
}
