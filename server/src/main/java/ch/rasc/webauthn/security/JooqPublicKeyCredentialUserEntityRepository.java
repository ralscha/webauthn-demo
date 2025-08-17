package ch.rasc.webauthn.security;

import static ch.rasc.javersdemo.db.tables.AppUser.APP_USER;

import org.jooq.DSLContext;
import org.springframework.security.web.webauthn.api.Bytes;
import org.springframework.security.web.webauthn.api.ImmutablePublicKeyCredentialUserEntity;
import org.springframework.security.web.webauthn.api.PublicKeyCredentialUserEntity;
import org.springframework.security.web.webauthn.management.PublicKeyCredentialUserEntityRepository;
import org.springframework.util.Assert;

import ch.rasc.javersdemo.db.tables.records.AppUserRecord;

public class JooqPublicKeyCredentialUserEntityRepository
    implements PublicKeyCredentialUserEntityRepository {

  private final DSLContext dsl;

  public JooqPublicKeyCredentialUserEntityRepository(DSLContext dsl) {
    this.dsl = dsl;
  }

  @Override
  public PublicKeyCredentialUserEntity findById(Bytes id) {
    Assert.notNull(id, "id cannot be null");

    Long userId = JooqUserCredentialRepository.getUserIdFromBytes(id);
    AppUserRecord record = this.dsl.selectFrom(APP_USER).where(APP_USER.ID.eq(userId))
        .fetchOne();

    return record != null ? mapToUserEntity(record) : null;
  }

  @Override
  public PublicKeyCredentialUserEntity findByUsername(String username) {
    Assert.hasText(username, "username cannot be null or empty");

    AppUserRecord record = this.dsl.selectFrom(APP_USER)
        .where(APP_USER.USERNAME.eq(username)).fetchOne();

    return record != null ? mapToUserEntity(record) : null;
  }

  @Override
  public void save(PublicKeyCredentialUserEntity userEntity) {
    Assert.notNull(userEntity, "userEntity cannot be null");

    Long userId = JooqUserCredentialRepository.getUserIdFromBytes(userEntity.getId());

    AppUserRecord existingRecord = this.dsl.selectFrom(APP_USER)
        .where(APP_USER.ID.eq(userId)).fetchOne();

    if (existingRecord != null) {
      this.dsl.update(APP_USER).set(APP_USER.USERNAME, userEntity.getName())
          .where(APP_USER.ID.eq(userId)).execute();
    }
    else {
      this.dsl.insertInto(APP_USER).set(APP_USER.USERNAME, userEntity.getName())
          .execute();
    }
  }

  @Override
  public void delete(Bytes id) {
    Assert.notNull(id, "id cannot be null");

    Long userId = JooqUserCredentialRepository.getUserIdFromBytes(id);
    this.dsl.deleteFrom(APP_USER).where(APP_USER.ID.eq(userId)).execute();
  }

  private static PublicKeyCredentialUserEntity mapToUserEntity(AppUserRecord record) {
    return ImmutablePublicKeyCredentialUserEntity.builder()
        .id(JooqUserCredentialRepository.getBytesFromUserId(record.getId()))
        .name(record.getUsername()).displayName(record.getUsername()).build();
  }

}
