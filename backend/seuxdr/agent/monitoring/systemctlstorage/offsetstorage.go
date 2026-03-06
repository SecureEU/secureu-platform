package systemctlstorage

import (
	"SEUXDR/agent/db"
	"SEUXDR/agent/db/models"
	"SEUXDR/agent/db/scopes"
	"SEUXDR/agent/logging"

	"github.com/sirupsen/logrus"
	"gorm.io/gorm"
)

type OffsetStorage struct {
	journalCtlRepo db.JournalctlRepository
	JournalData    *models.JournalctlLog // Metadata struct
	logger         logging.EULogger
}

func NewOffsetStorage(channel string, query string, journalCtlRepo db.JournalctlRepository, format string, logger logging.EULogger) (*OffsetStorage, error) {
	var offsetStorage OffsetStorage

	offsetStorage.logger = logger

	scopeList := []func(db *gorm.DB) *gorm.DB{}
	scopeList = append(scopeList, scopes.ByQuery(query))

	offsetStorage.journalCtlRepo = journalCtlRepo
	syslogFilesData, err := offsetStorage.journalCtlRepo.Find(scopeList...)
	if err != nil && err != gorm.ErrRecordNotFound {
		offsetStorage.logger.LogWithContext(logrus.ErrorLevel, "Failed to check for log file data "+channel, logrus.Fields{"error": err.Error()})
		return &offsetStorage, err
	}

	if len(syslogFilesData) > 0 {
		offsetStorage.JournalData = &syslogFilesData[0]
	} else {
		offsetStorage.JournalData = &models.JournalctlLog{Type: channel, Query: &query}
		if len(query) > 0 {
			offsetStorage.JournalData.Query = &query
		}
		if err := journalCtlRepo.Create(offsetStorage.JournalData); err != nil {
			offsetStorage.logger.LogWithContext(logrus.ErrorLevel, "Failed to store metadata for log file "+channel, logrus.Fields{"error": err.Error()})
			return &offsetStorage, err
		}
	}

	return &offsetStorage, nil
}

// readOffset reads the saved offset from the binary file
func (offStrg *OffsetStorage) ReadOffset() (string, error) {
	var offset string

	syslogFilesData, err := offStrg.journalCtlRepo.Find(scopes.ByType(offStrg.JournalData.Type))
	if err != nil && err != gorm.ErrRecordNotFound {
		offStrg.logger.LogWithContext(logrus.ErrorLevel, "Failed to check for log file data for"+offStrg.JournalData.Type, logrus.Fields{"error": err.Error()})
		return offset, err
	}

	if len(syslogFilesData) > 0 {
		offset = syslogFilesData[0].JournalctlOffset
	}

	return offset, nil
}

// saveOffset writes the current offset to the binary file
func (offStrg *OffsetStorage) SaveOffset(offset string) error {

	offStrg.JournalData.JournalctlOffset = offset

	return offStrg.journalCtlRepo.Save(offStrg.JournalData)
}

// SaveMetadataToFile stores the file metadata in binary format
func (offsStrg *OffsetStorage) Save() error {

	return offsStrg.journalCtlRepo.Save(offsStrg.JournalData)
}
