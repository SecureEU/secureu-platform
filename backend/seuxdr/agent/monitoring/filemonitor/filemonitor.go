package filemonitor

import (
	"SEUXDR/agent/db"
	"SEUXDR/agent/db/models"
	"SEUXDR/agent/db/scopes"
	"SEUXDR/agent/logging"
	"crypto/md5"
	"fmt"
	"os"

	"github.com/sirupsen/logrus"
	"gorm.io/gorm"
)

// FileMetadata represents the metadata retrieved for each monitored file
type FileMetadata struct {
	Inode   uint64
	Hash    [md5.Size]byte // 16-byte MD5 hash
	Size    int64
	ModTime int64
	Path    string
	Offset  int64
}

type OffsetStorage struct {
	logFileRepository db.LogFileRepository
	FileMetaData      *models.LogFile // Metadata struct

	Directory    string // directory where offsets are stored
	LogFilePath  string // filepath of the file being monitored
	MetadataPath string // filepath where the log file's metadata are stored
	OffsetPath   string // filepath where the offset of the log file is stored

	logger logging.EULogger
}

func NewOffsetStorage(filePath string, query string, logFileRepo db.LogFileRepository, format string, logger logging.EULogger) (*OffsetStorage, error) {
	var offsetStorage OffsetStorage

	offsetStorage.logFileRepository = logFileRepo
	offsetStorage.LogFilePath = filePath
	offsetStorage.logger = logger

	scopeList := []func(db *gorm.DB) *gorm.DB{}
	scopeList = append(scopeList, scopes.ByPath(filePath))
	scopeList = append(scopeList, scopes.ByQuery(query))

	syslogFilesData, err := offsetStorage.logFileRepository.Find(scopeList...)
	if err != nil && err != gorm.ErrRecordNotFound {
		offsetStorage.logger.LogWithContext(logrus.ErrorLevel, "Failed to check for log file data"+filePath, logrus.Fields{"error": err.Error()})
		return &offsetStorage, err
	}

	if len(syslogFilesData) > 0 {
		offsetStorage.FileMetaData = &syslogFilesData[0]
	} else {
		offsetStorage.FileMetaData = &models.LogFile{Path: filePath, Type: format}
		if len(query) > 0 {
			offsetStorage.FileMetaData.Query = query
		}
		if err := logFileRepo.Create(offsetStorage.FileMetaData); err != nil {
			offsetStorage.logger.LogWithContext(logrus.ErrorLevel, "Failed to store metadata for log file "+filePath, logrus.Fields{"error": err.Error()})
			return &offsetStorage, err
		}
	}

	return &offsetStorage, nil
}

// readOffset reads the saved offset from the binary file
func (offStrg *OffsetStorage) ReadOffset() (int64, error) {
	var offset int64

	syslogFilesData, err := offStrg.logFileRepository.Find(scopes.ByPath(offStrg.LogFilePath))
	if err != nil && err != gorm.ErrRecordNotFound {
		offStrg.logger.LogWithContext(logrus.ErrorLevel, "Failed to check for log file data for"+offStrg.LogFilePath, logrus.Fields{"error": err.Error()})
		return offset, err
	}

	if len(syslogFilesData) > 0 {
		offset = syslogFilesData[0].OffsetBookmark
	}

	return offset, nil
}

// saveOffset writes the current offset to the binary file
func (offStrg *OffsetStorage) SaveOffset(offset int64) error {

	offStrg.FileMetaData.OffsetBookmark = offset

	return offStrg.logFileRepository.Save(offStrg.FileMetaData)
}

func (offStrg *OffsetStorage) LogIsTruncated(file *os.File, lastOffset int64) (bool, error) {

	// Check if the file has been truncated (size smaller than the last offset)
	fileInfo, err := file.Stat()
	if err != nil {
		return false, fmt.Errorf("failed to stat log file: %v", err)
	}
	return fileInfo.Size() < lastOffset, nil

}

// SaveMetadataToFile stores the file metadata in binary format
func (offsStrg *OffsetStorage) SaveMetadata(newMetaData FileMetadata) error {
	hash := md5.Sum(nil)
	offsStrg.FileMetaData.Inode = newMetaData.Inode
	offsStrg.FileMetaData.Hash = hash[:]
	offsStrg.FileMetaData.Size = newMetaData.Size
	offsStrg.FileMetaData.ModTime = newMetaData.ModTime
	offsStrg.FileMetaData.Path = newMetaData.Path
	offsStrg.FileMetaData.OffsetBookmark = newMetaData.Offset

	return offsStrg.logFileRepository.Save(offsStrg.FileMetaData)
}
