package middlewares

import (
	"net/http"
	"strings"
	"time"

	"github.com/gin-gonic/gin"
	"github.com/patrickmn/go-cache"
	"golang.org/x/time/rate"
)

// Define routes that need rate limiting
var needLimiterRoutes = map[string]bool{
	"/api/register": true,
}

// Check if the route needs rate limiting
func isLimiterNeeded(path string) bool {
	_, exists := needLimiterRoutes[path]
	return exists
}

// Create a cache with default expiration time of 10 minutes, and cleanup interval of 1 minute
var limiterCache = cache.New(10*time.Minute, 1*time.Minute)

// Retrieve or create a rate limiter for the given IP
func getClientLimiter(ip string) *rate.Limiter {
	limiter, found := limiterCache.Get(ip)
	if !found {
		// Create a new limiter: 1 request per second, burst of 0
		newLimiter := rate.NewLimiter(1, 1)
		limiterCache.Set(ip, newLimiter, cache.DefaultExpiration)
		return newLimiter
	}
	return limiter.(*rate.Limiter)
}

func (m *Middleware) Limiter(c *gin.Context) {
	path := strings.ToLower(c.Request.URL.Path)
	needToLimit := isLimiterNeeded(path)

	if needToLimit {
		ip := c.ClientIP()
		limiter := getClientLimiter(ip)

		if !limiter.Allow() {
			c.Header("Retry-After", "5") // Retry after 60 seconds
			c.JSON(http.StatusTooManyRequests, gin.H{"error": "Too many requests"})
			c.Abort()
			return
		}
	}

	c.Next()
}
