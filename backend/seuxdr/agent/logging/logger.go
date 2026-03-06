//go:generate: mockgen -destination=manager/mocks/mock_logging.go -package=mocks -source=manager/logging/logging.go

package logging

import (
	"runtime"
	"strconv"
	"time"

	"github.com/sirupsen/logrus"
	"gopkg.in/natefinch/lumberjack.v2"
)

// LogContext holds context for logs, such as the service name and request ID.
type LogContext struct {
	RequestID string
	Service   string
}

type EULogger interface {
	SetRequestID(requestID string)
	LogWithContext(level logrus.Level, msg string, fields logrus.Fields)
}

type euLogger struct {
	Context LogContext
	Logger  *logrus.Logger
}

// NewLogger initializes a Logrus logger with JSON formatting
func NewEULogger(service string, filename string) EULogger {
	logger := logrus.New()

	// Set JSON formatter for structured logging
	logger.SetFormatter(&logrus.JSONFormatter{
		TimestampFormat: time.RFC3339, // Set timestamp format
	})
	logger.SetOutput(&lumberjack.Logger{
		Filename:   filename,
		MaxSize:    10, // Megabytes
		MaxBackups: 3,
		MaxAge:     28,   // Days
		Compress:   true, // Compress rotated files
	}) // Output to console or a file

	logger.SetLevel(logrus.InfoLevel) // Set default log level

	// add only service name for now
	ctx := LogContext{Service: service}

	return &euLogger{Logger: logger, Context: ctx}
}

func (eulogger *euLogger) SetRequestID(requestID string) {
	eulogger.Context.RequestID = requestID
}

// LogWithContext logs a structured message with the provided context and log level
func (eulogger *euLogger) LogWithContext(level logrus.Level, msg string, fields logrus.Fields) {
	// Add default fields (timestamp, caller, etc.)
	defaultFields := logrus.Fields{
		"timestamp": time.Now().UTC().Format(time.RFC3339),
		"caller":    getCaller(),
		"service":   eulogger.Context.Service,
	}

	// Merge default fields with provided fields
	for k, v := range fields {
		defaultFields[k] = v
	}

	// Log based on the level
	switch level {
	case logrus.DebugLevel:
		eulogger.Logger.WithFields(defaultFields).Debug(msg)
	case logrus.InfoLevel:
		eulogger.Logger.WithFields(defaultFields).Info(msg)
	case logrus.WarnLevel:
		eulogger.Logger.WithFields(defaultFields).Warn(msg)
	case logrus.ErrorLevel:
		eulogger.Logger.WithFields(defaultFields).Error(msg)
	case logrus.FatalLevel:
		eulogger.Logger.WithFields(defaultFields).Fatal(msg)
	default:
		eulogger.Logger.WithFields(defaultFields).Info(msg)
	}
}

// getCaller retrieves the file and line number where the log was called
func getCaller() string {
	_, file, line, ok := runtime.Caller(2)
	if !ok {
		return "unknown"
	}
	s := strconv.Itoa(line)
	return file + ":" + s
}
