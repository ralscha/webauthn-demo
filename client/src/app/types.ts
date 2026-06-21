/**
 * JSON-serialized representation of a PublicKeyCredential,
 * as returned by PublicKeyCredential.toJSON().
 */
export type PublicKeyCredentialJSON = ReturnType<PublicKeyCredential['toJSON']>;
