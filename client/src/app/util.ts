import {fromByteArray, toByteArray} from 'base64-js';

export function base64ToUint8Array(str: string): Uint8Array {
  str = str + '===='.substring(0, (4 - (str.length % 4)) % 4);
  return toByteArray(str);
}

export function uint8ArrayTobase64(input: Uint8Array): string {
  return fromByteArray(new Uint8Array(input)).replace(/\+/g, '-')
    .replace(/\//g, '_')
    .replace(/=/g, '');
}
