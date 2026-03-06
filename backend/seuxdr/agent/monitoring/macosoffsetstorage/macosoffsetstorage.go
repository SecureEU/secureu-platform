//go:build darwin
// +build darwin

package macosoffsetstorage

import (
	"SEUXDR/agent/config"
	"SEUXDR/agent/db"
	"SEUXDR/agent/db/models"
	"SEUXDR/agent/db/scopes"
	"SEUXDR/agent/helpers"
	"SEUXDR/agent/logging"

	"github.com/sirupsen/logrus"
	"gorm.io/gorm"
)

type OffsetStorage struct {
	journalCtlRepo db.MacosLogsRepository
	JournalData    *models.MacOSLog // Metadata struct
	logger         logging.EULogger
}

func NewOffsetStorage(channel string, query config.Query, macOSLogRepo db.MacosLogsRepository, format string, logger logging.EULogger) (*OffsetStorage, error) {
	var offsetStorage OffsetStorage

	offsetStorage.logger = logger

	scopeList := []func(db *gorm.DB) *gorm.DB{}
	predicate, err := helpers.ConvertQueryToPredicate(query)
	if err != nil {
		offsetStorage.logger.LogWithContext(logrus.ErrorLevel, "Failed to convert predicate for "+channel, logrus.Fields{"error": err.Error()})
	} else {
		scopeList = append(scopeList, scopes.ByPredicate(predicate))
	}

	offsetStorage.journalCtlRepo = macOSLogRepo
	syslogFilesData, err := offsetStorage.journalCtlRepo.Find(scopeList...)
	if err != nil && err != gorm.ErrRecordNotFound {
		offsetStorage.logger.LogWithContext(logrus.ErrorLevel, "Failed to check for log file data "+channel, logrus.Fields{"error": err.Error()})
		return &offsetStorage, err
	}

	if len(syslogFilesData) > 0 {
		offsetStorage.JournalData = &syslogFilesData[0]
	} else {

		offsetStorage.JournalData = &models.MacOSLog{Type: channel, Predicate: &predicate}
		if len(predicate) > 0 {
			offsetStorage.JournalData.Predicate = &predicate
		}
		if err := macOSLogRepo.Create(offsetStorage.JournalData); err != nil {
			offsetStorage.logger.LogWithContext(logrus.ErrorLevel, "Failed to store metadata for "+channel, logrus.Fields{"error": err.Error()})
			return &offsetStorage, err
		}
	}

	return &offsetStorage, nil
}

// readOffset reads the saved offset from the binary file
func (offStrg *OffsetStorage) ReadOffset() (string, bool, error) {
	var offset string
	var exists bool

	syslogFilesData, err := offStrg.journalCtlRepo.Find(scopes.ByType(offStrg.JournalData.Type), scopes.ByPredicate(*offStrg.JournalData.Predicate))
	if err != nil && err != gorm.ErrRecordNotFound {
		offStrg.logger.LogWithContext(logrus.ErrorLevel, "Failed to check for log file data for"+offStrg.JournalData.Type, logrus.Fields{"error": err.Error()})
		return offset, exists, err
	}

	if len(syslogFilesData) > 0 {
		exists = true
		if syslogFilesData[0].LogShowOffset != nil {
			offset = *syslogFilesData[0].LogShowOffset
		}
	}

	return offset, exists, nil
}

// saveOffset writes the current offset to the binary file
func (offStrg *OffsetStorage) SaveOffset(offset string) error {
	offStrg.JournalData.LogShowOffset = &offset
	return offStrg.journalCtlRepo.Save(offStrg.JournalData)
}

// SaveMetadataToFile stores the file metadata in binary format
func (offsStrg *OffsetStorage) Save() error {

	return offsStrg.journalCtlRepo.Save(offsStrg.JournalData)
}
