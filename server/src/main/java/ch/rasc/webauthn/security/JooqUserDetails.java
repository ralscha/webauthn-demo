package ch.rasc.webauthn.security;

import java.util.Collection;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import ch.rasc.webauthn.db.tables.records.AppUserRecord;

public class JooqUserDetails implements UserDetails {

  private static final long serialVersionUID = 1L;

  private final String username;

  private final Long userDbId;

  public JooqUserDetails(AppUserRecord user) {
    this.userDbId = user.getId();
    this.username = user.getUsername();
  }

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return List.of();
  }

  public Long getUserDbId() {
    return this.userDbId;
  }

  @Override
  public String getPassword() {
    return null;
  }

  @Override
  public String getUsername() {
    return this.username;
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
