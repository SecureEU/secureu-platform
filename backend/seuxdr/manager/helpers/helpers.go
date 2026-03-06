package helpers

import (
	"SEUXDR/manager/db"
	"archive/tar"
	"archive/zip"
	"compress/gzip"
	"crypto/rand"
	"crypto/rsa"
	"crypto/x509"
	"encoding/hex"
	"encoding/pem"
	"errors"
	"fmt"
	"io"
	"math/big"
	"os"
	"path/filepath"
	"regexp"
	"strconv"
	"strings"
	"unicode"

	"github.com/gin-gonic/gin"
)

func FileExists(filename string) bool {
	_, err := os.Stat(filename)
	return !errors.Is(err, os.ErrNotExist)
}

func JsonResponseWithMessage(err bool, message string) JsonResponse {
	return JsonResponse{Error: err, Message: message}
}

func DeleteFiles(files []string) error {
	var err error
	for _, file := range files {
		if err = os.Remove(file); err != nil {
			err = fmt.Errorf("failed to delete %s: %w", file, err)
		}
	}
	return err
}

// GetLatestCA retrieves the CA with the latest valid_until date from a list of CAs
func GetLatestCA(cas []*db.CA) (*db.CA, error) {
	if len(cas) == 0 {
		return nil, fmt.Errorf("the CA list is empty")
	}

	// Assume the first CA is the latest initially
	latestCA := cas[0]

	for _, ca := range cas[1:] {
		if ca.ValidUntil.After(latestCA.ValidUntil) {
			latestCA = ca
		}
	}

	return latestCA, nil
}

// GetLatestCA retrieves the CA with the latest valid_until date from a list of CAs
func GetLatestServerCert(cas []*db.ServerCert) (*db.ServerCert, error) {
	if len(cas) == 0 {
		return nil, fmt.Errorf("the CA list is empty")
	}

	// Assume the first CA is the latest initially
	latestCA := cas[0]

	for _, ca := range cas[1:] {
		if ca.ValidUntil.After(latestCA.ValidUntil) {
			latestCA = ca
		}
	}

	return latestCA, nil
}

func CopyFile(src, dst string) error {
	in, err := os.Open(src)
	if err != nil {
		return err
	}
	defer in.Close()

	out, err := os.Create(dst)
	if err != nil {
		return err
	}
	defer out.Close()

	_, err = io.Copy(out, in)
	return err
}

func CopyFolder(srcFolder, dstFolder string) error {
	// Get the base name of the source folder (e.g., if srcFolder is "/path/to/src", srcBase will be "src")
	srcBase := filepath.Base(srcFolder)

	// Define the new destination folder to include the source folder itself
	dstFolderWithSrc := filepath.Join(dstFolder, srcBase)
	return filepath.Walk(srcFolder, func(path string, info os.FileInfo, err error) error {
		if err != nil {
			return err
		}

		relPath, err := filepath.Rel(srcFolder, path)
		if err != nil {
			return err
		}

		destPath := filepath.Join(dstFolderWithSrc, relPath)

		if info.IsDir() {
			return os.MkdirAll(destPath, info.Mode())
		} else {
			return CopyFile(path, destPath)
		}
	})
}

func CleanUpTempDir(tempDir string) error {
	return os.RemoveAll(tempDir)
}

func CreateTempDir(dir string, orgName string, groupId string) (string, error) {
	tempDir, err := os.MkdirTemp(dir, "build_"+orgName+"_"+groupId)
	if err != nil {
		return "", fmt.Errorf("failed to create temp dir: %w", err)
	}
	return tempDir, nil
}

