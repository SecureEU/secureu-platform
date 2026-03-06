//go:build linux
// +build linux

package filemonitor

import (
	"bytes"
	"crypto/md5"
	"fmt"
	"io"
	"os"
	"os/exec"
	"strconv"
	"strings"
)

// GetFileMetadata retrieves the file's metadata (including inode, hash, size, mod time)
func (offsStrg *OffsetStorage) GetFileMetadata() (FileMetadata, error) {
	var metadata FileMetadata
	fileInfo, err := os.Stat(offsStrg.LogFilePath)
	if err != nil {
		return metadata, err
	}

	ind, err := offsStrg.getInodeUsingCommand(offsStrg.LogFilePath)
	if err != nil {
		return metadata, err
	}
	// Convert string to int64
	inode, err := strconv.ParseUint(ind, 10, 64)
	if err != nil {
		return metadata, fmt.Errorf("error converting string to int64: %v", err)
	}

	// Compute MD5 hash of the file
	file, err := os.Open(offsStrg.LogFilePath)
	if err != nil {
		return metadata, err
	}
	defer file.Close()

	hash := md5.New()
	if _, err := io.Copy(hash, file); err != nil {
		return metadata, err
	}
	metadata = FileMetadata{
		Inode:   inode,
		Hash:    md5.Sum(nil), // Compute and store the hash
		Size:    fileInfo.Size(),
		ModTime: fileInfo.ModTime().Unix(),
		Path:    offsStrg.LogFilePath,
		Offset:  offsStrg.FileMetaData.OffsetBookmark,
	}

	return metadata, nil
}

func (offsStrg *OffsetStorage) getInodeUsingCommand(filePath string) (string, error) {
	cmd := exec.Command("ls", "-i", filePath)
	var out bytes.Buffer
	cmd.Stdout = &out
	err := cmd.Run()
	if err != nil {
		return "", fmt.Errorf("failed to execute command: %v", err)
	}
	output := out.String()
	parts := strings.Fields(output)
	if len(parts) < 2 {
		return "", fmt.Errorf("unexpected output format")
	}
	return parts[0], nil
}
