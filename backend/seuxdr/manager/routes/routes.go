package routes

import (
	conf "SEUXDR/manager/config"
	"SEUXDR/manager/handlers"
	"SEUXDR/manager/middlewares"
	"net/http"

	"github.com/gin-contrib/cors"
	"github.com/gin-gonic/gin"
)

const contentLengthHeader = "Content-Length"

func InitializemTLSRoutes(h *handlers.Handlers, m *middlewares.Middleware) *gin.Engine {
	router := gin.New()
	router.Use(gin.Recovery())
	router.Use(cors.New(cors.Config{
		AllowOriginFunc: func(origin string) bool {
			return true
		},
		AllowMethods:     []string{"GET", "POST", "PUT", "DELETE", "OPTIONS"},
		AllowHeaders:     []string{"Origin", contentLengthHeader, "Content-Type", "Authorization"},
		ExposeHeaders:    []string{contentLengthHeader},
		AllowCredentials: true,
		AllowWildcard:    true,
		// AllowWebSockets:  true,
		// AllowFiles:       true,
	}))

	// server.SetCors(router)
	mainRouter := router.Group("/api")
	mainRouter.Use(m.CustomLogger("logs/registrations.log", "registration"))
	mainRouter.Use(m.Limiter)

	mainRouter.POST("/register", h.Register)
	mainRouter.GET("/status", func(c *gin.Context) {
		c.JSON(200, gin.H{
			"message": "Ok",
		})
	})

	return router

}

func InitializeTLSRoutes(h *handlers.Handlers, m *middlewares.Middleware, cfg conf.Configuration) *gin.Engine {
	router := gin.New()
	router.Use(gin.Recovery())
	router.Use(cors.New(cors.Config{
		AllowOriginFunc: func(origin string) bool {
			return true // Allow all origins since authentication is disabled
		},
		AllowMethods:     []string{"GET", "POST", "PUT", "DELETE", "OPTIONS"},
		AllowHeaders:     []string{"Origin", contentLengthHeader, "Content-Type", "Authorization"},
		ExposeHeaders:    []string{contentLengthHeader},
		AllowCredentials: true,
		AllowWildcard:    true,
		AllowWebSockets:  true,

		// AllowFiles:       true,
	}))

	mainRouter := router.Group("/api")
	mainRouter.Use(m.CustomLogger("logs/server.log", "SEUXDR"))
	mainRouter.Use(m.Authenticate)

	mainRouter.POST("/create/agent", h.GenerateAgentClientWithVersion)
	mainRouter.POST("/view/alerts", h.ViewAlerts)
	mainRouter.GET("/download/agent", h.DownloadAgentWithVersion)
	mainRouter.GET("/download/raw/:agentUUID", h.DownloadExecutableByURL)
	// mainRouter.GET("/getExecutables/:group_id", h.GetExecutables)
	mainRouter.GET("/log", h.Log)
	mainRouter.GET("/status", func(c *gin.Context) {
		c.JSON(200, gin.H{
			"message": "Ok",
		})
	})
	mainRouter.POST("/orgs", h.ManageOrgs)
	mainRouter.POST("/create/org", h.CreateOrg)
	mainRouter.POST("/create/group", h.CreateGroup)
	mainRouter.POST("/view/agents", h.ViewAgents)
	mainRouter.POST("/agent/activate", h.ActivateAgent)
	mainRouter.POST("/agent/deactivate", h.DeactivateAgent)

	mainRouter.POST("/users", h.ManageUsers)
	mainRouter.POST("/create/user", h.CreateUser)
	mainRouter.POST("/change-password", h.ChangePassword)

	mainRouter.POST("/keepalive", h.KeepAliveWithUpdateCheck)
	// h.KeepAliveHandler()

	mainRouter.POST("/login", h.Login)
	mainRouter.POST("/register", h.UserRegister)
	mainRouter.POST("/logout", h.LogOut)

	mainRouter.PUT("/update/user/:user_id", h.UpdateUser)

	// In your Go app
	if !cfg.USE_SYSTEM_CA {
		mainRouter.GET("/certs/server-ca.crt", h.GetTLSCerts)
	}

	mainRouter.OPTIONS("/view-alerts", func(c *gin.Context) {
		c.Header("Access-Control-Allow-Origin", "*")
		c.Header("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS")
		c.Header("Access-Control-Allow-Headers", "Origin, Content-Type, Authorization")
		c.Status(http.StatusOK)
	})

	return router

}
