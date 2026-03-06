//go:build windows
// +build windows

package channelservice

import (
	"sync"
	"time"

	"github.com/elastic/beats/v7/libbeat/logp"
	"github.com/elastic/beats/v7/winlogbeat/sys/winevent"
	"github.com/elastic/beats/v7/winlogbeat/sys/wineventlog"
)

// winMetaCache retrieves and caches WinMeta tables by provider name.
// It is a cut down version of the PublisherMetadataStore caching in wineventlog.Renderer.
type winMetaCache struct {
	ttl    time.Duration
	logger *logp.Logger

	mu    sync.RWMutex
	cache map[string]winMetaCacheEntry
}

type winMetaCacheEntry struct {
	expire time.Time
	*winevent.WinMeta
}

func newWinMetaCache(ttl time.Duration) winMetaCache {
	return winMetaCache{cache: make(map[string]winMetaCacheEntry), ttl: ttl, logger: logp.L()}
}

func (c *winMetaCache) winMeta(provider string) *winevent.WinMeta {
	c.mu.RLock()
	e, ok := c.cache[provider]
	c.mu.RUnlock()
	if ok && time.Until(e.expire) > 0 {
		return e.WinMeta
	}

	// Upgrade lock.
	defer c.mu.Unlock()
	c.mu.Lock()

	// Did the cache get updated during lock upgrade?
	// No need to check expiry here since we must have a new entry
	// if there is an entry at all.
	if e, ok := c.cache[provider]; ok {
		return e.WinMeta
	}

	s, err := wineventlog.NewPublisherMetadataStore(wineventlog.NilHandle, provider, c.logger)
	if err != nil {
		// Return an empty store on error (can happen in cases where the
		// log was forwarded and the provider doesn't exist on collector).
		s = wineventlog.NewEmptyPublisherMetadataStore(provider, c.logger)
		// logp.Warn("failed to load publisher metadata for %v (returning an empty metadata store): %v", provider, err)
	}
	s.Close()
	c.cache[provider] = winMetaCacheEntry{expire: time.Now().UTC().Add(c.ttl), WinMeta: &s.WinMeta}
	return &s.WinMeta
}
