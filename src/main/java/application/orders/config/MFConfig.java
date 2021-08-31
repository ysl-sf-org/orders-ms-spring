package application.orders.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@EnableConfigurationProperties
@ConfigurationProperties(prefix = "mf")
public class MFConfig {

	private String appId;
	private String authUrl;
	private String pushUrl;
	private String clientId;
	private String secret;

	public String getSecret() {
		return secret;
	}

	public String getAppId() {
		return appId;
	}

	public void setAppId(String appId) {
		this.appId = appId;
	}

	public void setSecret(final String secret) {
		this.secret = secret;
	}

	public String getClientId() {
		return clientId;
	}

	public void setClientId(final String clientId) {
		this.clientId = clientId;
	}

	public String getPushUrl() {
		return pushUrl;
	}

	public void setPushUrl(final String pushUrl) {
		this.pushUrl = pushUrl;
	}

	public String getAuthUrl() {
		return authUrl;
	}

	public void setAuthUrl(final String authUrl) {
		this.authUrl = authUrl;
	}

}