func CreatePemFile(filepath string, pemData []byte) error {
	// Create (or overwrite) the file
	file, err := os.Create(filepath)
	if err != nil {
		return err
	}
	defer file.Close()

	// Write the PEM data directly to the file
	_, err = file.Write(pemData)
	if err != nil {
		return err
	}

	return nil
}
func CompressToZip(sourceFiles []string, destinationZip string) error {
	// Create the destination zip file
	zipFile, err := os.Create(destinationZip)
	if err != nil {
		return fmt.Errorf("failed to create zip file: %w", err)
	}
	defer zipFile.Close()

	// Create a new zip writer
	zipWriter := zip.NewWriter(zipFile)
	defer zipWriter.Close()

	for _, sourceFile := range sourceFiles {
		// Add the source file to the zip
		err = addFileToZip(zipWriter, sourceFile)
		if err != nil {
			return fmt.Errorf("failed to add file to zip: %w", err)
		}
	}

	return nil
}

func addFileToZip(zipWriter *zip.Writer, sourceFile string) error {
	file, err := os.Open(sourceFile)
	if err != nil {
		return fmt.Errorf("failed to open source file: %w", err)
	}
	defer file.Close()

	// Get file info
	info, err := file.Stat()
	if err != nil {
		return fmt.Errorf("failed to get file info: %w", err)
	}

	// Create a zip header based on the file info
	header, err := zip.FileInfoHeader(info)
	if err != nil {
		return fmt.Errorf("failed to create zip header: %w", err)
	}

	// Use the full file path in the zip file
	header.Name = filepath.Base(sourceFile)
	header.Method = zip.Deflate // Use compression

	// Create a writer inside the zip
	writer, err := zipWriter.CreateHeader(header)
	if err != nil {
		return fmt.Errorf("failed to create zip writer: %w", err)
	}

	// Copy the file content into the zip writer
	_, err = io.Copy(writer, file)
	if err != nil {
		return fmt.Errorf("failed to write file to zip: %w", err)
	}

	return nil
}

// CompressToTarGz compresses files and directories into a tar.gz archive.
func CompressToTarGz(sourcePaths []string, destinationTarGz string) error {
	// Create the tar.gz file
	tarGzFile, err := os.Create(destinationTarGz)
	if err != nil {
		return fmt.Errorf("failed to create tar.gz file: %w", err)
	}
	defer tarGzFile.Close()

	// Create gzip writer
	gzipWriter := gzip.NewWriter(tarGzFile)
	defer gzipWriter.Close()

	// Create tar writer
	tarWriter := tar.NewWriter(gzipWriter)
	defer tarWriter.Close()

	// Process each source path
	for _, sourcePath := range sourcePaths {
		err := addToTar(tarWriter, sourcePath, "")
		if err != nil {
			return fmt.Errorf("failed to add %s to tar: %w", sourcePath, err)
		}
	}

	return nil
}

// addToTar adds files and directories to the tar archive.
func addToTar(tarWriter *tar.Writer, sourcePath, baseDir string) error {
	info, err := os.Stat(sourcePath)
	if err != nil {
		return fmt.Errorf("failed to get file info: %w", err)
	}

	// Determine tar header name
	headerName := filepath.Join(baseDir, filepath.Base(sourcePath))

	// If it's a directory, recursively add its contents
	if info.IsDir() {
		entries, err := os.ReadDir(sourcePath)
		if err != nil {
			return fmt.Errorf("failed to read directory: %w", err)
		}

		// Add an entry for the directory itself
		header, err := tar.FileInfoHeader(info, "")
		if err != nil {
			return fmt.Errorf("failed to create tar header: %w", err)
		}
		header.Name = headerName + "/"
		if err := tarWriter.WriteHeader(header); err != nil {
			return fmt.Errorf("failed to write directory header: %w", err)
		}

		// Recursively add files inside the directory
		for _, entry := range entries {
			entryPath := filepath.Join(sourcePath, entry.Name())
			err := addToTar(tarWriter, entryPath, headerName)
			if err != nil {
				return err
			}
		}
		return nil
	}

	// Otherwise, it's a file, so add it to the tar
	return addFileToTar(tarWriter, sourcePath, headerName)
}

