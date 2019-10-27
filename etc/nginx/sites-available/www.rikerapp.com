server {
    set $letsencrypt_webroot '/home/fprest/documents/letsencrypt_webroot';

    # http://serverfault.com/a/337893
    listen      80;
    server_name www.rikerapp.com;

    location /.well-known {
        root $letsencrypt_webroot;
        allow all;
    }

    location / {
        return 301 https://$server_name$request_uri;
    }
}

#
# HTTPS server
#
server {
    set $content_root        '/home/fprest/documents/riker-web';
    set $maintenance_on_file '/home/fprest/documents/maintenance-on';
    set $letsencrypt_webroot '/home/fprest/documents/letsencrypt_webroot';

    listen      443;
    server_name www.rikerapp.com;

    gzip       on;
    gzip_types *;

    ssl                       on;
    ssl_certificate           /etc/letsencrypt/live/www.rikerapp.com/fullchain.pem;
    ssl_certificate_key       /etc/letsencrypt/live/www.rikerapp.com/privkey.pem;

    # This works on mobile web and iOS app, but gets C rating from
    # https://www.ssllabs.com/ssltest/analyze.html?d=www.rikerapp.com
    ssl_session_timeout       5m;
    ssl_protocols             SSLv3 TLSv1 TLSv1.1 TLSv1.2;
    ssl_ciphers               "HIGH:!aNULL:!MD5 or HIGH:!aNULL:!MD5:!3DES";
    ssl_prefer_server_ciphers on;

    # This works on desktop browser, but doesn't work on mobile web and iOS app; gets A+ rating from
    # https://www.ssllabs.com/ssltest/analyze.html?d=www.rikerapp.com
    # ssl_protocols TLSv1 TLSv1.1 TLSv1.2;
    # ssl_prefer_server_ciphers on;
    # ssl_dhparam /etc/ssl/certs/dhparam.pem;
    # ssl_ciphers 'ECDHE-RSA-AES128-GCM-SHA256:ECDHE-ECDSA-AES128-GCM-SHA256:ECDHE-RSA-AES256-GCM-SHA384:ECDHE-ECDSA-AES256-GCM-SHA384:DHE-RSA-AES128-GCM-SHA256:DHE-DSS-AES128-GCM-SHA256:kEDH+AESGCM:ECDHE-RSA-AES128-SHA256:ECDHE-ECDSA-AES128-SHA256:ECDHE-RSA-AES128-SHA:ECDHE-ECDSA-AES128-SHA:ECDHE-RSA-AES256-SHA384:ECDHE-ECDSA-AES256-SHA384:ECDHE-RSA-AES256-SHA:ECDHE-ECDSA-AES256-SHA:DHE-RSA-AES128-SHA256:DHE-RSA-AES128-SHA:DHE-DSS-AES128-SHA256:DHE-RSA-AES256-SHA256:DHE-DSS-AES256-SHA:DHE-RSA-AES256-SHA:AES128-GCM-SHA256:AES256-GCM-SHA384:AES128-SHA256:AES256-SHA256:AES128-SHA:AES256-SHA:AES:CAMELLIA:DES-CBC3-SHA:!aNULL:!eNULL:!EXPORT:!DES:!RC4:!MD5:!PSK:!aECDH:!EDH-DSS-DES-CBC3-SHA:!EDH-RSA-DES-CBC3-SHA:!KRB5-DES-CBC3-SHA';
    # ssl_session_timeout 1d;
    # ssl_session_cache shared:SSL:50m;
    # ssl_stapling on;
    # ssl_stapling_verify on;
    # add_header Strict-Transport-Security max-age=15768000;

    location /monit/ {
        rewrite             ^/monit/(.*) /$1 break;
        proxy_set_header    Host $host;
        proxy_set_header    X-Real-IP $remote_addr;
        proxy_set_header    X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header    X-Forwarded-Proto $scheme;
        proxy_pass          http://localhost:2812;
        pproxy_read_timeout  90;
        proxy_redirect      http://localhost:2812 https://www.rikerapp.com/monit/;
    }

    location ~ /.well-known {
        root $letsencrypt_webroot;
        allow all;
    }

    location ~ /riker/d/users/[0-9]+/setsfileimport {
        client_max_body_size 13M; # equates to about 150,000 rows of set data
        include /home/fprest/documents/riker-app/nginx/riker_config.conf;
    }

    location /riker/d {
        include /home/fprest/documents/riker-app/nginx/riker_config.conf;
    }

    location /scripts {
        root $content_root/dist/client;
    }

    location /images {
        root $content_root/dist/client;
    }

    location /css/bootstrap.min.css.map {
        alias $content_root/node_modules/bootstrap/dist/css/bootstrap.min.css.map;
    }

    location /css/bootstrap.css.map {
        alias $content_root/node_modules/bootstrap/dist/css/bootstrap.css.map;
    }

    location /css/dist/css/bootstrap.css {
        alias $content_root/node_modules/bootstrap/dist/css/bootstrap.css;
    }

    location /css {
        root $content_root/dist/client;
    }

    location /fonts/redux-toastr.ttf {
        alias $content_root/node_modules/react-redux-toastr/lib/fonts/redux-toastr.ttf;
    }

    location /fonts/redux-toastr.woff {
        alias $content_root/node_modules/react-redux-toastr/lib/fonts/redux-toastr.woff;
    }

    location /fonts/redux-toastr.eot {
        alias $content_root/node_modules/react-redux-toastr/lib/fonts/redux-toastr.eot;
    }

    location /fonts/redux-toastr.svg {
        alias $content_root/node_modules/react-redux-toastr/lib/fonts/redux-toastr.svg;
    }

    location /fonts/rw-widgets.woff {
        alias $content_root/node_modules/react-widgets/lib/fonts/rw-widgets.woff;
    }

    location /fonts/rw-widgets.ttf {
        alias $content_root/node_modules/react-widgets/lib/fonts/rw-widgets.ttf;
    }

    location /fonts/rw-widgets.svg {
        alias $content_root/node_modules/react-widgets/lib/fonts/rw-widgets.svg;
    }

    location /fonts/rw-widgets.eot {
        alias $content_root/node_modules/react-widgets/lib/fonts/rw-widgets.eot;
    }

    location /fonts/jr-hand.ttf {
        alias $content_root/dist/client/fonts/jr-hand.ttf;
    }

    location / {
        proxy_set_header    Host $host;
        proxy_set_header    X-Real-IP $remote_addr;
        proxy_set_header    X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header    X-Forwarded-Proto $scheme;
        proxy_pass          http://localhost:3004;
        proxy_redirect      http://localhost:3004 https://www.rikerapp.com;
        proxy_read_timeout  90;
    }
}
