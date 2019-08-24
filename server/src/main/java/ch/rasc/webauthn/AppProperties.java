package ch.rasc.webauthn;

import java.net.URL;
import java.util.Set;

import javax.validation.constraints.NotEmpty;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "app")
@Validated
public class AppProperties {

	@NotEmpty
	private String relyingPartyId;

	@NotEmpty
	private String relyingPartyName;

	private URL relyingPartyIcon;

	@NotEmpty
	private Set<String> relyingPartyOrigins;

	public String getRelyingPartyId() {
		return this.relyingPartyId;
	}

	public void setRelyingPartyId(String relyingPartyId) {
		this.relyingPartyId = relyingPartyId;
	}

	public String getRelyingPartyName() {
		return this.relyingPartyName;
	}

	public void setRelyingPartyName(String relyingPartyName) {
		this.relyingPartyName = relyingPartyName;
	}

	public URL getRelyingPartyIcon() {
		return this.relyingPartyIcon;
	}

	public void setRelyingPartyIcon(URL relyingPartyIcon) {
		this.relyingPartyIcon = relyingPartyIcon;
	}

	public Set<String> getRelyingPartyOrigins() {
		return this.relyingPartyOrigins;
	}

	public void setRelyingPartyOrigins(Set<String> relyingPartyOrigins) {
		this.relyingPartyOrigins = relyingPartyOrigins;
	}

}
