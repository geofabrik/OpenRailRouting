FROM node:15-buster-slim

ARG HOST_USER
ARG HOT_UID
ARG HOST_GID

USER $HOST_UID:$HOST_GID

COPY docker/run.sh /

CMD /bin/bash /run.sh
