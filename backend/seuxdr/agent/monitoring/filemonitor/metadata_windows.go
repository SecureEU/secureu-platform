//go:build windows
// +build windows

package filemonitor

import (
	"crypto/md5"
	"io"
	"os"
)

// GetFileMetadata retrieves the file's metadata (including inode, hash, size, mod time)
func (offsStrg *OffsetStorage) GetFileMetadata() (FileMetadata, error) {
	var metadata FileMetadata

	fileInfo, err := os.Stat(offsStrg.LogFilePath)
	if err != nil {
		return metadata, err
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

	return FileMetadata{
		Inode:   0,
		Hash:    md5.Sum(nil), // Compute and store the hash
		Size:    fileInfo.Size(),
		ModTime: fileInfo.ModTime().Unix(),
		Path:    offsStrg.LogFilePath,
	}, nil
}
