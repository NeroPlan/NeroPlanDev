FROM nginx
# 시작 시 /etc/resolv.conf의 DNS 서버를 NGINX_LOCAL_RESOLVERS로 만들어 템플릿에서 사용
ENV NGINX_ENTRYPOINT_LOCAL_RESOLVERS=1
COPY nginx.conf /etc/nginx/nginx.conf
COPY default.conf.template /etc/nginx/templates/default.conf.template
