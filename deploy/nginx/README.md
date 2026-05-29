# deploy/nginx

Nginx reverse-proxy template for `bl-center` and `auth-center`.

- `sybase.conf`: route `/api/` and `/actuator/` to `bl-center:8080`, `/auth/` to `auth-center:8081`.
- `sybase-with-web.conf`: serve frontend static files and keep both `/api/v1/*` and legacy `/api/*` backend routes in the same server block.
- `sybase-with-web-local.conf`: same as above, but ready for local terminal checks against `127.0.0.1` and `/tmp/soft/blxc/web/bl`.
