package models

// LogFile represents the structure of the log_file table in the database.
type LogFile struct {
	ID             uint   `gorm:"primaryKey;autoIncrement" json:"id"`
	Type           string `gorm:"type:varchar(256);not null" json:"type"`
	Path           string `gorm:"type:varchar(256);not null" json:"name"`
	Query          string `gorm:"type:varchar(1000)" json:"query"`
	OffsetBookmark int64  `gorm:"column:offset_bookmark;not null" json:"bookmark"`
	Inode          uint64 `gorm:"column:inode"`
	Hash           []byte `gorm:"column:hash;type:binary(16)"`
	Size           int64  `gorm:"column:size"`
	ModTime        int64  `gorm:"column:mod_time"`
}

func (lf *LogFile) IsEmpty() bool {
	return lf.ID == 0 &&
		lf.Path == "" &&
		lf.Query == "" &&
		lf.OffsetBookmark == 0 &&
		lf.Inode == 0 &&
		lf.Hash == nil &&
		lf.Size == 0 &&
		lf.ModTime == 0
}
