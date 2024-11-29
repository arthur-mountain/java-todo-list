ARG POSTGRES_IMAGE

FROM alpine:3.20.3 as prep
WORKDIR /prep
COPY ./database ./database
RUN mkdir docker-entrypoint && \
  cp \
  ./database/*.sql \
  ./database/migrations/*.sql \
  ./docker-entrypoint

FROM "$POSTGRES_IMAGE"
COPY --from=prep /prep/docker-entrypoint /docker-entrypoint-initdb.d
