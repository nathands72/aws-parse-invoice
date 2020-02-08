package sen.cloud.project.common;

public class Message {

	private String type;
	private String signatureVersion;
	private String signature;
	private String subject;
	private String message;
	private String messageId;
	private String topicArn;
	private String subscribeURL;
	private String signingCertURL;
	private String timestamp;
	private String token;
	
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	public String getSignatureVersion() {
		return signatureVersion;
	}
	public void setSignatureVersion(String signatureVersion) {
		this.signatureVersion = signatureVersion;
	}
	public String getSignature() {
		return signature;
	}
	public void setSignature(String signature) {
		this.signature = signature;
	}
	public String getSubject() {
		return subject;
	}
	public void setSubject(String subject) {
		this.subject = subject;
	}
	public String getMessage() {
		return message;
	}
	public void setMessage(String message) {
		this.message = message;
	}
	public String getMessageId() {
		return messageId;
	}
	public void setMessageId(String messageId) {
		this.messageId = messageId;
	}
	public String getTopicArn() {
		return topicArn;
	}
	public void setTopicArn(String topicArn) {
		this.topicArn = topicArn;
	}
	public String getSubscribeURL() {
		return subscribeURL;
	}
	public void setSubscribeURL(String subscribeURL) {
		this.subscribeURL = subscribeURL;
	}
	public String getSigningCertURL() {
		return signingCertURL;
	}
	public void setSigningCertURL(String signingCertURL) {
		this.signingCertURL = signingCertURL;
	}
	public String getTimestamp() {
		return timestamp;
	}
	public void setTimestamp(String timestamp) {
		this.timestamp = timestamp;
	}
	public String getToken() {
		return token;
	}
	public void setToken(String token) {
		this.token = token;
	}
}
