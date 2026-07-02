// Web Crypto Utility Helpers for Cloudflare Workers

/**
 * Derives a PBKDF2 key from a password and salt.
 */
async function deriveKey(password: string, salt: Uint8Array, iterations: number = 100000): Promise<ArrayBuffer> {
  const encoder = new TextEncoder();
  const passwordBuffer = encoder.encode(password);
  
  const baseKey = await crypto.subtle.importKey(
    "raw",
    passwordBuffer,
    "PBKDF2",
    false,
    ["deriveBits", "deriveKey"]
  );
  
  return await crypto.subtle.deriveBits(
    {
      name: "PBKDF2",
      salt: salt as any,
      iterations: iterations,
      hash: "SHA-256"
    },
    baseKey,
    256 // 32 bytes
  );
}

/**
 * Hashes a password using PBKDF2.
 * Returns "iterations$saltHex$hashHex".
 */
export async function hashPassword(password: string): Promise<string> {
  const salt = crypto.getRandomValues(new Uint8Array(16));
  const hash = await deriveKey(password, salt);
  
  const saltHex = Array.from(salt).map(b => b.toString(16).padStart(2, '0')).join('');
  const hashHex = Array.from(new Uint8Array(hash)).map(b => b.toString(16).padStart(2, '0')).join('');
  
  return `100000$${saltHex}$${hashHex}`;
}

/**
 * Verifies a password against a PBKDF2 hash.
 */
export async function verifyPassword(password: string, storedHash: string): Promise<boolean> {
  try {
    const parts = storedHash.split('$');
    if (parts.length !== 3) return false;
    
    const iterations = parseInt(parts[0], 10);
    const saltHex = parts[1];
    const hashHex = parts[2];
    
    const salt = new Uint8Array(saltHex.match(/.{1,2}/g)!.map(byte => parseInt(byte, 16)));
    const expectedHash = new Uint8Array(hashHex.match(/.{1,2}/g)!.map(byte => parseInt(byte, 16)));
    
    const derivedHash = new Uint8Array(await deriveKey(password, salt, iterations));
    
    // Constant-time comparison
    if (derivedHash.length !== expectedHash.length) return false;
    let result = 0;
    for (let i = 0; i < derivedHash.length; i++) {
      result |= derivedHash[i] ^ expectedHash[i];
    }
    return result === 0;
  } catch (e) {
    return false;
  }
}

/**
 * Generates a secure random session token.
 */
export function generateToken(): string {
  const bytes = crypto.getRandomValues(new Uint8Array(32));
  return Array.from(bytes).map(b => b.toString(16).padStart(2, '0')).join('');
}

/**
 * Generates a random 6-digit numeric verification code.
 */
export function generateVerificationCode(): string {
  const bytes = crypto.getRandomValues(new Uint8Array(4));
  const num = new DataView(bytes.buffer).getUint32(0, true);
  return (num % 1000000).toString().padStart(6, '0');
}
