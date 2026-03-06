package encryptionservice_test

import (
	"SEUXDR/manager/api/encryptionservice"
	"SEUXDR/manager/helpers"
	"SEUXDR/manager/mocks"
	"SEUXDR/manager/utils"
	"testing"

	"github.com/stretchr/testify/assert"
	"go.uber.org/mock/gomock"
)

func TestEncryptionService(t *testing.T) {

	mockCtrl := gomock.NewController(t)
	defer mockCtrl.Finish()
	mockLogger := mocks.NewMockEULogger(mockCtrl)
	prvKey := "prv_key.pem"
	publicKey := "pub_key.pem"

	err := utils.GeneratePrivateAndPublicKeyCertificate(prvKey, publicKey)
	assert.Nil(t, err)
	defer helpers.DeleteFiles([]string{prvKey, publicKey})

	encrSvc, err := encryptionservice.NewEncryptionService(prvKey, publicKey, mockLogger)

	assert.Nil(t, err)
	assert.NotNil(t, encrSvc)

	aesKey, err := utils.GenerateAESKey()
	assert.Nil(t, err)

	ciphertxt, err := encrSvc.EncryptAESKeyWithKEK(aesKey)
	assert.Nil(t, err)

	plaintxt, err := encrSvc.DecryptAESKeyWithKEK(ciphertxt)
	assert.Nil(t, err)

	assert.Equal(t, aesKey, plaintxt)

}
