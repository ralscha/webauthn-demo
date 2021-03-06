/*
 * This file is generated by jOOQ.
 */
package ch.rasc.webauthn.db.tables;

import java.util.Arrays;
import java.util.List;

import org.jooq.Field;
import org.jooq.ForeignKey;
import org.jooq.Index;
import org.jooq.Name;
import org.jooq.Record;
import org.jooq.Row4;
import org.jooq.Schema;
import org.jooq.Table;
import org.jooq.TableField;
import org.jooq.TableOptions;
import org.jooq.UniqueKey;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;
import org.jooq.impl.TableImpl;

import ch.rasc.webauthn.db.Indexes;
import ch.rasc.webauthn.db.Keys;
import ch.rasc.webauthn.db.Webauthn;
import ch.rasc.webauthn.db.tables.records.CredentialsRecord;

/**
 * This class is generated by jOOQ.
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class Credentials extends TableImpl<CredentialsRecord> {

  private static final long serialVersionUID = 1L;

  /**
   * The reference instance of <code>webauthn.credentials</code>
   */
  public static final Credentials CREDENTIALS = new Credentials();

  /**
   * The class holding records for this type
   */
  @Override
  public Class<CredentialsRecord> getRecordType() {
    return CredentialsRecord.class;
  }

  /**
   * The column <code>webauthn.credentials.id</code>.
   */
  public final TableField<CredentialsRecord, byte[]> ID = createField(DSL.name("id"),
      SQLDataType.VARBINARY(128).nullable(false), this, "");

  /**
   * The column <code>webauthn.credentials.app_user_id</code>.
   */
  public final TableField<CredentialsRecord, Long> APP_USER_ID = createField(
      DSL.name("app_user_id"), SQLDataType.BIGINT.nullable(false), this, "");

  /**
   * The column <code>webauthn.credentials.count</code>.
   */
  public final TableField<CredentialsRecord, Long> COUNT = createField(DSL.name("count"),
      SQLDataType.BIGINT.nullable(false), this, "");

  /**
   * The column <code>webauthn.credentials.public_key_cose</code>.
   */
  public final TableField<CredentialsRecord, byte[]> PUBLIC_KEY_COSE = createField(
      DSL.name("public_key_cose"), SQLDataType.VARBINARY(500).nullable(false), this, "");

  private Credentials(Name alias, Table<CredentialsRecord> aliased) {
    this(alias, aliased, null);
  }

  private Credentials(Name alias, Table<CredentialsRecord> aliased,
      Field<?>[] parameters) {
    super(alias, null, aliased, parameters, DSL.comment(""), TableOptions.table());
  }

  /**
   * Create an aliased <code>webauthn.credentials</code> table reference
   */
  public Credentials(String alias) {
    this(DSL.name(alias), CREDENTIALS);
  }

  /**
   * Create an aliased <code>webauthn.credentials</code> table reference
   */
  public Credentials(Name alias) {
    this(alias, CREDENTIALS);
  }

  /**
   * Create a <code>webauthn.credentials</code> table reference
   */
  public Credentials() {
    this(DSL.name("credentials"), null);
  }

  public <O extends Record> Credentials(Table<O> child,
      ForeignKey<O, CredentialsRecord> key) {
    super(child, key, CREDENTIALS);
  }

  @Override
  public Schema getSchema() {
    return Webauthn.WEBAUTHN;
  }

  @Override
  public List<Index> getIndexes() {
    return Arrays.<Index>asList(Indexes.CREDENTIALS_APP_USER_ID);
  }

  @Override
  public UniqueKey<CredentialsRecord> getPrimaryKey() {
    return Keys.KEY_CREDENTIALS_PRIMARY;
  }

  @Override
  public List<UniqueKey<CredentialsRecord>> getKeys() {
    return Arrays.<UniqueKey<CredentialsRecord>>asList(Keys.KEY_CREDENTIALS_PRIMARY);
  }

  @Override
  public List<ForeignKey<CredentialsRecord, ?>> getReferences() {
    return Arrays.<ForeignKey<CredentialsRecord, ?>>asList(Keys.CREDENTIALS_IBFK_1);
  }

  private transient AppUser _appUser;

  public AppUser appUser() {
    if (this._appUser == null) {
      this._appUser = new AppUser(this, Keys.CREDENTIALS_IBFK_1);
    }

    return this._appUser;
  }

  @Override
  public Credentials as(String alias) {
    return new Credentials(DSL.name(alias), this);
  }

  @Override
  public Credentials as(Name alias) {
    return new Credentials(alias, this);
  }

  /**
   * Rename this table
   */
  @Override
  public Credentials rename(String name) {
    return new Credentials(DSL.name(name), null);
  }

  /**
   * Rename this table
   */
  @Override
  public Credentials rename(Name name) {
    return new Credentials(name, null);
  }

  // -------------------------------------------------------------------------
  // Row4 type methods
  // -------------------------------------------------------------------------

  @Override
  public Row4<byte[], Long, Long, byte[]> fieldsRow() {
    return (Row4) super.fieldsRow();
  }
}
