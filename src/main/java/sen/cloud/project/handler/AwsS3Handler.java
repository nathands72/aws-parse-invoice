package sen.cloud.project.handler;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Date;

import org.json.JSONArray;
import org.json.JSONObject;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;

import sen.cloud.project.common.UtilLogger;

public class AwsS3Handler
{

	public void processS3Notification(String message) throws Exception
	{
		boolean success = false;

		JSONObject json = new JSONObject(message);

		if (json.has("Records")) {
			JSONArray records = json.getJSONArray("Records");

			if (records != null && records.length() > 0) {
				JSONObject record = (JSONObject) records.get(0);

				if (record.has("s3")) {
					JSONObject s3 = record.getJSONObject("s3");

					if (s3.has("bucket") && s3.has("object")) {
						success = true;

						String bucketName = s3.getJSONObject("bucket").getString("name");
						String fileName = s3.getJSONObject("object").getString("key");
						processFile(bucketName, fileName);
					}
				}
			}
		}

		if (!success) {
			throw new Exception("Invalid S3 notification message");
		}
	}

	private void processFile(String bucketName, String fileName) throws Exception
	{
		final AmazonS3ClientBuilder builder = AmazonS3ClientBuilder.standard();
		final AmazonS3 s3 = builder.withRegion(Regions.US_WEST_2).build();

		try {
			UtilLogger.info(">> Will read the file %s from the bucket %s", fileName, bucketName);

			S3Object o = s3.getObject(bucketName, fileName);
			S3ObjectInputStream s3is = o.getObjectContent();
			InputStreamReader inStrm = new InputStreamReader(s3is);
			BufferedReader reader = new BufferedReader(inStrm);
			StringBuilder content = new StringBuilder();

			String line;
			String custId = "";
			String invId = "";
			String dated = "";
			String from = "";
			String to = "";
			String amt = "";
			String sgst = "";
			String total = "";
			String inwords = "";
			while ((line = reader.readLine()) != null) {
				if (line.startsWith("Customer-ID:")) {
					custId = (line.split(":")[1]).trim();
				} else if (line.startsWith("Inv-ID:")) {
					invId = (line.split(":")[1]).trim();
				} else if (line.startsWith("Dated:")) {
					dated = (line.split(":")[1]).trim();
				} else if (line.startsWith("From:")) {
					from = (line.split(":")[1]).trim();
				} else if (line.startsWith("To:")) {
					to = (line.split(":")[1]).trim();
				} else if (line.startsWith("Amount:")) {
					amt = (line.split(":")[1]).trim();
				} else if (line.startsWith("SGST:")) {
					sgst = (line.split(":")[1]).trim();
				} else if (line.startsWith("Total:")) {
					total = (line.split(":")[1]).trim();
				} else if (line.startsWith("InWords:")) {
					inwords = (line.split(":")[1]).trim();
				}

				content.append(System.lineSeparator());
				content.append(line.replace(",", ";"));
			}
			reader.close();
			inStrm.close();
			s3is.close();

			String csvString = custId + "," + invId + "," + dated + "," + from + "," + to + "," + amt + "," + sgst + ","
					+ total + "," + inwords;

			UtilLogger.info(">> File Content");
			UtilLogger.info(content.toString());

			UtilLogger.info(">> CSV Content");
			UtilLogger.info(csvString);

			writeFile("gl-proj-dest-bucket", csvString, custId, invId);

			AwsDynamoDbHandler dynamoDb = new AwsDynamoDbHandler();
			dynamoDb.insertRecord(custId, invId, content.toString(), csvString);

		} catch (Exception e) {
			UtilLogger.error(e.getMessage());
			throw e;
		}
	}

	private void writeFile(String bucketName, String csvString, String custId, String invId) throws Exception
	{
		final AmazonS3ClientBuilder builder = AmazonS3ClientBuilder.standard();
		final AmazonS3 s3 = builder.withRegion(Regions.US_WEST_2).build();

		try {
			Date date = new Date();
			long time = date.getTime();

			String fileName = time + "_" + custId + "_" + invId + ".csv";

			UtilLogger.info(">> Uploading %s to bucket %s", fileName, bucketName);

			s3.putObject(bucketName, fileName, csvString);
		} catch (Exception e) {
			UtilLogger.error(e.getMessage());
			throw e;
		}
	}
}