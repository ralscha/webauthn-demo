/*
 * This file is generated by jOOQ.
 */
package ch.rasc.webauthn.db.tables.records;

import org.jooq.Field;
import org.jooq.Record2;
import org.jooq.Record5;
import org.jooq.Row5;
import org.jooq.impl.UpdatableRecordImpl;

import ch.rasc.webauthn.db.tables.Credentials;

/**
 * This class is generated by jOOQ.
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class CredentialsRecord extends UpdatableRecordImpl<CredentialsRecord>
    implements Record5<byte[], Long, Long, byte[], String> {

  private static final long serialVersionUID = 1L;

  /**
   * Setter for <code>webauthn.credentials.id</code>.
   */
  public void setId(byte[] value) {
    set(0, value);
  }

  /**
   * Getter for <code>webauthn.credentials.id</code>.
   */
  public byte[] getId() {
    return (byte[]) get(0);
  }

  /**
   * Setter for <code>webauthn.credentials.app_user_id</code>.
   */
  public void setAppUserId(Long value) {
    set(1, value);
  }

  /**
   * Getter for <code>webauthn.credentials.app_user_id</code>.
   */
  public Long getAppUserId() {
    return (Long) get(1);
  }

  /**
   * Setter for <code>webauthn.credentials.count</code>.
   */
  public void setCount(Long value) {
    set(2, value);
  }

  /**
   * Getter for <code>webauthn.credentials.count</code>.
   */
  public Long getCount() {
    return (Long) get(2);
  }

  /**
   * Setter for <code>webauthn.credentials.public_key_cose</code>.
   */
  public void setPublicKeyCose(byte[] value) {
    set(3, value);
  }

  /**
   * Getter for <code>webauthn.credentials.public_key_cose</code>.
   */
  public byte[] getPublicKeyCose() {
    return (byte[]) get(3);
  }

  /**
   * Setter for <code>webauthn.credentials.transports</code>.
   */
  public void setTransports(String value) {
    set(4, value);
  }

  /**
   * Getter for <code>webauthn.credentials.transports</code>.
   */
  public String getTransports() {
    return (String) get(4);
  }

  // -------------------------------------------------------------------------
  // Primary key information
  // -------------------------------------------------------------------------

  @Override
  public Record2<byte[], Long> key() {
    return (Record2) super.key();
  }

  // -------------------------------------------------------------------------
  // Record5 type implementation
  // -------------------------------------------------------------------------

  @Override
  public Row5<byte[], Long, Long, byte[], String> fieldsRow() {
    return (Row5) super.fieldsRow();
  }

  @Override
  public Row5<byte[], Long, Long, byte[], String> valuesRow() {
    return (Row5) super.valuesRow();
  }

  @Override
  public Field<byte[]> field1() {
    return Credentials.CREDENTIALS.ID;
  }

  @Override
  public Field<Long> field2() {
    return Credentials.CREDENTIALS.APP_USER_ID;
  }

  @Override
  public Field<Long> field3() {
    return Credentials.CREDENTIALS.COUNT;
  }

  @Override
  public Field<byte[]> field4() {
    return Credentials.CREDENTIALS.PUBLIC_KEY_COSE;
  }

  @Override
  public Field<String> field5() {
    return Credentials.CREDENTIALS.TRANSPORTS;
  }

  @Override
  public byte[] component1() {
    return getId();
  }

  @Override
  public Long component2() {
    return getAppUserId();
  }

  @Override
  public Long component3() {
    return getCount();
  }

  @Override
  public byte[] component4() {
    return getPublicKeyCose();
  }

  @Override
  public String component5() {
    return getTransports();
  }

  @Override
  public byte[] value1() {
    return getId();
  }

  @Override
  public Long value2() {
    return getAppUserId();
  }

  @Override
  public Long value3() {
    return getCount();
  }

  @Override
  public byte[] value4() {
    return getPublicKeyCose();
  }

  @Override
  public String value5() {
    return getTransports();
  }

  @Override
  public CredentialsRecord value1(byte[] value) {
    setId(value);
    return this;
  }

  @Override
  public CredentialsRecord value2(Long value) {
    setAppUserId(value);
    return this;
  }

  @Override
  public CredentialsRecord value3(Long value) {
    setCount(value);
    return this;
  }

  @Override
  public CredentialsRecord value4(byte[] value) {
    setPublicKeyCose(value);
    return this;
  }

  @Override
  public CredentialsRecord value5(String value) {
    setTransports(value);
    return this;
  }

  @Override
  public CredentialsRecord values(byte[] value1, Long value2, Long value3, byte[] value4,
      String value5) {
    value1(value1);
    value2(value2);
    value3(value3);
    value4(value4);
    value5(value5);
    return this;
  }

  // -------------------------------------------------------------------------
  // Constructors
  // -------------------------------------------------------------------------

  /**
   * Create a detached CredentialsRecord
   */
  public CredentialsRecord() {
    super(Credentials.CREDENTIALS);
  }

  /**
   * Create a detached, initialised CredentialsRecord
   */
  public CredentialsRecord(byte[] id, Long appUserId, Long count, byte[] publicKeyCose,
      String transports) {
    super(Credentials.CREDENTIALS);

    setId(id);
    setAppUserId(appUserId);
    setCount(count);
    setPublicKeyCose(publicKeyCose);
    setTransports(transports);
  }
}