// addFileToTar adds a single file to the tar archive.
func addFileToTar(tarWriter *tar.Writer, sourceFile, tarName string) error {
	file, err := os.Open(sourceFile)
	if err != nil {
		return fmt.Errorf("failed to open source file: %w", err)
	}
	defer file.Close()

	info, err := file.Stat()
	if err != nil {
		return fmt.Errorf("failed to get file info: %w", err)
	}

	header, err := tar.FileInfoHeader(info, "")
	if err != nil {
		return fmt.Errorf("failed to create tar header: %w", err)
	}
	header.Name = tarName // Preserve relative path inside tar

	if err := tarWriter.WriteHeader(header); err != nil {
		return fmt.Errorf("failed to write tar header: %w", err)
	}

	if _, err := io.Copy(tarWriter, file); err != nil {
		return fmt.Errorf("failed to write file to tar: %w", err)
	}

	return nil
}

func SavePrivateKeyToFile(privateKey *rsa.PrivateKey, filepath string) error {
	// Convert the RSA private key to DER format
	privDER, err := x509.MarshalPKCS8PrivateKey(privateKey)
	if err != nil {
		return err
	}

	// Create a PEM block with the private key
	privBlock := &pem.Block{
		Type:  "PRIVATE KEY",
		Bytes: privDER,
	}

	// Create the file to save the private key
	privateKeyFile, err := os.Create(filepath)
	if err != nil {
		return err
	}
	defer privateKeyFile.Close()

	// Write the PEM block to the file
	return pem.Encode(privateKeyFile, privBlock)
}

func SavePublicKeyToFile(publicKey *rsa.PublicKey, filepath string) error {
	// Convert the RSA public key to DER format
	pubDER, err := x509.MarshalPKIXPublicKey(publicKey)
	if err != nil {
		return err
	}

	// Create a PEM block with the public key
	pubBlock := &pem.Block{
		Type:  "PUBLIC KEY",
		Bytes: pubDER,
	}

	// Create the file to save the public key
	publicKeyFile, err := os.Create(filepath)
	if err != nil {
		return err
	}
	defer publicKeyFile.Close()

	// Write the PEM block to the file
	return pem.Encode(publicKeyFile, pubBlock)
}

func GenerateRSAKeyPair(bits int) (*rsa.PrivateKey, *rsa.PublicKey, error) {
	// Generate the RSA private key
	privateKey, err := rsa.GenerateKey(rand.Reader, bits)
	if err != nil {
		return nil, nil, err
	}

	// Extract the public key from the private key
	publicKey := &privateKey.PublicKey

	return privateKey, publicKey, nil
}

// ConvertPrivateKeyToBytesPKCS8 converts an RSA private key to a PKCS#8 byte slice.
func ConvertPrivateKeyToBytesPKCS8(privateKey *rsa.PrivateKey) ([]byte, error) {
	return x509.MarshalPKCS8PrivateKey(privateKey)
}

// ConvertPublicKeyToBytes converts an RSA public key to DER encoded bytes.
func ConvertPublicKeyToBytes(publicKey *rsa.PublicKey) ([]byte, error) {
	return x509.MarshalPKIXPublicKey(publicKey)
}

func ConvertBytesToPrivateKey(privateKeyBytes []byte) (*rsa.PrivateKey, error) {
	privateKey, err := x509.ParsePKCS8PrivateKey(privateKeyBytes)
	if err != nil {
		return nil, err
	}

	rsaPrivateKey, ok := privateKey.(*rsa.PrivateKey)
	if !ok {
		return nil, fmt.Errorf("key is not an RSA private key")
	}

	return rsaPrivateKey, nil
}

func ConvertBytesToPublicKey(publicKeyBytes []byte) (*rsa.PublicKey, error) {
	publicKey, err := x509.ParsePKIXPublicKey(publicKeyBytes)
	if err != nil {
		return nil, err
	}

	rsaPublicKey, ok := publicKey.(*rsa.PublicKey)
	if !ok {
		return nil, fmt.Errorf("key is not an RSA public key")
	}
	return rsaPublicKey, nil

}

