package ch.rasc.webauthn.security;

import static ch.rasc.webauthn.db.tables.AppUser.APP_USER;
import static ch.rasc.webauthn.db.tables.Credentials.CREDENTIALS;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.jooq.DSLContext;
import org.springframework.stereotype.Component;

import com.yubico.webauthn.AssertionResult;
import com.yubico.webauthn.CredentialRepository;
import com.yubico.webauthn.RegisteredCredential;
import com.yubico.webauthn.data.ByteArray;
import com.yubico.webauthn.data.PublicKeyCredentialDescriptor;

import ch.rasc.webauthn.util.BytesUtil;

@Component
public class JooqCredentialRepository implements CredentialRepository {

  private final DSLContext dsl;

  public JooqCredentialRepository(DSLContext dsl) {
    this.dsl = dsl;
  }

  public void addCredential(long userId, byte[] credentialId, byte[] publicKeyCose,
      long counter) {
    this.dsl.insertInto(CREDENTIALS)
        .columns(CREDENTIALS.ID, CREDENTIALS.APP_USER_ID, CREDENTIALS.PUBLIC_KEY_COSE,
            CREDENTIALS.COUNT)
        .values(credentialId, userId, publicKeyCose, counter).execute();
  }

  @Override
  public Set<PublicKeyCredentialDescriptor> getCredentialIdsForUsername(String username) {
    System.out.println("JCR: getCredentialIdsForUsername: " + username);

    var records = this.dsl.select(CREDENTIALS.ID).from(CREDENTIALS).innerJoin(APP_USER)
        .onKey().where(APP_USER.USERNAME.eq(username)).fetch();

    Set<PublicKeyCredentialDescriptor> result = new HashSet<>();

    for (var record : records) {
      PublicKeyCredentialDescriptor descriptor = PublicKeyCredentialDescriptor.builder()
          .id(new ByteArray(record.get(CREDENTIALS.ID))).build();
      result.add(descriptor);
    }

    return result;
  }

  @Override
  public Optional<String> getUsernameForUserHandle(ByteArray userHandle) {
    System.out.println(
        "JCR: getUsernameForUserHandle: " + BytesUtil.bytesToLong(userHandle.getBytes()));

    long id = BytesUtil.bytesToLong(userHandle.getBytes());
    var record = this.dsl.select(APP_USER.USERNAME).from(APP_USER)
        .where(APP_USER.ID.eq(id)).fetchOne();

    if (record != null) {
      return Optional.of(record.get(APP_USER.USERNAME));
    }

    return Optional.empty();
  }

  @Override
  public Optional<ByteArray> getUserHandleForUsername(String username) {
    System.out.println("JCR: getUserHandleForUsername: " + username);

    var record = this.dsl.select(APP_USER.ID).from(APP_USER)
        .where(APP_USER.USERNAME.eq(username)).fetchOne();

    if (record != null) {
      Long id = record.get(APP_USER.ID);
      return Optional.of(new ByteArray(BytesUtil.longToBytes(id)));
    }

    return Optional.empty();
  }

  @Override
  public Optional<RegisteredCredential> lookup(ByteArray credentialId,
      ByteArray userHandle) {
    System.out.println("JCR: lookup: " + credentialId + ":"
        + BytesUtil.bytesToLong(userHandle.getBytes()));

    long id = BytesUtil.bytesToLong(userHandle.getBytes());

    var record = this.dsl
        .select(CREDENTIALS.ID, CREDENTIALS.PUBLIC_KEY_COSE, CREDENTIALS.COUNT)
        .from(CREDENTIALS).innerJoin(APP_USER).onKey()
        .where(APP_USER.ID.eq(id).and(CREDENTIALS.ID.eq(credentialId.getBytes())))
        .fetchOne();

    if (record != null) {
      return Optional.of(RegisteredCredential.builder()
          .credentialId(new ByteArray(record.get(CREDENTIALS.ID))).userHandle(userHandle)
          .publicKeyCose(new ByteArray(record.get(CREDENTIALS.PUBLIC_KEY_COSE)))
          .signatureCount(record.get(CREDENTIALS.COUNT)).build());
    }
    return Optional.empty();
  }

  @Override
  public Set<RegisteredCredential> lookupAll(ByteArray credentialId) {
    System.out.println("JCR: lookupAll: " + credentialId);
    System.out.println("JCR: lookupAll length: " + credentialId.getBytes().length);

    var records = this.dsl
        .select(CREDENTIALS.ID, CREDENTIALS.APP_USER_ID, CREDENTIALS.PUBLIC_KEY_COSE,
            CREDENTIALS.COUNT)
        .from(CREDENTIALS).where(CREDENTIALS.ID.eq(credentialId.getBytes())).fetch();

    Set<RegisteredCredential> result = new HashSet<>();
    for (var record : records) {
      result.add(RegisteredCredential.builder()
          .credentialId(new ByteArray(record.get(CREDENTIALS.ID)))
          .userHandle(
              new ByteArray(BytesUtil.longToBytes(record.get(CREDENTIALS.APP_USER_ID))))
          .publicKeyCose(new ByteArray(record.get(CREDENTIALS.PUBLIC_KEY_COSE)))
          .signatureCount(record.get(CREDENTIALS.COUNT)).build());
    }
    return result;
  }

  public boolean updateSignatureCount(AssertionResult result) {
    System.out
        .println("JCR: updateSignatureCount: " + result.getCredential().getUserHandle());

    long id = BytesUtil.bytesToLong(result.getCredential().getUserHandle().getBytes());
    int noOfUpdates = this.dsl.update(CREDENTIALS)
        .set(CREDENTIALS.COUNT, result.getSignatureCount())
        .where(CREDENTIALS.ID.eq(result.getCredential().getCredentialId().getBytes())
            .and(CREDENTIALS.APP_USER_ID.eq(id)))
        .execute();

    return noOfUpdates == 1;
  }

}
