package ch.rasc.webauthn.security;

import static ch.rasc.javersdemo.db.tables.Credentials.CREDENTIALS;

import java.nio.ByteBuffer;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jooq.DSLContext;
import org.springframework.security.web.webauthn.api.AuthenticatorTransport;
import org.springframework.security.web.webauthn.api.Bytes;
import org.springframework.security.web.webauthn.api.CredentialRecord;
import org.springframework.security.web.webauthn.api.ImmutableCredentialRecord;
import org.springframework.security.web.webauthn.api.ImmutablePublicKeyCose;
import org.springframework.security.web.webauthn.api.PublicKeyCredentialType;
import org.springframework.security.web.webauthn.management.UserCredentialRepository;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import ch.rasc.javersdemo.db.tables.records.CredentialsRecord;

public class JooqUserCredentialRepository implements UserCredentialRepository {

  private final DSLContext dsl;

  public JooqUserCredentialRepository(DSLContext dsl) {
    this.dsl = dsl;
  }

  @Override
  public void delete(Bytes credentialId) {
    Assert.notNull(credentialId, "credentialId cannot be null");

    this.dsl.deleteFrom(CREDENTIALS).where(CREDENTIALS.ID.eq(credentialId.getBytes()))
        .execute();
  }

  @Override
  public void save(CredentialRecord credentialRecord) {
    Assert.notNull(credentialRecord, "credentialRecord cannot be null");

    CredentialsRecord existingRecord = this.dsl.selectFrom(CREDENTIALS)
        .where(CREDENTIALS.ID.eq(credentialRecord.getCredentialId().getBytes()))
        .fetchOne();

    if (existingRecord != null) {
      updateCredentialRecord(credentialRecord);
    }
    else {
      insertCredentialRecord(credentialRecord);
    }
  }

  private void insertCredentialRecord(CredentialRecord record) {
    List<String> transports = new ArrayList<>();
    if (!CollectionUtils.isEmpty(record.getTransports())) {
      for (AuthenticatorTransport transport : record.getTransports()) {
        transports.add(transport.getValue());
      }
    }

    this.dsl.insertInto(CREDENTIALS)
        .set(CREDENTIALS.ID, record.getCredentialId().getBytes())
        .set(CREDENTIALS.APP_USER_ID, getUserIdFromBytes(record.getUserEntityUserId()))
        .set(CREDENTIALS.PUBLIC_KEY, record.getPublicKey().getBytes())
        .set(CREDENTIALS.SIGNATURE_COUNT, record.getSignatureCount())
        .set(CREDENTIALS.UV_INITIALIZED, record.isUvInitialized())
        .set(CREDENTIALS.BACKUP_ELIGIBLE, record.isBackupEligible())
        .set(CREDENTIALS.AUTHENTICATOR_TRANSPORTS, String.join(",", transports))
        .set(CREDENTIALS.PUBLIC_KEY_CREDENTIAL_TYPE,
            record.getCredentialType() != null ? record.getCredentialType().getValue()
                : null)
        .set(CREDENTIALS.BACKUP_STATE, record.isBackupState())
        .set(CREDENTIALS.ATTESTATION_OBJECT,
            record.getAttestationObject() != null
                ? record.getAttestationObject().getBytes()
                : null)
        .set(CREDENTIALS.ATTESTATION_CLIENT_DATA_JSON,
            record.getAttestationClientDataJSON() != null
                ? record.getAttestationClientDataJSON().getBytes()
                : null)
        .set(CREDENTIALS.CREATED,
            record.getCreated() != null
                ? LocalDateTime.ofInstant(record.getCreated(), ZoneOffset.UTC)
                : null)
        .set(CREDENTIALS.LAST_USED,
            record.getLastUsed() != null
                ? LocalDateTime.ofInstant(record.getLastUsed(), ZoneOffset.UTC)
                : null)
        .set(CREDENTIALS.LABEL, record.getLabel()).execute();
  }

