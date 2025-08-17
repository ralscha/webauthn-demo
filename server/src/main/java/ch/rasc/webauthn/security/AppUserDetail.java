package ch.rasc.webauthn.security;

import java.util.Collection;
import java.util.Set;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import ch.rasc.javersdemo.db.tables.records.AppUserRecord;

public class AppUserDetail implements UserDetails {

  private static final long serialVersionUID = 1L;

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

  @Override
  public String getUsername() {
    return this.username;
  }

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return this.authorities;
  }

  @Override
  public String getPassword() {
    // WebAuthn doesn't use passwords
    return null;
  }

  @Override
  public boolean isAccountNonExpired() {
    return true;
  }

  @Override
  public boolean isAccountNonLocked() {
    return true;
  }

  @Override
  public boolean isCredentialsNonExpired() {
    return true;
  }

  @Override
  public boolean isEnabled() {
    return true;
  }

}
