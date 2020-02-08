package sen.cloud.project.handler;

import java.util.HashMap;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.model.AttributeDefinition;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.CreateTableRequest;
import com.amazonaws.services.dynamodbv2.model.DescribeTableResult;
import com.amazonaws.services.dynamodbv2.model.KeySchemaElement;
import com.amazonaws.services.dynamodbv2.model.KeyType;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput;
import com.amazonaws.services.dynamodbv2.model.ResourceNotFoundException;
import com.amazonaws.services.dynamodbv2.model.ScalarAttributeType;
import com.amazonaws.services.dynamodbv2.model.TableDescription;
import com.amazonaws.services.dynamodbv2.model.TableStatus;

import sen.cloud.project.common.UtilLogger;

public class AwsDynamoDbHandler
{
	private String tableName = "invoice";

	protected void insertRecord(String custId, String invId, String content, String csvFormat) throws Exception
	{
		AmazonDynamoDBClientBuilder builder = AmazonDynamoDBClientBuilder.standard();
		AmazonDynamoDB dynamoDb = builder.withRegion(Regions.US_WEST_2).build();

		// Verify whether the invoice table exists and it is active
		boolean isTableActive = false;
		for (int i = 1; i <= 5; i++) {
			if (isTableActive(dynamoDb)) {
				isTableActive = true;
				break;
			}
			Thread.sleep(1000L);
		}

		if (isTableActive) {
			insertRecord(dynamoDb, custId, invId, content, csvFormat);
		} else {
			throw new Exception("Table invoice is not active.");
		}
	}

	private boolean isTableActive(AmazonDynamoDB dynamoDb) throws Exception
	{
		try {
			DescribeTableResult descTable = dynamoDb.describeTable(tableName);
			TableDescription table = descTable.getTable();
			if (table.getTableStatus().equals(TableStatus.ACTIVE.toString())) {
				return true;
			}
		} catch (ResourceNotFoundException e) {
			createTable(dynamoDb);
		} catch (Exception e) {
			UtilLogger.error(e.getMessage());
			throw e;
		}

		return false;
	}

	private void createTable(AmazonDynamoDB dynamoDb) throws Exception
	{
		UtilLogger.info(">> Creating table invoice");

		CreateTableRequest request = new CreateTableRequest()
				.withAttributeDefinitions(new AttributeDefinition("cust_id", ScalarAttributeType.S),
						new AttributeDefinition("inv_id", ScalarAttributeType.S))
				.withKeySchema(new KeySchemaElement("cust_id", KeyType.HASH),
						new KeySchemaElement("inv_id", KeyType.RANGE))
				.withProvisionedThroughput(new ProvisionedThroughput(new Long(1), new Long(1)))
				.withTableName(tableName);

		try {
			dynamoDb.createTable(request);
			UtilLogger.info(">> Done!");
		} catch (Exception e) {
			UtilLogger.error(e.getMessage());
			throw e;
		}
	}

	private void insertRecord(AmazonDynamoDB dynamoDb, String custId, String invId, String content, String csvFormat)
			throws Exception
	{
		UtilLogger.info(">> Inserting data in the table");

		HashMap<String, AttributeValue> itemValues = new HashMap<String, AttributeValue>();

		itemValues.put("cust_id", new AttributeValue(custId));
		itemValues.put("inv_id", new AttributeValue(invId));
		itemValues.put("details", new AttributeValue(content));
		itemValues.put("csvdtls", new AttributeValue(csvFormat));

		try {
			dynamoDb.putItem(tableName, itemValues);
			UtilLogger.info(">> Done!");
		} catch (Exception e) {
			UtilLogger.error(e.getMessage());
			throw e;
		}
	}
}