var timestampPatterns = []string{
	`^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}(?:Z|[+-]\d{2}:\d{2})`, // ISO 8601
	`^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}(\.\d+)?([+-]\d{2}:\d{2}|Z)`,
	`^\w{3} \d{2} \d{2}:\d{2}:\d{2}`,                        // syslog
	`^\w{3}, \d{2} \w{3} \d{4} \d{2}:\d{2}:\d{2} [+-]\d{4}`, // RFC 2822
	`^\d{4}-\d{2}-\d{2} \d{2}:\d{2}`,
	`^\d{10}`,
	`^\d{2}\/\d{2}\/\d{4} \d{2}:\d{2}:\d{2}(?: [AP]M)?`,
	`^\d{2}\/\d{2}\/\d{4} \d{2}:\d{2}:\d{2}`,
	`^\[\d{2}\/\w{3}\/\d{4}:\d{2}:\d{2}:\d{2} [+-]\d{4}\]`,
	`^\d{8}\d{6}`,
	`^\w{3,9}, \w{3,9} \d{1,2}, \d{4} \d{2}:\d{2}:\d{2}`,
	`^\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}`,
}

// Syslog parsing function
func ParseSyslog(syslog string) (string, string, string, error) {
	// Find matching timestamp
	var timestamp string
	for _, pattern := range timestampPatterns {
		re := regexp.MustCompile(pattern)
		match := re.FindString(syslog)
		if match != "" {
			timestamp = match
			break
		}
	}

	if timestamp == "" {
		return "", "", "", errors.New("timestamp not found")
	}

	// Extract remaining log details after timestamp
	remainingLog := strings.TrimSpace(strings.TrimPrefix(syslog, timestamp))

	// Split log to extract hostname (first word after timestamp)
	parts := strings.Fields(remainingLog)
	if len(parts) < 2 {
		return timestamp, "", "", errors.New("hostname not found")
	}
	hostname := parts[0]

	// Extract group_id at the end of the log
	groupIDRegex := regexp.MustCompile(`\[group_id=(\d+)\]`)
	groupIDMatch := groupIDRegex.FindStringSubmatch(syslog)
	if len(groupIDMatch) < 2 {
		return timestamp, hostname, "", errors.New("group_id not found")
	}

	groupID := groupIDMatch[1]
	return timestamp, hostname, groupID, nil
}

// GenerateRandomAPIKey generates a secure random API key of a given length (in bytes)
func GenerateRandomAPIKey(length int) (string, error) {
	// Create a slice of random bytes
	bytes := make([]byte, length)
	_, err := rand.Read(bytes)
	if err != nil {
		return "", err
	}

	// Convert the bytes to a hexadecimal string
	apiKey := hex.EncodeToString(bytes)
	return apiKey, nil
}

// GenerateRandomLicenseKey generates a random license key in a format like XXXX-XXXX-XXXX-XXXX
func GenerateRandomLicenseKey(groups, length int) (string, error) {
	var sb strings.Builder

	// Generate groups of random bytes
	for i := 0; i < groups; i++ {
		// Generate random bytes for each group
		bytes := make([]byte, length)
		_, err := rand.Read(bytes)
		if err != nil {
			return "", err
		}

		// Convert bytes to a hexadecimal string and append to the builder
		group := hex.EncodeToString(bytes)
		sb.WriteString(group[:length]) // Only take the first 'length' characters

		// Add hyphen unless it's the last group
		if i < groups-1 {
			sb.WriteString("-")
		}
	}

	return sb.String(), nil
}

// Capitalize capitalizes the first letter and lowercases the rest
func Capitalize(s string) string {
	if s == "" {
		return ""
	}
	runes := []rune(s)
	runes[0] = unicode.ToUpper(runes[0])
	return string(runes[0]) + strings.ToLower(string(runes[1:]))
}

