package ch.rasc.webauthn.security;

import java.util.Set;

import org.springframework.security.core.GrantedAuthority;

import ch.rasc.webauthn.db.tables.records.AppUserRecord;

public class AppUserDetail {

  private final Long appUserId;

  private final String username;

  private final Set<GrantedAuthority> authorities;

  public AppUserDetail(AppUserRecord user, GrantedAuthority authority) {
    this.appUserId = user.getId();
    this.username = user.getUsername();
    this.authorities = Set.of(authority);
  }

  public Long getAppUserId() {
    return this.appUserId;
  }

  public String getUsername() {
    return this.username;
  }

  public Set<GrantedAuthority> getAuthorities() {
    return this.authorities;
  }

}