  private void updateCredentialRecord(CredentialRecord record) {
    List<String> transports;
    if (!CollectionUtils.isEmpty(record.getTransports())) {
      transports = record.getTransports().stream().map(AuthenticatorTransport::getValue)
          .toList();
    }
    else {
      transports = List.of();
    }

    this.dsl.update(CREDENTIALS)
        .set(CREDENTIALS.APP_USER_ID, getUserIdFromBytes(record.getUserEntityUserId()))
        .set(CREDENTIALS.PUBLIC_KEY, record.getPublicKey().getBytes())
        .set(CREDENTIALS.SIGNATURE_COUNT, record.getSignatureCount())
        .set(CREDENTIALS.UV_INITIALIZED, record.isUvInitialized())
        .set(CREDENTIALS.BACKUP_ELIGIBLE, record.isBackupEligible())
        .set(CREDENTIALS.AUTHENTICATOR_TRANSPORTS, String.join(",", transports))
        .set(CREDENTIALS.PUBLIC_KEY_CREDENTIAL_TYPE,
            record.getCredentialType() != null ? record.getCredentialType().getValue()
                : null)
        .set(CREDENTIALS.BACKUP_STATE, record.isBackupState())
        .set(CREDENTIALS.ATTESTATION_OBJECT,
            record.getAttestationObject() != null
                ? record.getAttestationObject().getBytes()
                : null)
        .set(CREDENTIALS.ATTESTATION_CLIENT_DATA_JSON,
            record.getAttestationClientDataJSON() != null
                ? record.getAttestationClientDataJSON().getBytes()
                : null)
        .set(CREDENTIALS.CREATED,
            record.getCreated() != null
                ? LocalDateTime.ofInstant(record.getCreated(), ZoneOffset.UTC)
                : null)
        .set(CREDENTIALS.LAST_USED,
            record.getLastUsed() != null
                ? LocalDateTime.ofInstant(record.getLastUsed(), ZoneOffset.UTC)
                : null)
        .set(CREDENTIALS.LABEL, record.getLabel())
        .where(CREDENTIALS.ID.eq(record.getCredentialId().getBytes())).execute();
  }

  @Override
  public CredentialRecord findByCredentialId(Bytes credentialId) {
    Assert.notNull(credentialId, "credentialId cannot be null");

    CredentialsRecord record = this.dsl.selectFrom(CREDENTIALS)
        .where(CREDENTIALS.ID.eq(credentialId.getBytes())).fetchOne();

    return record != null ? mapToCredentialRecord(record) : null;
  }

  @Override
  public List<CredentialRecord> findByUserId(Bytes userId) {
    Assert.notNull(userId, "userId cannot be null");

    List<CredentialsRecord> records = this.dsl.selectFrom(CREDENTIALS)
        .where(CREDENTIALS.APP_USER_ID.eq(getUserIdFromBytes(userId))).fetch();

    List<CredentialRecord> result = new ArrayList<>();
    for (CredentialsRecord record : records) {
      result.add(mapToCredentialRecord(record));
    }
    return result;
  }

  private static CredentialRecord mapToCredentialRecord(CredentialsRecord record) {
    Set<AuthenticatorTransport> transports = new HashSet<>();
    String transportStr = record.getAuthenticatorTransports();
    if (StringUtils.hasText(transportStr)) {
      String[] transportArray = transportStr.split(",");
      for (String transport : transportArray) {
        if (StringUtils.hasText(transport.trim())) {
          transports.add(AuthenticatorTransport.valueOf(transport.trim()));
        }
      }
    }

    return ImmutableCredentialRecord.builder().credentialId(new Bytes(record.getId()))
        .userEntityUserId(getBytesFromUserId(record.getAppUserId()))
        .publicKey(new ImmutablePublicKeyCose(record.getPublicKey()))
        .signatureCount(record.getSignatureCount())
        .uvInitialized(
            record.getUvInitialized() != null ? record.getUvInitialized() : false)
        .backupEligible(record.getBackupEligible())
        .credentialType(record.getPublicKeyCredentialType() != null
            ? PublicKeyCredentialType.valueOf(record.getPublicKeyCredentialType())
            : null)
        .backupState(record.getBackupState())
        .attestationObject(record.getAttestationObject() != null
            ? new Bytes(record.getAttestationObject())
            : null)
        .attestationClientDataJSON(record.getAttestationClientDataJson() != null
            ? new Bytes(record.getAttestationClientDataJson())
            : null)
        .created(
            record.getCreated() != null ? record.getCreated().toInstant(ZoneOffset.UTC)
                : null)
        .lastUsed(
            record.getLastUsed() != null ? record.getLastUsed().toInstant(ZoneOffset.UTC)
                : null)
        .label(record.getLabel()).transports(transports).build();
  }

  public static Long getUserIdFromBytes(Bytes userEntityId) {
    byte[] bytes = userEntityId.getBytes();
    if (bytes.length == 8) {
      return ByteBuffer.wrap(bytes).getLong();
    }
    throw new IllegalArgumentException("Invalid user entity ID format");
  }

  public static Bytes getBytesFromUserId(Long appUserId) {
    return new Bytes(ByteBuffer.allocate(8).putLong(appUserId).array());
  }

}
