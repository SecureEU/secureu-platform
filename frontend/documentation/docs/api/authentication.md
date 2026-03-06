---
sidebar_position: 2
---

# Authentication

Current authentication methods for the SECUR-EU API.

## Current Status

:::info Development Mode
The API currently operates without authentication for development purposes. Authentication will be added in a future release.
:::

## Planned Authentication

### API Keys

Future implementation will support API key authentication:

```bash
curl -H "X-API-Key: your-api-key" http://localhost:3001/scans
```

### JWT Tokens

For user-based authentication:

```bash
# Login
curl -X POST http://localhost:3001/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username": "user", "password": "pass"}'

# Response
{
  "token": "eyJhbGciOiJIUzI1NiIs...",
  "expiresIn": 3600
}

# Use token
curl -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIs..." \
  http://localhost:3001/scans
```

## Security Considerations

### Network Security

Since authentication is not yet implemented:

1. **Run locally only** - Don't expose to public internet
2. **Use firewall** - Restrict access to trusted IPs
3. **VPN access** - Use VPN for remote access
4. **Reverse proxy** - Add authentication at proxy level

### Reverse Proxy Authentication

Example Nginx configuration with basic auth:

```nginx
server {
    listen 443 ssl;
    server_name api.example.com;

    auth_basic "SECUR-EU API";
    auth_basic_user_file /etc/nginx/.htpasswd;

    location / {
        proxy_pass http://localhost:3001;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
}
```

## CORS Configuration

The API allows requests from configured origins:

```go
AllowOrigins: []string{
    "http://localhost:3000",
    "http://localhost:3001",
}
```

To add additional origins, modify the CORS configuration in the backend.

## Best Practices

### Development

- Use localhost only
- Don't commit sensitive data
- Use environment variables for secrets

### Production (Future)

- Enable authentication
- Use HTTPS
- Implement rate limiting
- Audit API access
- Rotate credentials regularly

## Related

- [API Overview](/api/overview)
- [Endpoint Reference](/api/endpoints)
