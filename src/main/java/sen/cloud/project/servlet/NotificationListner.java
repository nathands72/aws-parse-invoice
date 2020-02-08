package sen.cloud.project.servlet;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.security.Signature;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.Scanner;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;

import sen.cloud.project.common.Message;
import sen.cloud.project.common.UtilLogger;
import sen.cloud.project.handler.AwsS3Handler;

@WebServlet(urlPatterns = "/notify", loadOnStartup = 1)
public class NotificationListner extends HttpServlet
{

	private static final long serialVersionUID = 2L;

	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException, SecurityException
	{

		// Get the message type header.
		String messagetype = request.getHeader("x-amz-sns-message-type");
		// If message doesn't have the message type header, don't process it.
		if (messagetype == null) {
			UtilLogger.info(">>Empty message.");
			return;
		}

		// Parse the JSON message in the message body
		// and hydrate a Message object with its contents
		// so that we have easy access to the name-value pairs
		// from the JSON message.
		Scanner scan = new Scanner(request.getInputStream());
		StringBuilder builder = new StringBuilder();
		while (scan.hasNextLine()) {
			builder.append(scan.nextLine());
		}
		scan.close();
		Message msg = readMessageFromJson(builder.toString());

		// The signature is based on SignatureVersion 1.
		// If the sig version is something other than 1,
		// throw an exception.
		if (msg.getSignatureVersion().equals("1")) {
			// Check the signature and throw an exception if the signature verification
			// fails.
			if (isMessageSignatureValid(msg)) {
				UtilLogger.info(">>Signature verification succeeded");
			} else {
				UtilLogger.info(">>Signature verification failed");
				throw new SecurityException("Signature verification failed.");
			}
		} else {
			UtilLogger.info(">>Unexpected signature version. Unable to verify signature.");
			throw new SecurityException("Unexpected signature version. Unable to verify signature.");
		}

		// Process the message based on type.
		if (messagetype.equals("Notification")) {
			// Do something with the Message and Subject.
			// Just log the subject (if it exists) and the message.
			String logMsgAndSubject = ">>Notification received from topic " + msg.getTopicArn();
			if (msg.getSubject() != null) {
				logMsgAndSubject += " Subject: " + msg.getSubject();
			}
			logMsgAndSubject += " Message: " + msg.getMessage();
			UtilLogger.info(logMsgAndSubject);

			AwsS3Handler s3 = new AwsS3Handler();
			try {
				s3.processS3Notification(msg.getMessage());
			} catch (Exception e) {
				throw new ServletException("Process invoice notification failed");
			}

		} else if (messagetype.equals("SubscriptionConfirmation")) {
			// You should make sure that this subscription is from the topic you expect.
			// Compare topicARN to your list of topics
			// that you want to enable to add this endpoint as a subscription.

			// Confirm the subscription by going to the subscribeURL location
			// and capture the return value (XML message body as a string)
			Scanner sc = new Scanner(new URL(msg.getSubscribeURL()).openStream());
			StringBuilder sb = new StringBuilder();
			while (sc.hasNextLine()) {
				sb.append(sc.nextLine());
			}
			sc.close();
			UtilLogger
					.info(">>Subscription confirmation (" + msg.getSubscribeURL() + ") Return value: " + sb.toString());
			// Process the return value to ensure the endpoint is subscribed.
		} else if (messagetype.equals("UnsubscribeConfirmation")) {
			// Handle UnsubscribeConfirmation message.
			// For example, take action if unsubscribing should not have occurred.
			// You can read the SubscribeURL from this message and
			// re-subscribe the endpoint.
			UtilLogger.info(">>Unsubscribe confirmation: " + msg.getMessage());
		} else {
			// Handle unknown message type.
			UtilLogger.info(">>Unknown message type.");
		}
		UtilLogger.info(">>Done processing message: " + msg.getMessageId());

		response.getWriter().println("API invoked; your http record is now saved.");
		response.setStatus(HttpServletResponse.SC_OK);
	}

