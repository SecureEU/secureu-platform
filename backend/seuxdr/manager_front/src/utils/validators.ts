export function validateName(name: string): string | null {
  if (name.length < 2 || name.length > 50) {
    return 'Name must be between 2 and 50 characters';
  }

  for (let i = 0; i < name.length; i++) {
    const char = name[i];
    const isLetter = /^[A-Za-z]$/.test(char);
    const isValidSymbol = char === '-' || char === "'";

    if (isLetter) continue;

    if (isValidSymbol) {
      if (i === 0 || i === name.length - 1) {
        return 'Name cannot start or end with a hyphen or apostrophe';
      }
      continue;
    }

    return 'Name contains invalid characters';
  }

  return null;
}

export function validatePassword(password: string): string | null {
  const reasons: string[] = [];

  if (password.length < 8) reasons.push('at least 8 characters');
  if (!/[0-9]/.test(password)) reasons.push('a number');
  if (!/[A-Z]/.test(password)) reasons.push('an uppercase letter');
  if (!/[a-z]/.test(password)) reasons.push('a lowercase letter');
  if (!/[!@#$%^&*(),.?":{}|<>_\-\\/~+=`]/.test(password)) reasons.push('a special character');
  if (/\s/.test(password)) reasons.push('no spaces');

  return reasons.length > 0 ? `Password must contain ${reasons.join(', ')}` : null;
}

export function validateEmail(email: string): string | null {
  if (!email) {
    return 'Email is required';
  }

  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

  if (!emailRegex.test(email)) {
    return 'Invalid email format';
  }

  return null;
}

export const nameValidator = (_: any, value: string) => {
  const error = validateName(value);
  return error ? Promise.reject(new Error(error)) : Promise.resolve();
};

export const passwordValidator = (_: any, value: string) => {
  const error = validatePassword(value);
  return error ? Promise.reject(new Error(error)) : Promise.resolve();
};

export const emailValidator = (_: any, value: string) => {
  const error = validateEmail(value);
  return error ? Promise.reject(new Error(error)) : Promise.resolve();
};
