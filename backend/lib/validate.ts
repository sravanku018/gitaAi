import { MAX_USER_ID_LENGTH, MAX_NAME_LENGTH, MAX_EMAIL_LENGTH, MIN_PASSWORD_LENGTH } from "../config.ts";

export interface ValidationError {
  field: string;
  message: string;
}

export function validateUserId(user_id: unknown): ValidationError | null {
  if (!user_id || typeof user_id !== "string") {
    return { field: "user_id", message: "user_id is required and must be a string" };
  }
  if (user_id.length < 1 || user_id.length > MAX_USER_ID_LENGTH) {
    return { field: "user_id", message: `user_id must be 1-${MAX_USER_ID_LENGTH} characters` };
  }
  if (!/^[a-zA-Z0-9_-]+$/.test(user_id)) {
    return { field: "user_id", message: "user_id may only contain letters, numbers, underscores, and hyphens" };
  }
  return null;
}

export function validatePassword(password: unknown): ValidationError | null {
  if (!password || typeof password !== "string") {
    return { field: "password", message: "password is required" };
  }
  if (password.length < MIN_PASSWORD_LENGTH) {
    return { field: "password", message: `password must be at least ${MIN_PASSWORD_LENGTH} characters` };
  }
  return null;
}

export function validateEmail(email: unknown): ValidationError | null {
  if (email === undefined || email === "") return null; // optional
  if (typeof email !== "string") {
    return { field: "email", message: "email must be a string" };
  }
  if (email.length > MAX_EMAIL_LENGTH) {
    return { field: "email", message: `email too long (max ${MAX_EMAIL_LENGTH})` };
  }
  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
  if (!emailRegex.test(email)) {
    return { field: "email", message: "invalid email format" };
  }
  return null;
}

export function validateName(name: unknown): ValidationError | null {
  if (name === undefined || name === "") return null;
  if (typeof name !== "string") {
    return { field: "name", message: "name must be a string" };
  }
  if (name.length > MAX_NAME_LENGTH) {
    return { field: "name", message: `name too long (max ${MAX_NAME_LENGTH})` };
  }
  return null;
}

export function sanitizeUserId(raw: unknown): string | null {
  if (typeof raw !== "string") return null;
  const trimmed = raw.trim();
  return trimmed.length > 0 ? trimmed : null;
}
