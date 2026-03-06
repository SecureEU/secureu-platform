package validators

import (
	"errors"
	"fmt"
	"strings"
	"unicode"
)

func verifyPassword(s string) (longEnough, hasNumber, hasUpper, hasLower, hasSpecial, hasNoSpace bool) {
	var length int
	hasNoSpace = true

	for _, c := range s {
		switch {
		case unicode.IsSpace(c):
			hasNoSpace = false
		case unicode.IsNumber(c):
			hasNumber = true
		case unicode.IsUpper(c):
			hasUpper = true
		case unicode.IsLower(c):
			hasLower = true
		case unicode.IsPunct(c) || unicode.IsSymbol(c):
			hasSpecial = true
		}
		if !unicode.IsSpace(c) {
			length++
		}
	}

	longEnough = length >= 8
	return
}

func ValidatePassword(password string) error {
	long, num, upper, lower, special, noSpace := verifyPassword(password)

	if !long || !num || !upper || !lower || !special || !noSpace {
		var reasons []string
		if !long {
			reasons = append(reasons, "at least 8 characters")
		}
		if !num {
			reasons = append(reasons, "a number")
		}
		if !upper {
			reasons = append(reasons, "an uppercase letter")
		}
		if !lower {
			reasons = append(reasons, "a lowercase letter")
		}
		if !special {
			reasons = append(reasons, "a special character")
		}
		if !noSpace {
			reasons = append(reasons, "no spaces")
		}
		return fmt.Errorf("password must contain %s", joinReasons(reasons))
	}

	return nil
}

// joinReasons formats reason list in a readable way.
func joinReasons(reasons []string) string {
	n := len(reasons)
	if n == 1 {
		return reasons[0]
	}
	return fmt.Sprintf("%s and %s",
		strings.Join(reasons[:n-1], ", "),
		reasons[n-1],
	)
}

// ValidateName checks that the name contains only letters, hyphens or apostrophes,
// and is between 2 and 50 characters long, without starting/ending with hyphen or apostrophe.
func ValidateName(name string) error {
	if len(name) < 2 || len(name) > 50 {
		return errors.New("name must be between 2 and 50 characters")
	}

	for i, r := range name {
		switch {
		case unicode.IsLetter(r):
			continue
		case r == '-' || r == '\'':
			if i == 0 || i == len(name)-1 {
				return errors.New("name cannot start or end with a hyphen or apostrophe")
			}
		default:
			return errors.New("name contains invalid characters")
		}
	}

	return nil
}
