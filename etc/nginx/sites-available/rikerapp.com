server {

    # http://serverfault.com/a/337893
    listen      80;
    server_name rikerapp.com;

    location / {
        return 301 https://www.$server_name$request_uri;
    }
}