var (
	lowerChars   = "abcdefghijklmnopqrstuvwxyz"
	upperChars   = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
	digitChars   = "0123456789"
	specialChars = "!@#$%^&*()-_+="
	allChars     = lowerChars + upperChars + digitChars + specialChars
)

func randomCharFromSet(charset string) (byte, error) {
	index, err := rand.Int(rand.Reader, big.NewInt(int64(len(charset))))
	if err != nil {
		return 0, err
	}
	return charset[index.Int64()], nil
}

func GenerateSecurePassword(length int) (string, error) {
	if length < 8 {
		return "", fmt.Errorf("password length must be at least 8")
	}

	// Pre-fill with one character from each required class
	required := []string{lowerChars, upperChars, digitChars, specialChars}
	password := make([]byte, length)

	for i, set := range required {
		c, err := randomCharFromSet(set)
		if err != nil {
			return "", err
		}
		password[i] = c
	}

	// Fill the rest of the password with random characters from allChars
	for i := len(required); i < length; i++ {
		c, err := randomCharFromSet(allChars)
		if err != nil {
			return "", err
		}
		password[i] = c
	}

	// Shuffle the password to mix up required characters
	shuffled, err := shuffle(password)
	if err != nil {
		return "", err
	}

	return string(shuffled), nil
}

// Fisher–Yates shuffle
func shuffle(data []byte) ([]byte, error) {
	shuffled := make([]byte, len(data))
	copy(shuffled, data)

	for i := len(shuffled) - 1; i > 0; i-- {
		jBig, err := rand.Int(rand.Reader, big.NewInt(int64(i+1)))
		if err != nil {
			return nil, err
		}
		j := int(jBig.Int64())
		shuffled[i], shuffled[j] = shuffled[j], shuffled[i]
	}
	return shuffled, nil
}

// sanitize input to only inclide numbers, letters and underscores
func SanitizeInput(input string) string {
	// Replace all characters not matching the allowed pattern with an empty string
	re := regexp.MustCompile(`[^a-zA-Z0-9_]`)
	return re.ReplaceAllString(input, "")
}

// parseDownloadParams extracts and validates download parameters
func ParseDownloadParams(c *gin.Context) (int64, string, string, string, string, error) {
	architecture := c.Query("arch")
	os := c.Query("os")
	groupID := c.Query("group_id")
	distro := c.Query("distro")
	version := c.Query("version")

	// Validate inputs
	if !IsValidInput(architecture) || !IsValidInput(os) {
		return 0, "", "", "", "", fmt.Errorf("invalid input parameters")
	}

	if os == "linux" && !IsValidInput(distro) {
		return 0, "", "", "", "", fmt.Errorf("distro required for Linux")
	}

	groupIDInt, err := strconv.ParseInt(groupID, 10, 64)
	if err != nil {
		return 0, "", "", "", "", fmt.Errorf("invalid group ID")
	}

	return groupIDInt, os, architecture, distro, version, nil
}

// validateOSAndArchitecture validates OS, architecture, and distro inputs
func ValidateOSAndArchitecture(os, arch string, distro *string) (string, error) {
	osOptions := map[string]string{
		"macos":   "darwin",
		"windows": "windows",
		"linux":   "linux",
	}

	archOptions := map[string]string{
		"amd64": "amd64",
		"arm64": "arm64",
	}

	distroOptions := map[string]string{
		"deb": "deb",
		"rpm": "rpm",
	}

	osFlag, exists := osOptions[os]
	if !exists {
		return "", fmt.Errorf("invalid OS type: %s", os)
	}

	_, exists = archOptions[arch]
	if !exists {
		return "", fmt.Errorf("invalid architecture type: %s", arch)
	}

	if os == "linux" {
		if distro == nil || *distro == "" {
			return "", fmt.Errorf("distro is required for Linux")
		}
		_, exists = distroOptions[*distro]
		if !exists {
			return "", fmt.Errorf("invalid distro type: %s", *distro)
		}
	}

	return osFlag, nil
}
