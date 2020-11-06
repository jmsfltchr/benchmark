package grakn.simulation.db.neo4j.action.write;

import grakn.simulation.db.common.action.write.InsertTransactionAction;
import grakn.simulation.db.common.utils.Pair;
import grakn.simulation.db.common.world.World;
import grakn.simulation.db.neo4j.driver.Neo4jOperation;
import grakn.simulation.db.neo4j.schema.Schema;
import org.neo4j.driver.Query;
import org.neo4j.driver.Record;

import java.util.HashMap;

public class Neo4jInsertTransactionAction extends InsertTransactionAction<Neo4jOperation, Record> {

    public Neo4jInsertTransactionAction(Neo4jOperation dbOperation, World.Country country, Pair<Long, Double> transaction, Long sellerCompanyNumber, double value, int productQuantity, boolean isTaxable) {
        super(dbOperation, country, transaction, sellerCompanyNumber, value, productQuantity, isTaxable);
    }

    @Override
    public Record run() {
        HashMap<String, Object> parameters = new HashMap<String, Object>(){{
            put("barcode", transaction.getSecond());
            put("buyerNumber", transaction.getFirst());
            put("sellerNumber", sellerCompanyNumber);
            put("countryName", country.name());
            put("value", value);
            put("productQuantity", productQuantity);
            put("isTaxable", isTaxable);
        }};
        return singleResult(dbOperation.execute(new Query(query(), parameters)));
    }

    public static String query() {
        return "MATCH (product:Product {barcode: $barcode}),\n" +
                "(buyer:Company {companyNumber: $buyerNumber}),\n" +
                "(seller:Company {companyNumber: $sellerNumber}),\n" +
                "(country:Country {locationName: $countryName})\n" +
                "CREATE (transaction:Transaction{\n" +
                "   value: $value,\n" +
                "   productQuantity: $productQuantity,\n" +
                "   isTaxable: $isTaxable,\n" +
                "   locationName: country.locationName\n" + // This could be a relation, but would be inconsistent with how location is represented elsewhere
                "}),\n" +
                "(transaction)-[:SELLER]->(seller)," +
                "(transaction)-[:BUYER]->(buyer)," +
                "(transaction)-[:MERCHANDISE]->(product)\n" +
                "RETURN seller.companyNumber, buyer.companyNumber, product.barcode, transaction.value, transaction.productQuantity, transaction.isTaxable, country.locationName";
    }

    @Override
    protected HashMap<ComparableField, Object> outputForReport(Record answer) {
        return new HashMap<ComparableField, Object>() {{
            put(InsertTransactionActionField.SELLER, answer.asMap().get("seller" + Schema.COMPANY_NUMBER));
            put(InsertTransactionActionField.BUYER, answer.asMap().get("buyer" + Schema.COMPANY_NUMBER));
            put(InsertTransactionActionField.MERCHANDISE, answer.asMap().get("product." + Schema.PRODUCT_BARCODE));
            put(InsertTransactionActionField.VALUE, answer.asMap().get("transaction." + Schema.VALUE));
            put(InsertTransactionActionField.PRODUCT_QUANTITY, answer.asMap().get("transaction." + Schema.PRODUCT_QUANTITY));
            put(InsertTransactionActionField.IS_TAXABLE, answer.asMap().get("transaction." + Schema.IS_TAXABLE));
            put(InsertTransactionActionField.COUNTRY, answer.asMap().get("country." + Schema.LOCATION_NAME));

        }};
    }
}
