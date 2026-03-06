//go:build linux && amd64
// +build linux,amd64

package filemonitor

import (
	"crypto/md5"
	"io"
	"os"
	"syscall"
)

// GetFileMetadata retrieves the file's metadata (including inode, hash, size, mod time)
func (offsStrg *OffsetStorage) GetFileMetadata() (FileMetadata, error) {
	fileInfo, err := os.Stat(offsStrg.LogFilePath)
	if err != nil {
		return FileMetadata{}, err
	}

	stat := fileInfo.Sys().(*syscall.Stat_t)

	// Compute MD5 hash of the file
	file, err := os.Open(offsStrg.LogFilePath)
	if err != nil {
		return FileMetadata{}, err
	}
	defer file.Close()

	hash := md5.New()
	if _, err := io.Copy(hash, file); err != nil {
		return FileMetadata{}, err
	}

	return FileMetadata{
		Inode:   stat.Ino,
		Hash:    md5.Sum(nil), // Compute and store the hash
		Size:    fileInfo.Size(),
		ModTime: fileInfo.ModTime().Unix(),
		Path:    offsStrg.LogFilePath,
		Offset:  offsStrg.FileMetaData.OffsetBookmark,
	}, nil
}