	private Message readMessageFromJson(String string)
	{

		Message msg = new Message();
		JSONObject json = new JSONObject(string);

		msg.setType(getValue(json, "Type"));
		msg.setMessage(getValue(json, "Message"));
		msg.setMessageId(getValue(json, "MessageId"));
		msg.setToken(getValue(json, "Token"));
		msg.setSubject(getValue(json, "Subject"));
		msg.setTopicArn(getValue(json, "TopicArn"));
		msg.setSignature(getValue(json, "Signature"));
		msg.setSignatureVersion(getValue(json, "SignatureVersion"));
		msg.setTimestamp(getValue(json, "Timestamp"));
		msg.setSigningCertURL(getValue(json, "SigningCertURL"));
		msg.setSubscribeURL(getValue(json, "SubscribeURL"));

		UtilLogger.info(msg.getMessage());

		return msg;
	}

	private String getValue(JSONObject json, String key)
	{
		if (json.has(key)) {
			return json.getString(key);
		}

		return "";
	}

	private static boolean isMessageSignatureValid(Message msg)
	{
		try {
			URL url = new URL(msg.getSigningCertURL());
			verifyMessageSignatureURL(msg, url);

			InputStream inStream = url.openStream();
			CertificateFactory cf = CertificateFactory.getInstance("X.509");
			X509Certificate cert = (X509Certificate) cf.generateCertificate(inStream);
			inStream.close();

			Signature sig = Signature.getInstance("SHA1withRSA");
			sig.initVerify(cert.getPublicKey());
			sig.update(getMessageBytesToSign(msg));
			return sig.verify(Base64.getDecoder().decode(msg.getSignature()));
		} catch (Exception e) {
			throw new SecurityException("Verify method failed.", e);
		}
	}

	private static void verifyMessageSignatureURL(Message msg, URL endpoint)
	{
		URI certUri = URI.create(msg.getSigningCertURL());

		if (!"https".equals(certUri.getScheme())) {
			throw new SecurityException("SigningCertURL was not using HTTPS: " + certUri.toString());
		}

		if (!endpoint.getHost().equals(certUri.getHost())) {
			throw new SecurityException(String.format(
					"SigningCertUrl does not match expected endpoint. " + "Expected %s but received endpoint was %s.",
					endpoint, certUri.getHost()));

		}
	}

	private static byte[] getMessageBytesToSign(Message msg)
	{
		byte[] bytesToSign = null;
		if (msg.getType().equals("Notification"))
			bytesToSign = buildNotificationStringToSign(msg).getBytes();
		else if (msg.getType().equals("SubscriptionConfirmation") || msg.getType().equals("UnsubscribeConfirmation"))
			bytesToSign = buildSubscriptionStringToSign(msg).getBytes();
		return bytesToSign;
	}

	// Build the string to sign for Notification messages.
	public static String buildNotificationStringToSign(Message msg)
	{
		String stringToSign = null;

		// Build the string to sign from the values in the message.
		// Name and values separated by newline characters
		// The name value pairs are sorted by name
		// in byte sort order.
		stringToSign = "Message\n";
		stringToSign += msg.getMessage() + "\n";
		stringToSign += "MessageId\n";
		stringToSign += msg.getMessageId() + "\n";
		if (msg.getSubject() != null) {
			stringToSign += "Subject\n";
			stringToSign += msg.getSubject() + "\n";
		}
		stringToSign += "Timestamp\n";
		stringToSign += msg.getTimestamp() + "\n";
		stringToSign += "TopicArn\n";
		stringToSign += msg.getTopicArn() + "\n";
		stringToSign += "Type\n";
		stringToSign += msg.getType() + "\n";
		return stringToSign;
	}

	// Build the string to sign for SubscriptionConfirmation
	// and UnsubscribeConfirmation messages.
	public static String buildSubscriptionStringToSign(Message msg)
	{
		String stringToSign = null;
		// Build the string to sign from the values in the message.
		// Name and values separated by newline characters
		// The name value pairs are sorted by name
		// in byte sort order.
		stringToSign = "Message\n";
		stringToSign += msg.getMessage() + "\n";
		stringToSign += "MessageId\n";
		stringToSign += msg.getMessageId() + "\n";
		stringToSign += "SubscribeURL\n";
		stringToSign += msg.getSubscribeURL() + "\n";
		stringToSign += "Timestamp\n";
		stringToSign += msg.getTimestamp() + "\n";
		stringToSign += "Token\n";
		stringToSign += msg.getToken() + "\n";
		stringToSign += "TopicArn\n";
		stringToSign += msg.getTopicArn() + "\n";
		stringToSign += "Type\n";
		stringToSign += msg.getType() + "\n";
		return stringToSign;
	}
}
